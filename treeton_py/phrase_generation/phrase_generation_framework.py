# coding: utf-8
import logging
import os
import attr
import random
import ruamel.yaml as yaml

from copy import deepcopy
from itertools import chain
from morph.morph_interface import SynthResult


logger = logging.getLogger(__name__)


def list_data_files(directory):
    for fname in os.listdir(directory):
        name = os.path.basename(fname)
        path = os.path.join(directory, name)

        name, _ = os.path.splitext(name)
        name, _ = os.path.splitext(name)

        if os.path.isfile(path):
            yield name, path


def _update_grammemes(old_grammemes, new_grammemes, morph_engine):
    if old_grammemes is None:
        return set()

    if new_grammemes:
        new_known_categories = set()
        for gr in new_grammemes:
            cat = morph_engine.get_category_for_grammeme(gr)
            if cat:
                if cat in new_known_categories:
                    logger.error('Duplicate grammeme for category %s detected, skipping' % cat)
                else:
                    new_known_categories.add(cat)

        union_grammemes = set()

        for gr in old_grammemes:
            cat = morph_engine.get_category_for_grammeme(gr)
            if not cat or cat not in new_known_categories:
                union_grammemes.add(gr)

        union_grammemes.update(new_grammemes)
        return union_grammemes
    elif new_grammemes is None:
        return None
    else:
        return old_grammemes


@attr.s(repr=False)
class PhraseDescription(object):
    name = attr.ib()
    onto = attr.ib(default=None)
    tag = attr.ib(default=None)
    is_inline = attr.ib(default=False)
    predecessors = attr.ib(default=attr.Factory(set))
    gov_models = attr.ib(default=attr.Factory(dict))
    punctuation_info = attr.ib(default=attr.Factory(dict))
    grammemes = attr.ib(default=attr.Factory(set))

    @staticmethod
    def disjunctive_generator(phrase_description):
        if isinstance(phrase_description, PhraseDescriptionDisjunctive):
            for phrase in phrase_description.phrases_variants:
                for p in PhraseDescription.disjunctive_generator(phrase):
                    yield p
        else:
            yield phrase_description


@attr.s(repr=False)
class PhraseDescriptionDisjunctive(PhraseDescription):
    phrases_variants = attr.ib(default=attr.Factory(list))  # list of PhraseDescriptionWithReference


@attr.s(repr=False)
class PhraseDescriptionComposite(PhraseDescription):
    root_variants = attr.ib(default=attr.Factory(list))  # list of PhraseDescriptionWithReference
    root_formulae = attr.ib(default=attr.Factory(list))
    children_info = attr.ib(default=attr.Factory(dict))  # semantic role -> list of PhraseDescriptionWithReference
    children_formulae = attr.ib(default=attr.Factory(dict))


@attr.s(repr=False)
class PhraseDescriptionLookup(PhraseDescription):
    label = attr.ib(default=attr.Factory(list))


@attr.s(repr=False)
class PhraseDescriptionLex(PhraseDescription):
    lex_variants = attr.ib(default=attr.Factory(list))


@attr.s
class PhraseDescriptionWithReference(object):
    phrase_description = attr.ib()
    reference = attr.ib(default=attr.Factory(list))


@attr.s(hash=True)
class GovernmentModel(object):
    name = attr.ib()
    optional = attr.ib(default=False)
    orientation = attr.ib(default=None)
    max_cardinality = attr.ib(default=1)
    grammemes = attr.ib(default=attr.Factory(frozenset))
    agree_categories = attr.ib(default=attr.Factory(frozenset))
    prefixes = attr.ib(default=attr.Factory(frozenset))
    indecl = attr.ib(default=False)


@attr.s(hash=True)
class PhraseDescriptionKey(object):
    full_name = attr.ib()
    context = attr.ib(default=None)


@attr.s(hash=True)
class PhraseDescriptionContext(object):
    tag = attr.ib(default=None)
    predecessors = attr.ib(default=attr.Factory(frozenset))
    gov_models = attr.ib(default=attr.Factory(tuple))
    onto = attr.ib(default=None)
    punctuation_info = attr.ib(default=attr.Factory(tuple))
    grammemes = attr.ib(default=attr.Factory(frozenset))


