# coding: utf-8
import logging.config
import json
import os
import codecs
import argparse
import random
import datetime

from phrase_generation_framework import PhraseGrammar, PhraseGenerator
from morph.paradigms_parser import ParadigmsParser

logger = logging.getLogger(__name__)

SAMPLE_FROM_LIST = 'sample_from_list'
SAMPLE_FROM_FILE = 'sample_from_file'
SAMPLE_FROM_JSON_LIST = 'sample_from_json_list'
SAMPLE_FROM_DATETIME_RAW = 'sample_from_datetime_raw'


POSSIBLE_TASKS = [
    SAMPLE_FROM_LIST,
    SAMPLE_FROM_FILE,
    SAMPLE_FROM_JSON_LIST,
    SAMPLE_FROM_DATETIME_RAW
]


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


parsed_text_lists = {}


def sample_from_file(params):
    big_list = []
    for p in params['path']:
        path = os.path.join(os.path.dirname(args.config_path), p)

        if path not in parsed_text_lists:
            l = []
            with codecs.open(path, 'r', encoding='utf-8') as f_in:
                for line in f_in:
                    line = line.strip()
                    if line:
                        l.append(line)
            parsed_text_lists[path] = l

        big_list += parsed_text_lists[path]
    return random.choice(big_list)


parsed_json_lists = {}


def sample_from_json(params):
    big_list = []
    for p in params['path']:
        path = os.path.join(os.path.dirname(args.config_path), p)

        if path not in parsed_json_lists:
            l = []
            with codecs.open(path, 'r', encoding='utf-8') as f_in:
                list_of_slot_values = json.load(f_in)
                for slot_value in list_of_slot_values:
                    value = slot_value.get('value')
                    if value:
                        l.append(value)
            parsed_json_lists[path] = l

        big_list += parsed_json_lists[path]
    return random.choice(big_list)


def choose_value(task, params):
    if task == SAMPLE_FROM_LIST:
        return random.choice(params)
    elif task == SAMPLE_FROM_FILE:
        return sample_from_file(params)
    elif task == SAMPLE_FROM_JSON_LIST:
        return sample_from_json(params)
    elif task == SAMPLE_FROM_DATETIME_RAW:
        r = random_datetime_raw(params.get('only_date', False))
        return r


def prepare_form(form):
    result = {}
    for k, v in form.items():
        if isinstance(v, list) and v and v[0] in POSSIBLE_TASKS:
            v = choose_value(v[0], v[1])
        if v:
            result[k] = v
    return result


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

    config_file = open(args.config_path)
    config = json.load(config_file)

    paradigms_parser = ParadigmsParser()
    morph_dict_4yandex = paradigms_parser.load_dict_from_directory(
        '/Users/starost/projects/4yandex27082017/data', light_weight=True
    )

    phrase_generator = PhraseGenerator(PhraseGrammar(args.phrase_grammar_path), morph_dict_4yandex)

    shortest_toplist = []

    def context_generator():
        for form in config['form']:
            for _ in range(config['number_of_iterations']):
                for pid in config['phrase_ids']:
                    yield (form, pid)

    unique_phrases = set()

    n_tries = 0
    for source_form, phrase_id in context_generator():
        while True:
            try:
                prepared_form = prepare_form(source_form)

                phrases_iterator = phrase_generator.generate(phrase_id, context=prepared_form)
                structured_phrase = next(phrases_iterator)
                if not structured_phrase:
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

    shortest_toplist.sort(key=lambda t: len(t[1]))

    with codecs.open(args.out_path, 'w', encoding='utf-8') as f_out:
        timestamp = datetime.datetime.now().strftime("%d.%m.%Y, %H:%M:%S")
        f_out.write(
            '# This file was autogenerated by phrase_generator.py\n'
            '# using grammar from %s on %s\n\n' % (args.phrase_grammar_path, timestamp)
        )
        for _, phrase in shortest_toplist:
            f_out.write(phrase)
            f_out.write('\n')

    if args.top_path:
        shortest_toplist = shortest_toplist[0:args.shortest_top_size]

        toplist_json = json.dumps(
            shortest_toplist, sort_keys=False, ensure_ascii=False, indent=2, separators=(',', ': ')
        )
        with codecs.open(args.top_path, 'w', encoding='utf8') as f:
            f.write(toplist_json + '\n')