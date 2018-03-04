# coding: utf-8
import logging.config
import json
import os
import codecs
import argparse
import random
import attr
import numpy

from multiprocessing import Pool
from phrase_generation_framework import PhraseGrammar, PhraseGenerator
from morph.paradigms_parser import ParadigmsParser

logger = logging.getLogger(__name__)

SAMPLE_FROM_LIST = '_sample_from_list'
SAMPLE_FROM_FILE = '_sample_from_file'
SAMPLE_FROM_JSON_LIST = '_sample_from_json_list'
SAMPLE_FROM_DATETIME_RAW = '_sample_from_datetime_raw'
OPTIONAL = '_optional'


POSSIBLE_TASKS = {
    SAMPLE_FROM_LIST,
    SAMPLE_FROM_FILE,
    SAMPLE_FROM_JSON_LIST,
    SAMPLE_FROM_DATETIME_RAW,
    OPTIONAL
}


def random_datetime_raw(only_date):
    result = {}
    components = ['years', 'months', 'weeks', 'days'] if only_date else [
        'years', 'months', 'weeks', 'days', 'hours', 'minutes', 'seconds'
    ]

    relative = random.choice([-1, 1])
    relative_lower_bound = 0
    zero_relative_generated = False

    max_number_of_components = 1 if random.random() < 0.5 else len(components)

    for i in range(len(components)):
        if not max_number_of_components:
            break
        cur_component = components[i]

        if random.random() < 0.25 and max_number_of_components > 1:
            relative = 0

        if random.random() < 0.5 or cur_component == 'weeks' and not relative:
            continue

        val = None

        if relative:
            val = random.randint(relative_lower_bound, 2)
        else:
            if cur_component == 'years':
                val = random.randint(2017, 2018)
            elif cur_component == 'months':
                val = random.randint(1, 2)
            elif cur_component == 'days':
                val = random.randint(1, 2)
            elif cur_component == 'hours':
                val = random.randint(0, 2)
            elif cur_component == 'minutes':
                val = random.randint(0, 2)
            elif cur_component == 'seconds':
                val = random.randint(0, 2)

        # The only chance to generate zero relative is to generate it at the first iteration
        relative_lower_bound = 1

        result[cur_component] = val * relative if relative else val
        max_number_of_components -= 1

        if relative:
            result['%s_relative' % cur_component] = True

        if relative and not val:
            # do not try to extend zero relative with other information
            zero_relative_generated = True
            break

    if not zero_relative_generated and (random.random() < 0.25 if len(result) > 2 else 0.5):
        result['weekday'] = random.randint(1, 7)

    if not result:
        result = {'days_relative': True, 'days': 0}

    return result


@attr.s
class SamplingMemory:
    parsed_text_lists = {}
    parsed_json_lists = {}


def sample_from_file(params, config_path, sampling_memory):
    big_list = []
    for p in params[SAMPLE_FROM_FILE]:
        path = os.path.join(os.path.dirname(config_path), p)

        if path not in sampling_memory.parsed_text_lists:
            l = []
            with codecs.open(path, 'r', encoding='utf-8') as f_in:
                for line in f_in:
                    line = line.strip()
                    if line:
                        l.append(line)
                        sampling_memory.parsed_text_lists[path] = l

        big_list += sampling_memory.parsed_text_lists[path]
    return random.choice(big_list)


def sample_from_json(params, config_path, sampling_memory):
    big_list = []
    for p in params[SAMPLE_FROM_JSON_LIST]:
        path = os.path.join(os.path.dirname(config_path), p)

        if path not in sampling_memory.parsed_json_lists:
            with codecs.open(path, 'r', encoding='utf-8') as f_in:
                l = json.load(f_in)
                assert isinstance(l, list)
                sampling_memory.parsed_json_lists[path] = l

        big_list += sampling_memory.parsed_json_lists[path]
    return random.choice(big_list)