class PhraseGrammar(object):
    def __init__(self, grammar_path, morph_engine):
        self._phrase_descriptions = {}
        self._inline_phrase_descriptions = []
        self._raw_phrase_data = None
        self._morph_engine = morph_engine
        self._load(grammar_path)

    def is_loaded(self):
        return self._raw_phrase_data is not None

    def collect_all_phrase_names(self, phrase_description, target_set):
        target_set.add(phrase_description.name)

        for predecessor_name in phrase_description.predecessors:
            self.collect_all_phrase_names(
                self._phrase_descriptions[PhraseDescriptionKey(predecessor_name)], target_set
            )

    def _load(self, grammar_path):
        self._raw_phrase_data = {}

        for name, path in list_data_files(grammar_path):
            grammar = yaml.load(open(path), Loader=yaml.RoundTripLoader)

            for phrase_name, phrase_raw_data in grammar.items():
                full_name = name + '.' + phrase_name
                if full_name in self._raw_phrase_data:
                    raise ValueError('Duplicate definition of %s' % full_name)
                self._raw_phrase_data[full_name] = phrase_raw_data

        for full_name in self._raw_phrase_data.keys():
            self.get_phrase_description(PhraseDescriptionKey(full_name))

    def get_outer_phrase_descriptions(self):
        return tuple(self._phrase_descriptions.values())

    def get_inline_phrase_descriptions(self):
        return tuple(self._inline_phrase_descriptions)

    @staticmethod
    def _create_phrase_description(raw_phrase_description, full_name):
        if isinstance(raw_phrase_description, dict):
            content_variants = raw_phrase_description.get('content')
            lex = raw_phrase_description.get('lex')
            lookup = raw_phrase_description.get('lookup')

            if lex:
                assert not content_variants and not lookup
                phrase_description = PhraseDescriptionLex(full_name)
            elif lookup:
                assert not content_variants
                phrase_description = PhraseDescriptionLookup(full_name)
            elif content_variants:
                phrase_description = PhraseDescriptionDisjunctive(full_name)
            else:
                phrase_description = PhraseDescriptionComposite(full_name)
        else:
            phrase_description = PhraseDescriptionDisjunctive(full_name)

        return phrase_description

    def get_phrase_description(self, key):
        phrase_description = self._phrase_descriptions.get(key)

        if phrase_description:
            return phrase_description

        raw_phrase_description = self._raw_phrase_data.get(key.full_name)

        if raw_phrase_description is None:
            raise LookupError('Unable to find phrase %s' % key.full_name)

        phrase_description = self._create_phrase_description(raw_phrase_description, key.full_name)
        self._phrase_descriptions[key] = phrase_description

        self._load_phrase_description(phrase_description, raw_phrase_description, key.context)

        return phrase_description

    @staticmethod
    def _globalize_name(context_name, name):
        if '.' not in name:
            name = context_name.split('.')[0] + '.' + name

        return name

    @staticmethod
    def _system_of_tuples_to_dict(data):
        if not isinstance(data, tuple):
            return deepcopy(data)

        res = {}
        for k, v in data:
            res[k] = PhraseGrammar._system_of_tuples_to_dict(v)

        return res

    @staticmethod
    def _dict_to_system_of_tuples(data):
        if not isinstance(data, dict):
            return deepcopy(data)

        return tuple(sorted([(k, PhraseGrammar._dict_to_system_of_tuples(v)) for k, v in data.items()]))

    def _load_phrase_description(self, phrase_description, data, context):
        if isinstance(data, dict):
            lex = data.get('lex')
            lookup = data.get('lookup')
            content_variants = data.get('content')

            if context:
                phrase_description.predecessors = set(context.predecessors)
                phrase_description.gov_models = self._system_of_tuples_to_dict(context.gov_models)
                phrase_description.onto = self._system_of_tuples_to_dict(context.onto)
                phrase_description.tag = context.tag
                phrase_description.punctuation_info = self._system_of_tuples_to_dict(context.punctuation_info)
                phrase_description.grammemes = set(context.grammemes)

            predecessors = data.get('extends')
            if predecessors:
                if not isinstance(predecessors, list):
                    predecessors = [predecessors]

                for predecessor_name in predecessors:
                    assert isinstance(predecessor_name, str)
                    predecessor_name = self._globalize_name(phrase_description.name, predecessor_name)
                    predecessor_phrase_descr = self.get_phrase_description(PhraseDescriptionKey(predecessor_name))

                    if predecessor_phrase_descr.tag:
                        phrase_description.tag = predecessor_phrase_descr.tag
                    if predecessor_phrase_descr.punctuation_info:
                        phrase_description.punctuation_info = dict(predecessor_phrase_descr.punctuation_info)

                    phrase_description.grammemes = _update_grammemes(
                        phrase_description.grammemes, predecessor_phrase_descr.grammemes, self._morph_engine
                    )
                    self._merge_gov_models(phrase_description.gov_models, predecessor_phrase_descr.gov_models)
                    if phrase_description.onto is None and predecessor_phrase_descr.onto:
                        phrase_description.onto = {}
                    # noinspection PyTypeChecker
                    phrase_description.onto = self._merge_onto(phrase_description.onto, predecessor_phrase_descr.onto)
                    phrase_description.predecessors.add(predecessor_name)

            raw_gov_models = data.get('gov')

            if raw_gov_models:
                self._load_gov_models(phrase_description, raw_gov_models)

            onto = data.get('onto')

            if onto:
                if phrase_description.onto is None:
                    phrase_description.onto = {}
                # noinspection PyTypeChecker
                phrase_description.onto = self._merge_onto(phrase_description.onto, onto)

            tag = data.get('tag')
            if tag:
                assert isinstance(tag, str)
                phrase_description.tag = tag

            punctuation_info = data.get('punctuation')
            if punctuation_info:
                assert isinstance(punctuation_info, dict)
                phrase_description.punctuation_info = punctuation_info

            gramm = data.get('gramm')

            if gramm:
                assert isinstance(gramm, list)
                phrase_description.grammemes = _update_grammemes(
                    phrase_description.grammemes, gramm, self._morph_engine
                )

            if lex:
                if not isinstance(lex, list):
                    assert isinstance(lex, str)
                    lex = [lex]

                phrase_description.lex_variants = list(lex)
            elif lookup:
                assert isinstance(lookup, str)

                phrase_description.lookup = lookup.split('.')
            elif content_variants:
                if predecessors or raw_gov_models or tag or punctuation_info or gramm or context or onto:
                    # noinspection PyTypeChecker
                    context = PhraseDescriptionContext(
                        predecessors=frozenset(phrase_description.predecessors),
                        onto=self._dict_to_system_of_tuples(phrase_description.onto),
                        grammemes=frozenset(phrase_description.grammemes),
                        gov_models=self._dict_to_system_of_tuples(phrase_description.gov_models),
                        punctuation_info=self._dict_to_system_of_tuples(phrase_description.punctuation_info),
                        tag=phrase_description.tag
                    )

                phrase_description.phrases_variants = self._load_phrases_from_list(
                    phrase_description.name, [], content_variants, context
                )
            else:
                self._load_composite_phrase(phrase_description, data)
        else:
            assert data
            if context:
                phrase_description.onto = self._system_of_tuples_to_dict(context.onto)
            phrase_description.phrases_variants = self._load_phrases_from_list(
                phrase_description.name, [], data, context
            )

        logger.info('%s successfully loaded.' % phrase_description.name)

        return phrase_description

    def _load_phrases_from_list(
        self, outer_phrase_name, inline_suffix, phrase_list, context=PhraseDescriptionContext()
    ):
        if not isinstance(phrase_list, list):
            phrase_list = [phrase_list]

        result = []

        counter = 0
        for raw_description in phrase_list:
            reference = []
            if isinstance(raw_description, str):
                phrase_description = self.get_phrase_description(
                    PhraseDescriptionKey(
                        full_name=self._globalize_name(outer_phrase_name, raw_description),
                        context=context
                    )
                )
            else:
                assert isinstance(raw_description, dict), 'raw_description is not a dict, phrase %s' % outer_phrase_name

                # reading inline phrase_description
                phrase_description = self._create_phrase_description(
                    raw_description,
                    '%s.%s' % (outer_phrase_name, '.'.join(inline_suffix + [str(counter)]))
                )

                reference = raw_description.get('reference')
                if reference:
                    assert isinstance(reference, str)
                    reference = [ref.strip() for ref in reference.split('.')]
                    if context.onto is None:
                        context.onto = tuple()

                phrase_description = self._load_phrase_description(
                    phrase_description,
                    raw_description,
                    context
                )
                phrase_description.is_inline = True
                self._inline_phrase_descriptions.append(phrase_description)

            counter += 1
            result.append(PhraseDescriptionWithReference(phrase_description, reference))

        return result

    def _load_composite_phrase(self, phrase_description, data):
        root_variants = data.get('root')

        if root_variants:
            phrase_description.root_variants = self._load_phrases_from_list(
                phrase_description.name, ['root'], root_variants
            )

            if not phrase_description.root_variants:
                raise ValueError('root section for phrase %s is empty' % phrase_description.name)

        for semantic_role, raw_children_variants in data.items():
            if semantic_role in {
                'root', 'extends', 'gov', 'tag', 'punctuation', 'gramm', 'onto', 'reference', 'content'
            }:
                continue

            phrase_description.children_info[semantic_role] = self._load_phrases_from_list(
                phrase_description.name, [semantic_role], raw_children_variants
            )

    def _load_gov_models(self, phrase_description, raw_gov_models):
        gov_models = {}
        for semantic_role, raw_gov_model in raw_gov_models.items():
            gov_model = GovernmentModel(semantic_role)

            gov_model.optional = raw_gov_model.get('optional', False)
            gov_model.orientation = raw_gov_model.get('orientation', None)
            gov_model.max_cardinality = raw_gov_model.get('max_cardinality', 1)
            gov_model.indecl = raw_gov_model.get('indecl', False)

            agree = raw_gov_model.get('agree')

            if agree:
                assert isinstance(agree, list)

                gov_model.agree_categories = frozenset(agree)

            gramm = raw_gov_model.get('gramm')

            if gramm:
                assert isinstance(gramm, list)
                gov_model.grammemes = frozenset(gramm)

            prefix = raw_gov_model.get('prefix')
            if prefix:
                assert isinstance(prefix, list)
                gov_model.prefixes = frozenset(prefix)

            gov_models[semantic_role] = gov_model
        self._merge_gov_models(phrase_description.gov_models, gov_models)

    @staticmethod
    def _merge_gov_models(old_gov_models, gov_models):
        for semantic_role, gov_model in gov_models.items():
            old_model = old_gov_models.get(semantic_role)
            if not old_model:
                old_model = GovernmentModel(semantic_role)
                old_gov_models[semantic_role] = old_model

            old_model.optional = gov_model.optional
            old_model.orientation = gov_model.orientation
            old_model.max_cardinality = gov_model.max_cardinality
            old_model.grammemes = old_model.grammemes.union(gov_model.grammemes)
            old_model.agree_categories = old_model.agree_categories.union(gov_model.agree_categories)
            old_model.prefixes = old_model.prefixes.union(gov_model.prefixes)
            old_model.indecl = gov_model.indecl

    @staticmethod
    def _merge_onto(old_onto, onto):
        if not onto:
            return old_onto

        if not isinstance(onto, dict):
            return deepcopy(onto)

        for k, v in onto.items():
            old_v = old_onto.get(k)

            if old_v == v:
                continue

            if old_v:
                if not isinstance(v, dict) or not isinstance(old_v, dict):
                    raise RuntimeError(
                        'Cannot merge non-dict values %s and %s during merging ontology contexts %s and %s' % (
                            old_v, v, old_onto, onto
                        )
                    )
                old_onto[k] = PhraseGrammar._merge_onto(old_v, v)
            else:
                old_onto[k] = deepcopy(v)

        return old_onto


