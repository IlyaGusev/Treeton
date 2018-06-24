# coding: utf-8
import logging.config
import json
import os
import codecs
import argparse

from copy import deepcopy

from phrase_generation_framework import PhraseGrammar, PhraseGenerator
from morph.paradigms_parser import ParadigmsParser

logger = logging.getLogger(__name__)


class Generator:
    def __init__(self, morph_path, morph_hints_path=None):
        paradigms_parser = ParadigmsParser()
        self._morph_dict = paradigms_parser.load_dict_from_directory(
            morph_path, light_weight=True
        )
        if morph_hints_path:
            self._morph_dict.load_morph_hints(morph_hints_path)

    @staticmethod
    def _substitute_in_form(form, placeholder, new_value):
        for k, v in form.items():
            if v == placeholder:
                form[k] = new_value
                return True
            elif isinstance(v, dict) and Generator._substitute_in_form(v, placeholder, new_value):
                return True

        return False

    def generate(self, phrase_grammar_path, config_path, out_path):
        config_file = open(config_path)
        config = json.load(config_file)
        external_morph_info_path = config.get('external_morph_info_path')

        if external_morph_info_path:
            external_morph_info_path = os.path.join(os.path.dirname(config_path), external_morph_info_path)
            external_morph_info = json.load(open(external_morph_info_path))
        else:
            external_morph_info = None

        phrase_generator = PhraseGenerator(
            PhraseGrammar(phrase_grammar_path, self._morph_dict),
            self._morph_dict, external_morph_info=external_morph_info,
            skip_untagged=config.get('skip_untagged_phrases', False),
            filter_tags=config.get('filter_tags', [])
        )

        form_config = config['form_config']

        for entity_name, entity_cfg in form_config.items():
            target_dict = {}
            limit = entity_cfg['limit']
            for entity_id in entity_cfg['values']:
                forms = entity_cfg['form']

                if not isinstance(forms, list):
                    forms = [forms]

                unique_phrases = set()

                for form in forms:
                    form = deepcopy(form)
                    if not Generator._substitute_in_form(form, '$' + entity_name, entity_id):
                        logger.warning("Unable to substitute entity id %s, skipping" % entity_id)
                        continue

                    n_tries = 0
                    failed_forms = set()
                    for i in range(limit):
                        if i % 100:
                            logger.info('Iteration %d, number of unique phrases %d' % (i, len(unique_phrases)))

                        while True:
                            try:
                                form_string = json.dumps(form, sort_keys=True, indent=2, ensure_ascii=False)

                                if form_string in failed_forms:
                                    if n_tries > 20:
                                        break
                                    n_tries += 1
                                    continue

                                phrases_iterator = phrase_generator.generate(onto_context=form)
                                structured_phrase = next(phrases_iterator)
                                if not structured_phrase:
                                    logger.warning('Failed to generate phrase for form:\n%s' % form_string)
                                    failed_forms.add(form_string)
                                    continue

                                phrase, _ = phrase_generator.render_strings(structured_phrase)

                                tag_mention = "'(%s)" % entity_name
                                splitted_phrase = phrase.split(tag_mention)

                                if len(splitted_phrase) > 2:
                                    logger.warning("phrase '%s' with multiple tag %s detected" % (phrase, entity_name))

                                if len(splitted_phrase) < 2:
                                    logger.warning("phrase '%s' without tag %s detected" % (phrase, entity_name))
                                    phrase = None
                                else:
                                    quote_index = splitted_phrase[0].rfind("'")

                                    if quote_index is None:
                                        logger.warning("Corrupted markup in phrase %s" % phrase)
                                        phrase = None
                                    else:
                                        phrase = splitted_phrase[0][quote_index+1:].strip()

                                if not phrase or phrase in unique_phrases:
                                    if n_tries > 20:
                                        break
                                    n_tries += 1
                                    continue

                                n_tries = 0
                                unique_phrases.add(phrase)

                                break
                            except Exception as e:
                                print('skipping form due to error: %s' % e)
                                logger.exception(e)

                target_dict[entity_id] = sorted(unique_phrases)

            target_path = os.path.join(out_path, entity_name + '.json')
            with codecs.open(target_path, mode='w', encoding='utf-8') as fout:
                json.dump(target_dict, fout, indent=2, ensure_ascii=False)

        detected_unknowns = sorted(phrase_generator.get_detected_unknown_words().items(), key=lambda x: x[1])
        if detected_unknowns:
            logger.warning('Unknown words were detected during generation: %s' % detected_unknowns)

        logger.debug(
            'Phrase usage statistics:\n\t%s' % (
                '\n\t'.join([
                    '%d: %s' % (stat, name)
                    for name, stat in sorted(
                        phrase_generator.get_phrase_usage_statistics().items(), key=lambda x: (-x[1], x[0])
                    )
                ])
            )
        )

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Generate vins custom entities using phrase generation framework.\n',
                                     formatter_class=argparse.RawDescriptionHelpFormatter)

    parser.add_argument('--phrase-grammar-path', type=str, required=True, help='path to phrase grammar folder')
    parser.add_argument('--config-path', type=str, required=True, help='path to generator json config')
    parser.add_argument(
        '--out-path', type=str, required=True, help='path where to store generated custom entities data'
    )

    args = parser.parse_args()

    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)

    generator = Generator(
        './morph/data/',
        './phrase_generation/music_queries/morpho_hints.json'
    )

    generator.generate(args.phrase_grammar_path, args.config_path, args.out_path)