def choose_value(params, config_path, sampling_memory):
    if OPTIONAL in params:
        return random.choice([prepare_form(params['_body'], config_path, sampling_memory), None])

    if SAMPLE_FROM_LIST in params:
        return random.choice(params[SAMPLE_FROM_LIST])
    elif SAMPLE_FROM_FILE in params:
        return sample_from_file(params, config_path, sampling_memory)
    elif SAMPLE_FROM_JSON_LIST in params:
        return sample_from_json(params, config_path, sampling_memory)
    elif SAMPLE_FROM_DATETIME_RAW in params:
        r = random_datetime_raw(params.get('_only_date', False))
        return r


def prepare_form(form, config_path, sampling_memory):
    if isinstance(form, dict):
        keys = set(form.keys())
        keys = keys.intersection(POSSIBLE_TASKS)

        if keys:
            result = choose_value(form, config_path, sampling_memory)
        else:
            result = {}
            for k, v in form.items():
                prepared = prepare_form(v, config_path, sampling_memory)
                if prepared:
                    result[k] = prepared
    elif isinstance(form, list):
        result = []
        for v in form:
            prepared = prepare_form(v, config_path, sampling_memory)
            if prepared:
                result.append(prepared)
    else:
        result = form

    return result


class GenerationContext:
    def __init__(
        self, config_path, phrase_grammar_path, json_out_dir, morph_path, num_processes, morph_hints_path=None
    ):
        paradigms_parser = ParadigmsParser()
        self.morph_dict = paradigms_parser.load_dict_from_directory(
            morph_path, light_weight=True
        )
        if morph_hints_path:
            self.morph_dict.load_morph_hints(morph_hints_path)

        self.config_path = config_path
        config_file = open(config_path)
        self.config = json.load(config_file)

        external_morph_info_path = self.config.get('external_morph_info_path')

        if external_morph_info_path:
            external_morph_info_path = os.path.join(os.path.dirname(config_path), external_morph_info_path)
            self.external_morph_info = json.load(open(external_morph_info_path))
        else:
            self.external_morph_info = None

        self.phrase_grammar_path = phrase_grammar_path
        self.num_processes = num_processes
        self.json_out_dir = json_out_dir
        self.phrase_generator = None

    def reload_generator(self):
        self.phrase_generator = PhraseGenerator(
            PhraseGrammar(self.phrase_grammar_path, self.morph_dict),
            self.morph_dict, external_morph_info=self.external_morph_info
        )


def process_forms(arg):
    prepared_forms_chunk, json_out_path, example_out_path, errors_out_path = arg
    target_json = []
    examples = []
    with codecs.open(errors_out_path, 'w', encoding='utf-8') as error_out:
        for form in prepared_forms_chunk:
            example_saved = False
            unique_phrases = set()
            n_tries = 0
            while n_tries < 100 and (
                len(unique_phrases) < generation_context.config['number_of_phrases_per_form_sample']
            ):
                try:
                    phrases_iterator = generation_context.phrase_generator.generate(
                        onto_context=form, phrase_full_names=generation_context.config.get('phrase_ids')
                    )
                    structured_phrase = next(phrases_iterator)
                    if not structured_phrase:
                        error_out.write('Failed to generate phrase for form:\n%s\n' % form)
                        break

                    tagged_phrase, phrase_no_tags = generation_context.phrase_generator.render_strings(
                        structured_phrase
                    )

                    if tagged_phrase in unique_phrases:
                        n_tries += 1
                        continue

                    n_tries = 0
                    unique_phrases.add(tagged_phrase)
                    target_json.append({
                        "phrase": phrase_no_tags,
                        "tagged_phrase": tagged_phrase,
                        "form": form
                    })

                    if len(examples) < max(1, int(10 / generation_context.num_processes)) and not example_saved:
                        examples.append(
                            (
                                form, tagged_phrase,
                                str(structured_phrase),
                                str(structured_phrase.reference_set)
                            )
                        )
                        example_saved = True
                except Exception as e:
                    logger.exception(e)

            if len(unique_phrases) < generation_context.config['number_of_phrases_per_form_sample']:
                error_out.write('Generated only %d phrases instead of %d for form:\n%s\n' % (
                    len(unique_phrases),
                    generation_context.config['number_of_phrases_per_form_sample'],
                    form
                ))

    with codecs.open(json_out_path, mode='w', encoding='utf-8') as fout:
        json.dump(target_json, fout, indent=2, ensure_ascii=False)

    logger.info('%d phrases were stored to %s' % (len(target_json), json_out_path))

    with codecs.open(example_out_path, 'w', encoding='utf-8') as f_out:
        f_out.write('============================\n')
        for form, phrase, structured_phrase, reference_set in examples:
            f_out.write(json.dumps(form, indent=2, sort_keys=True, ensure_ascii=False) + '\n')
            f_out.write(structured_phrase + '\n')
            f_out.write('reference set: ' + reference_set + '\n\n')
            f_out.write(phrase + '\n')
            f_out.write('============================\n')