@attr.s
class Phrase(object):
    phrase_description = attr.ib()
    intermediate_phrase_descriptions = attr.ib(default=attr.Factory(list))
    onto = attr.ib(default=None)
    reference_set = attr.ib(default=attr.Factory(set))
    root = attr.ib(default=None)
    children = attr.ib(default=attr.Factory(dict))
    parent = attr.ib(default=None)
    lex = attr.ib(default=None)
    tag = attr.ib(default=None)
    left_punctuator = attr.ib(default=None)
    right_punctuator = attr.ib(default=None)
    prefix = attr.ib(default=None)
    orientation = attr.ib(default=None)

    grammemes = attr.ib(default=attr.Factory(set))
    agree_categories = attr.ib(default=attr.Factory(set))

    morph_an_results = attr.ib(default=None)
    synth_result = attr.ib(default=None)

    def _depth(self):
        depth = 0
        parent = self.parent
        while parent:
            depth += 1
            parent = parent.parent
        return depth

    def __str__(self):
        if self.phrase_description.is_inline:
            name = next(iter(self.phrase_description.predecessors)) if (
                len(self.phrase_description.predecessors) == 1
            ) else '_'
        else:
            name = self.phrase_description.name

        result = '%s %s%s%s %s ' % (
            name,
            '<' + self.tag + '>' if self.tag else '',
            '(' + ','.join(self.grammemes) + ')' if self.grammemes else '',
            '(' + ','.join({'->' + cat for cat in self.agree_categories}) + ')' if self.agree_categories else '',
            self.onto
        )
        if self.lex:
            if self.prefix:
                result += self.prefix + ' '
            result += '%s::%s\n' % (self.lex, self.synth_result.form if self.synth_result else None)
        elif self.root:
            result += '[\n'
            result += '%sroot: %s' % ('\t' * (self._depth() + 1), self.root.__str__())
            for semantic_role, child_phrases in self.children.items():
                for child_phrase in child_phrases:
                    result += '%s%s: %s' % ('\t' * (self._depth() + 1), semantic_role, child_phrase.__str__())
            result += '%s]\n' % ('\t' * self._depth())
        else:
            result += '\n'
        return result


