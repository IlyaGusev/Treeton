import glob
import os
import logging
import difflib
import pickle
import _pickle

from .paradigms import MorphDictionary


class ParadigmsParser:
    @staticmethod
    def _extract(string):
        splitted_string = string.split(':', maxsplit=1)
        key = splitted_string[0].replace('-', '').strip()
        value = splitted_string[1].strip()

        if key in ('id', 'accent', 'sec_accent', 'yo_place'):
            value = int(value)
        elif key == 'frequency':
            value = float(value)
        elif key == 'awkward':
            value = 'true' == value
        elif key == 'gramm':
            assert value.startswith('[')
            assert value.endswith(']')

            value = [v.strip() for v in value[1:-1].split(',')]

            # TODO remove this fix after regeneration of paradigms
            value = ['noun' if v.lower() == 'n' else v for v in value]

        return key, value

    @staticmethod
    def string_keys():
        return frozenset((
            'lemma', 'zindex', 'starling_zindex', 'source_zindex', 'source_acc_info', 'starling_paradigm',
            'error_message', 'form', 'starling_form', 'starling_infl_info'
        ))

    def load_dict_from_directory(self, path, light_weight=False):
        logging.info('Reading morph dictionary from %s' % path)
        path_to_binary = path + '/.bin_morph'
        if light_weight and os.path.exists(path_to_binary):
            with open(path_to_binary, 'rb') as handle:
                morph_dict = _pickle.load(handle)
            if morph_dict:
                logging.info('Succesfully loaded %d paradigms from binaries' % morph_dict.size())
                return morph_dict

        morph_dict = MorphDictionary(light_weight=light_weight)
        file_count = 0
        encountered_keys = set()
        for file in glob.iglob(path+'/**/*', recursive=True):
            if os.path.isdir(file):
                continue

            logging.info('Reading data from %s' % file)
            file_count += 1

            current_paradigm_data = {}
            current_paradigm_element_data = None

            for l in open(file):
                l = l.strip()
                if l.startswith('- lemma:'):
                    if current_paradigm_element_data:
                        if 'paradigm' in current_paradigm_data:
                            current_paradigm_data['paradigm'].append(current_paradigm_element_data)
                        else:
                            current_paradigm_data['paradigm'] = [current_paradigm_element_data]
                        current_paradigm_element_data = None
                    if current_paradigm_data:
                        morph_dict.load_paradigm(current_paradigm_data)
                        current_paradigm_data = {}
                        current_paradigm_element_data = None

                if l.startswith('- form:'):
                    if current_paradigm_element_data:
                        if 'paradigm' in current_paradigm_data:
                            current_paradigm_data['paradigm'].append(current_paradigm_element_data)
                        else:
                            current_paradigm_data['paradigm'] = [current_paradigm_element_data]
                        current_paradigm_element_data = {}

                key, value = self._extract(l)
                encountered_keys.add(key)

                if key == 'paradigm':
                    current_paradigm_element_data = {}
                elif current_paradigm_element_data is not None:
                    current_paradigm_element_data[key] = value
                else:
                    current_paradigm_data[key] = value

            if current_paradigm_element_data:
                if 'paradigm' in current_paradigm_data:
                    current_paradigm_data['paradigm'].append(current_paradigm_element_data)
                else:
                    current_paradigm_data['paradigm'] = [current_paradigm_element_data]
            if current_paradigm_data:
                morph_dict.load_paradigm(current_paradigm_data)

        logging.info('%d files containing %d paradigms were read' % (file_count, morph_dict.size()))
        logging.info('encountered keys %s' % encountered_keys)

        if light_weight:
            path_to_binary = path + '/.bin_morph'
            with open(path_to_binary, 'wb') as handle:
                _pickle.dump(morph_dict, handle, protocol=pickle.HIGHEST_PROTOCOL)
            logging.info('Succesfully stored %d paradigms in binary form' % morph_dict.size())

        return morph_dict

if __name__ == "__main__":
    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)

    paradigms_parser = ParadigmsParser()

    morph_dict_4yandex = paradigms_parser.load_dict_from_directory(
        '/Users/starost/projects/4yandex27082017/data'
    )

    morph_dict_starling_macos = paradigms_parser.load_dict_from_directory(
        '/Users/starost/projects/treeton/runtime/domains/Russian/resources/starlingMorph/4Yandex/results'
    )

    # noinspection PyProtectedMember
    d1_ids = set(morph_dict_4yandex._paradigms.keys())
    # noinspection PyProtectedMember
    d2_ids = set(morph_dict_starling_macos._paradigms.keys())

    if d1_ids != d2_ids:
        print('Sets of identifiers differ: - %s, + %s' % (d1_ids - d2_ids, d2_ids - d1_ids))

    common_ids = d1_ids.intersection(d2_ids)

    for p_id in sorted(common_ids):
        p1 = morph_dict_4yandex.get_paradigm(p_id)
        p2 = morph_dict_starling_macos.get_paradigm(p_id)

        diff = p1.smart_compare(p2)

        if diff:
            name = '- %s + %s' % (p1.lemma, p2.lemma) if 'lemma' in diff else p1.lemma
            print('\n------------\nParadigms with id %d differ (lemma "%s"):' % (p_id, name))
            for k, (v1, v2) in diff.items():
                if k == 'lemma':
                    continue

                if k in ParadigmsParser.string_keys():
                    print('    %s:' % k)
                    string_diff = difflib.ndiff([v1 or ''], [v2 or ''])
                    print('\n'.join(string_diff))
                else:
                    print('    %s:\n- %s\n+ %s' % (k, v1, v2))