def generate():
    def context_generator():
        for form in generation_context.config['form']:
            for _ in range(generation_context.config['number_of_samples_per_form']):
                yield form

    sampling_memory = SamplingMemory()
    prepared_forms = {}

    for source_form in context_generator():
        prepared_form = prepare_form(source_form, generation_context.config_path, sampling_memory)
        form_string = json.dumps(prepared_form, sort_keys=True, indent=2, ensure_ascii=False)
        prepared_forms[form_string] = prepared_form

    prepared_forms = iter(numpy.array_split(list(prepared_forms.values()), generation_context.num_processes))
    out_json_paths = [
        os.path.join(generation_context.json_out_dir, '%d.json' % i) for i in range(generation_context.num_processes)
    ]
    out_example_paths = [
        os.path.join(generation_context.json_out_dir, '%d.example.txt' % i)
        for i in range(generation_context.num_processes)
    ]
    out_error_paths = [
        os.path.join(generation_context.json_out_dir, '%d.errors' % i)
        for i in range(generation_context.num_processes)
    ]

    with Pool(processes=generation_context.num_processes) as pool:
        pool.map(process_forms, zip(prepared_forms, out_json_paths, out_example_paths, out_error_paths))

    detected_unknowns = generation_context.phrase_generator.get_detected_unknown_words()
    if detected_unknowns:
        logger.warning('Unknown words were detected during generation: %s' % detected_unknowns)

    logger.debug(
        'Phrase usage statistics:\n\t%s' % (
            '\n\t'.join([
                '%d: %s' % (stat, name)
                for name, stat in generation_context.phrase_generator.get_phrase_usage_statistics()
            ])
        )
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Generate phrases using phrase generation framework.\n',
                                     formatter_class=argparse.RawDescriptionHelpFormatter)

    parser.add_argument('--phrase-grammar-path', type=str, required=True, help='path to phrase grammar folder')
    parser.add_argument('--config-path', type=str, required=True, help='path to generator json config')
    parser.add_argument(
        '--num-processes', type=int, required=False, default=1, help='number of parallel processes to use'
    )
    parser.add_argument(
        '--json-out-dir', type=str, required=False, help='directory where to store generated phrases along with forms'
    )

    args = parser.parse_args()

    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)

    generation_context = GenerationContext(
        args.config_path,
        args.phrase_grammar_path,
        args.json_out_dir,
        './morph/data',
        args.num_processes,
        './phrase_generation/music_queries/morpho_hints.json'
    )

    do_again = True
    while do_again:
        generation_context.reload_generator()
        generate()
        example_lines = []
        for j in range(args.num_processes):
            example_path = os.path.join(args.json_out_dir, '%d.example.txt' % j)

            f = open(example_path)
            for ln in f:
                example_lines.append(ln)

        logger.debug('Generation finished. Examples:\n%s' % ''.join(example_lines))

        yes_or_no = input('\nGenerate again? ')
        do_again = yes_or_no.strip().lower() in ['yes', 'y']