class PhraseGenerator(object):
    def __init__(self, grammar, morph_engine, external_morph_info=None):
        self._grammar = grammar
        assert self._grammar.is_loaded(), 'Phrase generator can not be built over the uninitialized grammar'
        self._morph_engine = morph_engine
        self._external_morph_info = external_morph_info
        self._detected_unknown_words = {}
        self._choosing_memory = {}
        self._phrase_usage_stats = {
            ph.name: 0
            for ph in chain(
                self._grammar.get_outer_phrase_descriptions(),
                self._grammar.get_inline_phrase_descriptions()
            )
        }

    def get_detected_unknown_words(self):
        return sorted(self._detected_unknown_words.items(), key=lambda x: x[1])

    def get_phrase_usage_statistics(self):
        return sorted(self._phrase_usage_stats.items(), key=lambda x: (-x[1], x[0]))

    def generate(self, onto_context, phrase_full_names=None, limit=1):
        logger.debug('Generating phrases for onto_context %s' % onto_context)

        onto_context, _ = self._preprocess_onto(onto_context)

        all_possible_references = set()
        self._collect_possible_references(onto_context, [], all_possible_references)
        fail_count = 0
        while limit and fail_count < 20:
            all_phrase_descriptions = [
                PhraseDescriptionWithReference(ph, [])
                for ph in self._grammar.get_outer_phrase_descriptions()
                if ph.onto and (not phrase_full_names or ph.name in phrase_full_names)
            ]

            phrase, matched_onto, reference_set = self._filter_and_choose(
               all_phrase_descriptions, onto_context
            )

            if not phrase:
                fail_count += 1
                continue

            current_possible_references = set()
            self._collect_possible_references(matched_onto, [], current_possible_references)

            if all_possible_references != current_possible_references or not self._inflect(phrase):
                fail_count += 1
                continue

            fail_count = 0

            limit -= 1

            self._update_usage_stats(phrase)
            yield phrase

        yield None

    def _collect_used_names(self, phrase, target_set):
        used_phrase_descrs = [phrase.phrase_description] + phrase.intermediate_phrase_descriptions
        for phrase_description in used_phrase_descrs:
            self._grammar.collect_all_phrase_names(phrase_description, target_set)

        if phrase.root:
            self._collect_used_names(phrase.root, target_set)
        for _, child_phrases in phrase.children.items():
            for child in child_phrases:
                self._collect_used_names(child, target_set)

    def _update_usage_stats(self, phrase):
        all_names = set()
        self._collect_used_names(phrase, all_names)

        all_all_names = set()

        for name in all_names:
            splitted_name = name.split('.')
            prefixes = ['.'.join(splitted_name[:length+1]) for length in range(len(splitted_name))]
            for prefix in prefixes:
                if prefix in self._phrase_usage_stats:
                    all_all_names.add(prefix)

        for name in all_all_names:
            self._phrase_usage_stats[name] += 1

    def _update_phrase_grammemes(self, phrase, new_grammemes):
        phrase.grammemes = _update_grammemes(phrase.grammemes, new_grammemes, self._morph_engine)

        if phrase.root:
            self._update_phrase_grammemes(phrase.root, new_grammemes)

    def _find_deepest_root(self, phrase):
        if phrase.lex:
            return phrase

        if phrase.root:
            return self._find_deepest_root(phrase.root)

        # in that case we are dealing with zero
        return phrase

    def _calculate_morph_an_results(self, phrase):
        if phrase.morph_an_results is None:
            phrase.morph_an_results = []
            if phrase.lex:
                for lex_variant in phrase.lex:
                    morph_an_results = self._morph_engine.analyse(lex_variant)

                    if self._external_morph_info:
                        external_morph_variants = self._external_morph_info.get(lex_variant)

                        if external_morph_variants:
                            for ext_variant in external_morph_variants:
                                morph_an_results.append(
                                    SynthResult(form=ext_variant[0], gramm=frozenset(ext_variant[1]))
                                )

                    if not morph_an_results:
                        counter = self._detected_unknown_words.get(lex_variant, 0)
                        self._detected_unknown_words[lex_variant] = counter + 1
                    else:
                        phrase.morph_an_results += morph_an_results

    def _inflect(self, phrase):
        categories_to_extract = set()
        new_grammemes = set()

        if phrase.agree_categories:
            assert phrase.parent
            assert phrase.parent.root != phrase
            agree_host = self._find_deepest_root(phrase.parent.root)

            for category in phrase.agree_categories:
                found = False

                possible_gramms = self._morph_engine.get_possible_grammemes(category)

                if agree_host.grammemes:
                    for gr in agree_host.grammemes:
                        if gr in possible_gramms:
                            new_grammemes.add(gr)
                            found = True
                            break

                if not found:
                    categories_to_extract.add(category)

        if categories_to_extract:
            assert phrase.parent
            assert phrase.parent.root != phrase
            agree_host = self._find_deepest_root(phrase.parent.root)

            if agree_host.synth_result:
                extracted_values = {category: None for category in categories_to_extract}
                for gr in agree_host.synth_result.gramm:
                    category = self._morph_engine.get_category_for_grammeme(gr)

                    if not category or category not in extracted_values:
                        continue

                    old_value = extracted_values.get(category)
                    if old_value and old_value != gr:
                        extracted_values.pop(category)
                    elif not old_value:
                        extracted_values[category] = gr

                for c in categories_to_extract:
                    if c not in extracted_values:
                        logger.warning(
                            'Unable to extract the value of category "%s", lex "%s", synth_result "%s"' %
                            (c, agree_host.lex, agree_host.synth_result)
                        )

                extracted_values = {gr for gr in extracted_values.values() if gr}

                if extracted_values:
                    new_grammemes.update(extracted_values)

        if new_grammemes:
            self._update_phrase_grammemes(phrase, new_grammemes)

        if phrase.lex:
            if phrase.grammemes is not None and phrase.morph_an_results:
                filter_grammemes = phrase.grammemes or set()
                synth_variants = []
                for morph_an_result in phrase.morph_an_results:
                    if isinstance(morph_an_result, SynthResult):
                        if filter_grammemes.issubset(morph_an_result.gramm):
                            synth_variants.append(morph_an_result)
                    else:
                        synth_variants += self._morph_engine.synthesise(
                            morph_an_result.paradigm_id,
                            filter_grammemes
                        )

                if synth_variants:
                    phrase.synth_result = random.choice(synth_variants)
                else:
                    logger.warning('Unable to inflect "%s" with gramemmes %s' % (
                        phrase.lex,
                        phrase.grammemes
                    ))

            if not phrase.synth_result:
                phrase.synth_result = SynthResult(form=random.choice(phrase.lex), gramm=frozenset())

        if phrase.root:
            if not self._inflect(phrase.root):
                return False

        for child_phrases in phrase.children.values():
            for child_phrase in child_phrases:
                if not self._inflect(child_phrase):
                    return False

        return True

    def render_string(self, phrase):
        if phrase.synth_result:
            result = phrase.synth_result.form.replace("'", " ")
        elif phrase.root:
            root_repr = self.render_string(phrase.root)

            left_children = []
            right_children = []

            for child_phrases in phrase.children.values():
                for child_phrase in child_phrases:
                    child_repr = self.render_string(child_phrase)

                    if child_repr:
                        if child_phrase.orientation == 'right':
                            list_to_update = right_children
                        elif child_phrase.orientation == 'left':
                            list_to_update = left_children
                        else:
                            list_to_update = random.choice([left_children, right_children])

                        index = random.choice(range(len(list_to_update) + 1))
                        list_to_update.insert(index, child_repr)

            result = ' '.join(left_children + ([root_repr] if root_repr else []) + right_children)
        else:
            result = ''

        left_additional = phrase.prefix + ' ' if phrase.prefix else ''

        if phrase.tag:
            left_additional += '\''

        if phrase.left_punctuator:
            left_additional += phrase.left_punctuator

        right_additional = phrase.right_punctuator or ''

        if phrase.tag:
            right_additional += '\'(%s)' % phrase.tag

        return left_additional + result + right_additional

    @staticmethod
    def _lookup(onto_context, keys):
        current = onto_context
        for key in keys:
            while isinstance(current, list):
                if len(current) > 1:
                    logger.warning(
                        'Ambiguos context %s for lookup operation with key %s, choosing first.' % (current, key)
                    )
                current = current[0][1]

            if not isinstance(current, dict):
                return None

            current = current.get(key)
            if not current:
                return None

        return current

    @staticmethod
    def _preprocess_onto(onto, n_used_ids=0):
        if isinstance(onto, list):
            res = []
            for sub_onto in onto:
                preprocessed_part, n_used_ids = PhraseGenerator._preprocess_onto(sub_onto, n_used_ids)
                res.append((n_used_ids, preprocessed_part))
                n_used_ids += 1

            return res, n_used_ids

        if isinstance(onto, dict):
            new_onto = {}
            for k, v in onto.items():
                preprocessed_part, n_used_ids = PhraseGenerator._preprocess_onto(v, n_used_ids)
                new_onto[k] = preprocessed_part

            onto = new_onto

        return onto, n_used_ids

    @staticmethod
    def _onto_match(current_onto, check_onto, strict=False):
        if not check_onto:
            # None succesfully matches nothing, empty dict succesfully matches all
            return {} if strict or check_onto is None else current_onto

        if not isinstance(check_onto, dict):
            return current_onto if current_onto == check_onto else None

        # find all variants of matching current_onto with check_onto

        if isinstance(current_onto, list):
            matched_onto = []
            for _id, sub_onto in current_onto:
                if not isinstance(sub_onto, (list, dict)):
                    continue

                sub_matched_onto = PhraseGenerator._onto_match(sub_onto, check_onto, strict=strict)

                if sub_matched_onto is not None:
                    matched_onto.append((_id, sub_matched_onto))

            return matched_onto or None

        matched_onto = {}

        for k, check_v in check_onto.items():
            current_v = current_onto.get(k)
            if not current_v:
                return None

            assert check_v is not None

            if isinstance(check_v, dict):
                if not isinstance(current_v, (dict, list)) and check_v:
                    return None

                sub_matched_onto = PhraseGenerator._onto_match(current_v, check_v, strict=strict)

                if sub_matched_onto is None:
                    return None

                matched_onto[k] = sub_matched_onto
            elif check_v == current_v:
                matched_onto[k] = current_v
            else:
                return None

        if not strict:
            for k, current_v in current_onto.items():
                if k in check_onto:
                    continue

                matched_onto[k] = deepcopy(current_v)

        return matched_onto

    @staticmethod
    def _update_onto(current_onto, new_onto):
        if not new_onto:
            return current_onto

        if not current_onto:
            return deepcopy(new_onto)

        assert type(current_onto) == type(new_onto)

        if isinstance(new_onto, dict):
            for k, sub_onto in new_onto.items():
                if k in current_onto:
                    current_onto[k] = PhraseGenerator._update_onto(current_onto[k], sub_onto)
                else:
                    current_onto[k] = deepcopy(sub_onto)
        elif isinstance(new_onto, list):
            to_add = []
            for new_id, new_sub_onto in new_onto:
                found = False
                for _id, sub_onto in current_onto:
                    if _id == new_id:
                        PhraseGenerator._update_onto(sub_onto, new_sub_onto)
                        found = True
                        break
                if not found:
                    to_add.append((new_id, deepcopy(new_sub_onto)))
            current_onto += to_add
        else:
            assert current_onto == new_onto

        return current_onto

    @staticmethod
    def _process_reference(reference, context):
        if not reference:
            return context

        if isinstance(context, dict):
            if reference[0] not in context:
                return None

            return PhraseGenerator._process_reference(reference[1:], context[reference[0]])
        elif isinstance(context, list):
            valid_sub_contexts = []
            for uid, sub_context in context:
                processed_context = PhraseGenerator._process_reference(reference, sub_context)
                if processed_context is not None:
                    valid_sub_contexts.append((uid, processed_context))

            return valid_sub_contexts or None
        else:
            return None

    @staticmethod
    def _process_reference(reference, context):
        if not reference:
            return context

        if isinstance(context, dict):
            if reference[0] not in context:
                return None

            return PhraseGenerator._process_reference(reference[1:], context[reference[0]])
        elif isinstance(context, list):
            valid_sub_contexts = []
            for uid, sub_context in context:
                processed_context = PhraseGenerator._process_reference(reference, sub_context)
                if processed_context is not None:
                    valid_sub_contexts.append((uid, processed_context))

            return valid_sub_contexts or None
        else:
            return None

    @staticmethod
    def _process_reference(reference, onto):
        if not reference:
            return onto

        if isinstance(onto, dict):
            if reference[0] not in onto:
                return None

            return PhraseGenerator._process_reference(reference[1:], onto[reference[0]])
        elif isinstance(onto, list):
            valid_sub_contexts = []
            for uid, sub_context in onto:
                processed_context = PhraseGenerator._process_reference(reference, sub_context)
                if processed_context is not None:
                    valid_sub_contexts.append((uid, processed_context))

            return valid_sub_contexts or None
        else:
            return None

    @staticmethod
    def _collect_possible_references(onto, current_reference, target_reference_set, collect_leaves=False):
        if isinstance(onto, dict):
            for k, sub_onto in onto.items():
                new_reference = current_reference + [k]
                target_reference_set.add(tuple(new_reference))
                PhraseGenerator._collect_possible_references(
                    sub_onto, new_reference, target_reference_set, collect_leaves
                )
        elif isinstance(onto, list):
            for _id, sub_onto in onto:
                PhraseGenerator._collect_possible_references(
                    sub_onto, current_reference + [_id], target_reference_set, collect_leaves
                )
        elif collect_leaves:
            new_reference = current_reference + [onto]
            target_reference_set.add(tuple(new_reference))

    @staticmethod
    def _filter_variants(onto, list_of_phrase_descr_with_ref):
        result = []
        for phrase_descr_with_ref in list_of_phrase_descr_with_ref:
            reference = phrase_descr_with_ref.reference
            phrase_description = phrase_descr_with_ref.phrase_description

            sub_onto = PhraseGenerator._process_reference(reference, onto)
            if sub_onto is None:
                continue

            # noinspection PyTypeChecker
            matched_onto = PhraseGenerator._onto_match(sub_onto, phrase_description.onto)
            if matched_onto is not None:
                result.append((matched_onto, phrase_description, reference))
        return result

    _INTERNAL_RETRIES_COUNT = 10

    @attr.s(hash=True)
    class ChoosingMemoryKey(object):
        phrases_repr = attr.ib(default=attr.Factory(frozenset))
        onto_repr = attr.ib(default=attr.Factory(frozenset))
        already_referenced_repr = attr.ib(default=attr.Factory(frozenset))

    def _filter_and_choose(self, list_of_phrase_descr_with_ref, onto, already_referenced=set()):
        phrases_repr = frozenset([
            (id(pd.phrase_description), tuple(pd.reference or ())) for pd in list_of_phrase_descr_with_ref
        ])
        possible_references = set()
        self._collect_possible_references(onto, [], possible_references, True)
        onto_repr = frozenset(possible_references)

        mem_key = PhraseGenerator.ChoosingMemoryKey(
            phrases_repr=phrases_repr,
            onto_repr=onto_repr,
            already_referenced_repr=frozenset(already_referenced)
        )

        memorized_filter_variants = self._choosing_memory.get(mem_key)
        if memorized_filter_variants is None:
            memorized_filter_variants = []
            filtered_variants = PhraseGenerator._filter_variants(onto, list_of_phrase_descr_with_ref)

            if filtered_variants:
                for matched_onto, phrase_variant, reference in filtered_variants:
                    result = self._choose(phrase_variant, matched_onto)
                    if result:
                        memorized_filter_variants.append((matched_onto, phrase_variant, reference))

            self._choosing_memory[mem_key] = memorized_filter_variants

        if not memorized_filter_variants:
            return None, None, None

        i = PhraseGenerator._INTERNAL_RETRIES_COUNT
        while i >= 0:
            matched_onto, phrase_variant, reference = random.choice(memorized_filter_variants)
            result = self._choose(phrase_variant, matched_onto)

            if result:
                new_reference_set = set()
                matched_onto = PhraseGenerator._rewind_reference(
                    result.onto, reference, [], new_reference_set, result.reference_set
                )
                if new_reference_set.isdisjoint(already_referenced):
                    return result, matched_onto, new_reference_set
            i -= 1

        return None, None, None

    @staticmethod
    def _rewind_reference(onto, reference, current_path, target_reference_set, reference_filter):
        if not reference:
            possible_references = set()
            PhraseGenerator._collect_possible_references(onto, [], possible_references)
            possible_references = possible_references.intersection(reference_filter)

            for reference in possible_references:
                target_reference_set.add(tuple(current_path + list(reference)))

            if current_path:
                target_reference_set.add(tuple(current_path))
            return onto

        ref = reference[0]

        if isinstance(onto, list):
            return {
                ref: [
                    (
                        _id,
                        PhraseGenerator._rewind_reference(
                            sub_onto, reference[1:], current_path + [ref, _id], target_reference_set, reference_filter
                        )
                    )
                    for (_id, sub_onto) in onto
                ]
            }

        return {
            ref:
            PhraseGenerator._rewind_reference(
                onto, reference[1:], current_path + [ref], target_reference_set, reference_filter
            )
        }

    def _choose(self, phrase_description, onto):
        if isinstance(phrase_description, PhraseDescriptionDisjunctive):
            result, matched_onto, reference_set = self._filter_and_choose(
                phrase_description.phrases_variants, onto
            )
            if not result:
                return None
            result.intermediate_phrase_descriptions.append(phrase_description)
            result.onto = matched_onto
            result.reference_set = reference_set
        else:
            result = Phrase(phrase_description, grammemes=set(phrase_description.grammemes))
            if onto:
                result.onto = self._onto_match(onto, phrase_description.onto, strict=True)

            if isinstance(phrase_description, PhraseDescriptionLex):
                result.lex = deepcopy(phrase_description.lex_variants)
                self._calculate_morph_an_results(result)
            elif isinstance(phrase_description, PhraseDescriptionLookup):
                result.lex = [self._lookup(onto, phrase_description.lookup)]
                self._calculate_morph_an_results(result)
            else:
                assert isinstance(phrase_description, PhraseDescriptionComposite)

                if phrase_description.root_variants:
                    result.root, root_matched_onto, root_reference_set = self._filter_and_choose(
                        phrase_description.root_variants, onto
                    )

                    if not result.root:
                        return None

                    result.root.parent = result
                    result.reference_set = root_reference_set
                    result.onto = self._update_onto(result.onto, root_matched_onto)

                    chosen_children = {}

                    children_candidates = list(phrase_description.children_info.items())
                    random.shuffle(children_candidates)

                    candidates_left = True

                    while candidates_left:
                        candidates_left = False

                        for semantic_role, children_variants in children_candidates:
                            gov_model = result.root.phrase_description.gov_models.get(semantic_role)

                            if not gov_model:
                                continue

                            if semantic_role in chosen_children:
                                children_phrases = chosen_children[semantic_role]
                            else:
                                children_phrases = []
                                chosen_children[semantic_role] = children_phrases

                            if len(children_phrases) < gov_model.max_cardinality:
                                candidates_left = True

                                child_phrase, child_matched_onto, child_reference_set = self._filter_and_choose(
                                    children_variants, onto, already_referenced=result.reference_set
                                )

                                if not child_phrase and not children_phrases and not gov_model.optional:
                                    return None

                                if child_phrase and not child_matched_onto and random.choice([0, 1]):
                                    if gov_model.optional or any(children_phrases):
                                        child_phrase = None

                                if child_phrase:
                                    result.onto = self._update_onto(result.onto, child_matched_onto)
                                    result.reference_set.update(child_reference_set)
                                    children_phrases.append(child_phrase)
                                else:
                                    children_phrases.append(None)

                    for semantic_role, children_phrases in chosen_children.items():
                        child_phrases = []
                        for child_phrase in children_phrases:
                            if not child_phrase:
                                continue

                            gov_model = result.root.phrase_description.gov_models.get(semantic_role)
                            child_phrase.agree_categories = set(gov_model.agree_categories)
                            self._update_phrase_grammemes(
                                child_phrase, None if gov_model.indecl else gov_model.grammemes
                            )
                            if gov_model.prefixes:
                                child_phrase.prefix = random.choice(list(gov_model.prefixes))

                            child_phrase.orientation = gov_model.orientation
                            child_phrase.parent = result
                            child_phrases.append(child_phrase)

                        result.children[semantic_role] = child_phrases
                else:
                    # special case - zero element
                    pass

        if result.root:
            punct_info = result.root.phrase_description.punctuation_info
            result.left_punctuator = punct_info.get('left')
            result.right_punctuator = punct_info.get('right')

        if not result.tag:
            result.tag = phrase_description.tag

        return result
