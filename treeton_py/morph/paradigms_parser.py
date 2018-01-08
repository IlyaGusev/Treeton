import attr
import glob
import os
import logging
import difflib


@attr.s
class Paradigm(object):
    lemma = attr.ib()
    id = attr.ib()
    frequency = attr.ib()
    status = attr.ib()
    zindex = attr.ib()
    source_zindex = attr.ib(default=None)
    source_acc_info = attr.ib(default=None)
    starling_zindex = attr.ib(default=None)
    starling_paradigm = attr.ib(default=None)
    error_message = attr.ib(default=None)
    gramm = attr.ib(default=attr.Factory(frozenset))
    elements = attr.ib(default=attr.Factory(list))

    @classmethod
    def from_dict(cls, data):
        p = Paradigm(
            lemma=data['lemma'],
            id=data['id'],
            frequency=data['frequency'],
            status=data['status'],
            zindex=data['zindex'],
            starling_zindex=data.get('starling_zindex'),
            source_zindex=data.get('source_zindex'),
            source_acc_info=data.get('source_acc_info'),
            starling_paradigm=data.get('starling_paradigm'),
            error_message=data.get('error_message'),
            gramm=frozenset(data['gramm'])
        )

        elements_data = data.get('paradigm')
        if elements_data:
            for e in elements_data:
                try:
                    pe = ParadigmElement.from_dict(e)
                except Exception:
                    if 'starling_unparsed' in e:
                        pe = UnparsedStarlingParadigm(starling_string=e['starling_unparsed'])
                    else:
                        logging.warning('unable to parse paradigm element %s of paradigm %s' % (e, p))
                        continue

                p.elements.append(pe)

        return p

    @staticmethod
    def smart_strings_cmp(s1, s2):
        if not s1 or not s2:
            return s1 == s2

        s1 = s1.replace('ё\'', 'е"').replace('[', '(').replace(']', ')')
        s2 = s2.replace('ё\'', 'е"').replace('[', '(').replace(']', ')')

        return s1 == s2

    def smart_compare(self, other_paradigm):
        diff = {}

        for key in [
            'lemma', 'id', 'frequency', 'status', 'zindex', 'starling_zindex',
            'source_zindex', 'source_acc_info', 'starling_paradigm', 'error_message'
        ]:
            self_value = self.__dict__.get(key)
            other_value = other_paradigm.__dict__.get(key)

            same = self.smart_strings_cmp(self_value, other_value) if (
                key in ('starling_paradigm', 'starling_zindex')
            ) else self_value == other_value

            if not same:
                diff[key] = (self_value, other_value)

        if self.gramm != other_paradigm.gramm:
            diff['gramm'] = (set(self.gramm - other_paradigm.gramm), set(other_paradigm.gramm - self.gramm))

        self_elements = set(self.elements)
        other_elements = set(other_paradigm.elements)

        if self_elements != other_elements:
            elements_diff = (self_elements - other_elements, other_elements - self_elements)
            elements_diff = (
                sorted({e.form if isinstance(e, ParadigmElement) else e.starling_string for e in elements_diff[0]}),
                sorted({e.form if isinstance(e, ParadigmElement) else e.starling_string for e in elements_diff[1]})
            )
            diff['elements'] = elements_diff

        return diff


@attr.s(hash=True)
class UnparsedStarlingParadigm(object):
    starling_string = attr.ib()


@attr.s(hash=True)
class ParadigmElement(object):
    form = attr.ib()
    starling_form = attr.ib()
    accent = attr.ib()
    awkward = attr.ib(default=False)
    sec_accent = attr.ib(default=None)
    yo_place = attr.ib(default=None)
    starling_infl_info = attr.ib(default=None)
    gramm = attr.ib(default=attr.Factory(frozenset))

    @classmethod
    def from_dict(cls, data):
        form = data['form']
        if 'ё' in form:
            index = form.index('ё')
            if index == data.get('yo_place'):
                form = form[:index] + 'е' + form[index+1:]

        starling_form = data.get('starling_form')
        if starling_form:
            starling_form = starling_form.replace('ё\'', 'е"')

        return ParadigmElement(
            form=form,
            starling_form=starling_form,
            accent=data['accent'],
            awkward=data.get('awkward'),
            sec_accent=data.get('sec_accent'),
            yo_place=data.get('yo_place'),
            starling_infl_info=data.get('starling_infl_info'),
            gramm=frozenset(data['gramm'])
        )


class MorphDictionary:
    def __init__(self):
        self.paradigms = {}  # id -> Paradigm

    def load_paradigm(self, paradigm_data):
        paradigm = Paradigm.from_dict(paradigm_data)

        if paradigm.id in self.paradigms:
            raise ValueError('Paradigm with id %d is already present in the dictionary' % paradigm.id)

        self.paradigms[paradigm.id] = paradigm

    def load_list(self, paradigms_list):
        for paradigm_data in paradigms_list:
            self.load_paradigm(paradigm_data)

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

        return key, value

    @staticmethod
    def string_keys():
        return frozenset((
            'lemma', 'zindex', 'starling_zindex', 'source_zindex', 'source_acc_info', 'starling_paradigm',
            'error_message', 'form', 'starling_form', 'starling_infl_info'
        ))

    @classmethod
    def load_from_directory(cls, path):
        morph_dict = MorphDictionary()
        logging.info('Reading files')
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

                key, value = cls._extract(l)
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

        logging.info('%d files containing %d paradigms were read' % (file_count, len(morph_dict.paradigms)))
        logging.info('encountered keys %s' % encountered_keys)
        return morph_dict

if __name__ == "__main__":
    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)
    morph_dict_4yandex = MorphDictionary.load_from_directory(
        '/Users/starost/projects/4yandex27082017/data'
    )

    morph_dict_starling_macos = MorphDictionary.load_from_directory(
        '/Users/starost/projects/treeton/runtime/domains/Russian/resources/starlingMorph/4Yandex/results'
    )

    d1_ids = set(morph_dict_4yandex.paradigms.keys())
    d2_ids = set(morph_dict_starling_macos.paradigms.keys())

    if d1_ids != d2_ids:
        print('Sets of identifiers differ: - %s, + %s' % (d1_ids - d2_ids, d2_ids - d1_ids))

    common_ids = d1_ids.intersection(d2_ids)

    for p_id in sorted(common_ids):
        p1 = morph_dict_4yandex.paradigms[p_id]
        p2 = morph_dict_starling_macos.paradigms[p_id]

        diff = p1.smart_compare(p2)

        if diff:
            name = '- %s + %s' % (p1.lemma, p2.lemma) if 'lemma' in diff else p1.lemma
            print('\n------------\nParadigms with id %d differ (lemma "%s"):' % (p_id, name))
            for k, (v1, v2) in diff.items():
                if k == 'lemma':
                    continue

                if k in MorphDictionary.string_keys():
                    print('    %s:' % k)
                    string_diff = difflib.ndiff([v1 or ''], [v2 or ''])
                    print('\n'.join(string_diff))
                else:
                    print('    %s:\n- %s\n+ %s' % (k, v1, v2))
