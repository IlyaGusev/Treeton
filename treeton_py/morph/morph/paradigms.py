import attr
import logging
import itertools
from marisa_trie import Trie

from .morph_interface import MorphEngine, MorphAnResult


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
            id=int(data['id']),
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
class LightParadigmElementInfo(object):
    flags = attr.ib()
    gramm = attr.ib()

    def is_awkward(self):
        return self.flags & 1

    def get_accent_place(self):
        return (self.flags >> 1) & 0xff

    def get_sec_accent_place(self):
        return (self.flags >> 17) & 0xff

    def get_yo_place(self):
        return (self.flags >> 33) & 0xff


@attr.s(hash=True)
class ParadigmElementInfo(LightParadigmElementInfo):
    starling_form = attr.ib()
    starling_infl_info = attr.ib()


@attr.s(hash=True)
class ParadigmElement(object):
    form = attr.ib()
    info = attr.ib()
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

        accent = data['accent']
        assert accent <= 255

        sec_accent = data.get('sec_accent', 0)
        assert sec_accent <= 255

        yo_place = data.get('yo_place', 0)
        assert yo_place <= 255

        awkward = 1 if data.get('awkward') else 0

        gramm = frozenset([g.lower() for g in data['gramm']])

        flags = awkward & (accent << 1) & (sec_accent << 17) & (yo_place << 33)

        return ParadigmElement(
            form=MorphDictionary.normalize(form),
            info=LightParadigmElementInfo(
                    flags=flags,
                    gramm=gramm
                ) if light else ParadigmElementInfo(
                    flags=flags,
                    gramm=gramm,
                    starling_form=starling_form,
                    starling_infl_info=data.get('starling_infl_info')
                )
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
        'verbform': {
            'conv', 'fin', 'inf', 'part'
        },
        'mood': {
            'cnd', 'imp', 'ind'
        },
        'animacy': {
            'anim', 'inan'
        }
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
            self.untrie_lex(pe.form)
            for pe in paradigm.elements
            if gramm.issubset(self.count_gramm(pe))
        ]

    @staticmethod
    def normalize(s):
        return s.strip().lower()

    def count_gramm(self, paradigm_element):
        if isinstance(paradigm_element.info.gramm, int):
            gramm = paradigm_element.info.gramm | paradigm_element.parent_paradigm.gramm

            gramm_set = set()
            current_gid = 1
            while gramm:
                if gramm & 1:
                    gramm_set.add(self._gid_to_gramms[current_gid])

                current_gid <<= 1
                gramm >>= 1

            gramm = gramm_set
        else:
            gramm = set(paradigm_element.info.gramm).union(paradigm_element.parent_paradigm.gramm)

        if 'adj' in gramm and 'pos' in gramm and 'inan' not in gramm and 'anim' not in gramm:
            gramm.add('inan')
            gramm.add('anim')

        return frozenset(gramm)

    def untrie_lex(self, int_index):
        if not self._lex_trie:
            return int_index

        assert isinstance(int_index, int)

        return self._lex_trie.restore_key(int_index)

    def analyse(self, word):
        normalized_word = self.normalize(word)
        if self._lex_trie:
            normalized_word = self._lex_trie.get(normalized_word)
            if not normalized_word:
                return []

        return [
            MorphAnResult(
                paradigm_id=pe.parent_paradigm.id,
                lemma=self.untrie_lex(pe.parent_paradigm.lemma),
                accent=pe.info.get_accent_place(),
                gramm=self.count_gramm(pe)
            )
            for pe in self._paradigm_elements.get(normalized_word, [])
        ]

    def __init__(self, light_weight=False):
        self._paradigms = {}  # id -> Paradigm
        self._paradigm_elements = {}  # word -> set of ParadigmElement
        self._light_weight = light_weight
        self._lex_trie = None
        self._gramm_trie = None
        self._gramms_to_gid = None
        self._gid_to_gramms = None

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

    def _pack_gramm(self, gramm):
        packed_gramm = 0
        for g in gramm:
            gid = self._gramms_to_gid.get(g)
            if not gid:
                gid = 1 << len(self._gramms_to_gid)
                self._gramms_to_gid[g] = gid
                self._gid_to_gramms[gid] = g

            packed_gramm |= gid

        return packed_gramm

    def pack(self):
        assert self._light_weight, 'Packing is allowed only in light weight mode'

        self._lex_trie = Trie(itertools.chain(
            (p.lemma for p in self._paradigms.values()),
            (pe.form for pe_list in self._paradigm_elements.values() for pe in pe_list)
        ))

        self._gramms_to_gid = {}
        self._gid_to_gramms = {}

        for p in self._paradigms.values():
            p.lemma = self._lex_trie[p.lemma]
            p.gramm = self._pack_gramm(p.gramm)

        packed_pe_infos = {}

        packed_paradigm_elements = {}
        for form, pe_list in self._paradigm_elements.items():
            for pe in pe_list:
                pe.form = self._lex_trie[pe.form]
                pe.info.gramm = self._pack_gramm(pe.info.gramm)
                packed_pe_info = packed_pe_infos.get(pe.info)
                if not packed_pe_info:
                    packed_pe_infos[pe.info] = pe.info
                else:
                    pe.info = packed_pe_info

            packed_paradigm_elements[self._lex_trie[form]] = pe_list

        self._paradigm_elements = packed_paradigm_elements

    def size(self):
        return len(self._paradigms)
