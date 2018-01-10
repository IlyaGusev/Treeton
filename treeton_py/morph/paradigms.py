import attr
import logging

from morph_interface import MorphEngine, MorphAnResult


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
    def from_dict(cls, data, light=False):
        p = Paradigm(
            lemma=MorphDictionary.normalize(data['lemma']),
            id=data['id'],
            frequency=None if light else data['frequency'],
            status=data['status'],
            zindex=None if light else data['zindex'],
            starling_zindex=None if light else data.get('starling_zindex'),
            source_zindex=None if light else data.get('source_zindex'),
            source_acc_info=None if light else data.get('source_acc_info'),
            starling_paradigm=None if light else data.get('starling_paradigm'),
            error_message=None if light else data.get('error_message'),
            gramm=frozenset([g.lower() for g in data['gramm']])
        )

        elements_data = data.get('paradigm')
        if elements_data:
            for e in elements_data:
                try:
                    pe = ParadigmElement.from_dict(e, light)
                except Exception:
                    if not light and 'starling_unparsed' in e:
                        pe = UnparsedStarlingParadigm(starling_string=e['starling_unparsed'])
                    else:
                        logging.warning('unable to load paradigm element %s of paradigm %s' % (e, p))
                        continue

                p.elements.append(pe)
                pe.parent_paradigm = p

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
    parent_paradigm = attr.ib(default=None)


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
    parent_paradigm = attr.ib(default=None)

    @classmethod
    def from_dict(cls, data, light=False):
        form = data['form']
        if 'ё' in form:
            index = form.index('ё')
            if index == data.get('yo_place'):
                form = form[:index] + 'е' + form[index+1:]

        if not light:
            starling_form = data.get('starling_form')
            if starling_form:
                starling_form = starling_form.replace('ё\'', 'е"')
        else:
            starling_form = None

        return ParadigmElement(
            form=MorphDictionary.normalize(form),
            starling_form=starling_form,
            accent=data['accent'],
            awkward=data.get('awkward'),
            sec_accent=data.get('sec_accent'),
            yo_place=data.get('yo_place'),
            starling_infl_info=None if light else data.get('starling_infl_info'),
            gramm=frozenset([g.lower() for g in data['gramm']])
        )


class MorphDictionary(MorphEngine):
    grammemes_by_categories = {
        'case': {
            'nom', 'acc', 'dat', 'gen', 'ins', 'loc', 'par', 'voc'
        },
        'number': {
            'plur', 'sing'
        },
        'gender': {
            'fem', 'masc', 'neut'
        },
    }

    categories_by_grammemes = {
        g: cat
        for cat, grammemes in grammemes_by_categories.items()
        for g in grammemes
    }

    def get_possible_grammemes(self, category):
        return self.grammemes_by_categories[category]

    def get_category_for_grammeme(self, grammeme):
        return self.categories_by_grammemes.get(grammeme)

    def synthesise(self, paradigm_id, gramm):
        paradigm = self.get_paradigm(paradigm_id)
        if not paradigm:
            return None

        return [
            pe.form
            for pe in paradigm.elements
            if gramm.issubset(pe.gramm.union(pe.parent_paradigm.gramm))
        ]

    @staticmethod
    def normalize(s):
        return s.strip().lower()

    def analyse(self, word):
        return [
            MorphAnResult(
                paradigm_id=pe.parent_paradigm.id,
                lemma=pe.parent_paradigm.lemma,
                accent=pe.accent,
                gramm=pe.gramm.union(pe.parent_paradigm.gramm)
            )
            for pe in self._paradigm_elements.get(self.normalize(word), [])
        ]

    def __init__(self, light_weight=False):
        self._paradigms = {}  # id -> Paradigm
        self._paradigm_elements = {}  # word -> set of ParadigmElement
        self._light_weight = light_weight

    def get_paradigm(self, paradigm_id):
        return self._paradigms.get(paradigm_id)

    def load_paradigm(self, paradigm_data):
        paradigm = Paradigm.from_dict(paradigm_data, light=self._light_weight)

        if paradigm.id in self._paradigms:
            raise ValueError('Paradigm with id %d is already present in the dictionary' % paradigm.id)

        if self._light_weight and paradigm.status == 'error':
            return

        self._paradigms[paradigm.id] = paradigm

        for pe in paradigm.elements:
            list_of_pe = self._paradigm_elements.get(pe.form)

            if not list_of_pe:
                list_of_pe = []
                self._paradigm_elements[pe.form] = list_of_pe

            list_of_pe.append(pe)

    def load_list(self, paradigms_list):
        for paradigm_data in paradigms_list:
            self.load_paradigm(paradigm_data)

    def size(self):
        return len(self._paradigms)
