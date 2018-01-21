# coding: utf-8
import logging.config
import json
import os
import codecs
import argparse
import random
import datetime
import attr

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
            l = []
            with codecs.open(path, 'r', encoding='utf-8') as f_in:
                list_of_slot_values = json.load(f_in)
                for slot_value in list_of_slot_values:
                    value = slot_value.get('value')
                    if value:
                        l.append(value)
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


class Generator:
    def __init__(self, morph_path):
        paradigms_parser = ParadigmsParser()
        self._morph_dict = paradigms_parser.load_dict_from_directory(morph_path, light_weight=True)

    def generate(self, phrase_grammar_path, config_path, out_path, top_path, shortest_top_size):
        phrase_generator = PhraseGenerator(PhraseGrammar(phrase_grammar_path), self._morph_dict)

        shortest_toplist = []

        config_file = open(config_path)
        config = json.load(config_file)

        def context_generator():
            for form in config['form']:
                for _ in range(config['number_of_iterations']):
                    yield (form, config.get('phrase_ids'))

        unique_phrases = set()
        sampling_memory = SamplingMemory()

        n_tries = 0
        failed_forms = set()
        for source_form, phrase_ids in context_generator():
            while True:
                try:
                    prepared_form = prepare_form(source_form, config_path, sampling_memory)
                    form_string = json.dumps(prepared_form, sort_keys=True, indent=2, ensure_ascii=False)

                    if form_string in failed_forms:
                        if n_tries > 100:
                            break
                        n_tries += 1
                        continue

                    phrases_iterator = phrase_generator.generate(
                        onto_context=prepared_form, phrase_full_names=phrase_ids
                    )
                    structured_phrase = next(phrases_iterator)
                    if not structured_phrase:
                        logger.warning('Failed to generate phrase for form:\n%s' % form_string)
                        failed_forms.add(form_string)
                        continue

                    phrase = phrase_generator.render_string(structured_phrase)

                    if phrase in unique_phrases:
                        if n_tries > 100:
                            break
                        n_tries += 1
                        continue
                    n_tries = 0
                    unique_phrases.add(phrase)
                    shortest_toplist.append((prepared_form, phrase,))

                    break
                except Exception as e:
                    print('skipping form due to error: %s' % e)
                    logger.exception(e)

        detected_unknowns = phrase_generator.get_detected_unknown_words()
        if detected_unknowns:
            logger.warning('Unknown words were detected during generation: %s' % detected_unknowns)

        logger.debug(
            'Phrase usage statistics:\n\t%s' % (
                '\n\t'.join(['%s: %d' % (name, stat) for name, stat in phrase_generator.get_phrase_usage_statistics()])
            )
        )

        shortest_toplist.sort(key=lambda t: len(t[1]))

        with codecs.open(out_path, 'w', encoding='utf-8') as f_out:
            timestamp = datetime.datetime.now().strftime("%d.%m.%Y, %H:%M:%S")
            f_out.write(
                '# This file was autogenerated by phrase_generator.py\n'
                '# using grammar from %s on %s\n\n' % (phrase_grammar_path, timestamp)
            )
            for _, phrase in shortest_toplist:
                f_out.write(phrase)
                f_out.write('\n')

        logger.info('%d phrases were stored to %s' % (len(shortest_toplist), out_path))
        if shortest_toplist:
            logger.info('10 random phrases:\n\t%s' % (
                    '\n\t'.join([phrase for (_, phrase) in random.choices(shortest_toplist, k=10)])
                )
            )

        if top_path:
            shortest_toplist = shortest_toplist[0:shortest_top_size]

            toplist_json = json.dumps(
                shortest_toplist, sort_keys=False, ensure_ascii=False, indent=2, separators=(',', ': ')
            )
            with codecs.open(top_path, 'w', encoding='utf8') as f:
                f.write(toplist_json + '\n')

            logger.info('%d form-phrase pairs were stored to %s' % (len(shortest_top_size), top_path))

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Generate phrases using phrase generation framework.\n',
                                     formatter_class=argparse.RawDescriptionHelpFormatter)

    parser.add_argument('--phrase-grammar-path', type=str, required=True, help='path to phrase grammar folder')
    parser.add_argument('--config-path', type=str, required=True, help='path to generator json config')
    parser.add_argument('--out-path', type=str, required=True, help='path where to store generated phrases')
    parser.add_argument('--shortest-top-size', type=int, default=100,
                        required=False, help='defines the size of shortest phrases toplist')
    parser.add_argument('--top-path', type=str, required=False, help='path where to store shortest toplist json')

    args = parser.parse_args()

    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)

    generator = Generator('/Users/starost/projects/4yandex27082017/data')
    do_again = True
    while do_again:
        generator.generate(args.phrase_grammar_path, args.config_path, args.out_path, args.top_path,
                           args.shortest_top_size)
        yes_or_no = input('\nGenerate again? ')
        do_again = yes_or_no.strip().lower() in ['yes', 'y']




