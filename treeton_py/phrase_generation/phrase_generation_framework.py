# coding: utf-8
import logging
import os
import attr
import random
import ruamel.yaml as yaml

from copy import deepcopy


logger = logging.getLogger(__name__)


def list_data_files(directory):
    for fname in os.listdir(directory):
        name = os.path.basename(fname)
        path = os.path.join(directory, name)

        name, _ = os.path.splitext(name)
        name, _ = os.path.splitext(name)

        if os.path.isfile(path):
            yield name, path


@attr.s
class PhraseDescription(object):
    name = attr.ib()
    onto = attr.ib(default=attr.Factory(dict))
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


@attr.s
class PhraseDescriptionDisjunctive(PhraseDescription):
    phrases_variants = attr.ib(default=attr.Factory(list))  # list of PhraseDescriptionWithReference


@attr.s
class PhraseDescriptionComposite(PhraseDescription):
    root_variants = attr.ib(default=attr.Factory(list))  # list of PhraseDescriptionWithReference
    root_formulae = attr.ib(default=attr.Factory(list))
    children_info = attr.ib(default=attr.Factory(dict))  # semantic role -> list of PhraseDescriptionWithReference
    children_formulae = attr.ib(default=attr.Factory(dict))


@attr.s
class PhraseDescriptionLookup(PhraseDescription):
    label = attr.ib(default=attr.Factory(list))


@attr.s
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
    onto = attr.ib(default=attr.Factory(tuple))
    punctuation_info = attr.ib(default=attr.Factory(tuple))
    grammemes = attr.ib(default=attr.Factory(frozenset))


class PhraseGrammar(object):
    def __init__(self, grammar_path):
        self._phrase_descriptions = {}
        self._raw_phrase_data = None
        self._inline_counter = 0
        self._load(grammar_path)

    def is_loaded(self):
        return self._raw_phrase_data is not None

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

                    phrase_description.grammemes.update(predecessor_phrase_descr.grammemes)
                    self._merge_gov_models(phrase_description.gov_models, predecessor_phrase_descr.gov_models)
                    self._merge_onto(phrase_description.onto, predecessor_phrase_descr.onto)
                    phrase_description.predecessors.add(predecessor_name)

            raw_gov_models = data.get('gov')

            if raw_gov_models:
                self._load_gov_models(phrase_description, raw_gov_models)

            onto = data.get('onto')

            if onto:
                assert isinstance(onto, dict)
                self._merge_onto(phrase_description.onto, onto)

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
                phrase_description.grammemes.update(gramm)

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
                    context = PhraseDescriptionContext(
                        predecessors=frozenset(phrase_description.predecessors),
                        onto=self._dict_to_system_of_tuples(phrase_description.onto),
                        grammemes=frozenset(phrase_description.grammemes),
                        gov_models=self._dict_to_system_of_tuples(phrase_description.gov_models),
                        punctuation_info=self._dict_to_system_of_tuples(phrase_description.punctuation_info),
                        tag=phrase_description.tag
                    )

                phrase_description.phrases_variants = self._load_phrases_from_list(
                    phrase_description.name, content_variants, context
                )
            else:
                self._load_composite_phrase(phrase_description, data)
        else:
            assert data
            phrase_description.phrases_variants = self._load_phrases_from_list(phrase_description.name, data, context)

        logger.info('%s successfully loaded.' % phrase_description.name)

        return phrase_description

    def _load_phrases_from_list(self, outer_phrase_name, phrase_list, context=PhraseDescriptionContext()):
        if not isinstance(phrase_list, list):
            phrase_list = [phrase_list]

        result = []

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
                    '%s.inline_%d' % (outer_phrase_name, self._inline_counter)
                )
                phrase_description = self._load_phrase_description(
                    phrase_description,
                    raw_description,
                    context
                )

                phrase_description.is_inline = True
                reference = raw_description.get('reference')
                if reference:
                    assert isinstance(reference, str)
                    reference = [ref.strip() for ref in reference.split('.')]

                self._inline_counter += 1

            result.append(PhraseDescriptionWithReference(phrase_description, reference))

        return result

    def _load_composite_phrase(self, phrase_description, data):
        root_variants = data.get('root')

        if root_variants:
            phrase_description.root_variants = self._load_phrases_from_list(phrase_description.name, root_variants)

            if not phrase_description.root_variants:
                raise ValueError('root section for phrase %s is empty' % phrase_description.name)

        for semantic_role, raw_children_variants in data.items():
            if semantic_role in {
                'root', 'extends', 'gov', 'tag', 'punctuation', 'gramm', 'onto', 'reference', 'content'
            }:
                continue

            phrase_description.children_info[semantic_role] = self._load_phrases_from_list(
                phrase_description.name, raw_children_variants
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
                PhraseGrammar._merge_onto(old_v, v)
            else:
                old_onto[k] = deepcopy(v)


@attr.s
class Phrase(object):
    phrase_description = attr.ib()
    # TODO гарантировать его заполнение
    onto = attr.ib(default=attr.Factory(set))
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
    inflected_form = attr.ib(default=None)

    def _depth(self):
        depth = 0
        parent = self.parent
        while parent:
            depth += 1
            parent = parent.parent
        return depth

    def __str__(self):
        if self.phrase_description.is_inline:
            name = self.phrase_description.predecessors[0].name if self.phrase_description.predecessors else '_'
        else:
            name = self.phrase_description.name

        result = '%s %s%s%s ' % (
            name,
            '<' + self.tag + '>' if self.tag else '',
            '(' + ','.join(self.grammemes) + ')' if self.grammemes else '',
            '(' + ','.join({'->' + cat for cat in self.agree_categories}) + ')' if self.agree_categories else '',
        )
        if self.lex:
            if self.prefix:
                result += self.prefix + ' '
            result += '%s::%s\n' % (self.lex, self.inflected_form)
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
    def __init__(self, grammar, morph_engine):
        self._grammar = grammar
        assert self._grammar.is_loaded(), 'Phrase generator can not be built over the uninitialized grammar'
        self._morph_engine = morph_engine
        self._detected_unknown_words = set()

    def get_detected_unknown_words(self):
        return list(self._detected_unknown_words)

    def generate(self, context, limit=1):
        logger.debug('Generating phrases for context %s' % context)

        while limit:
            # TODO не забыть _preprocess_onto(context)
            phrase = None

            limit -= 1

            self._inflect(phrase)
            yield phrase

        yield None

    def _update_grammemes(self, phrase, new_grammemes):
        if phrase.grammemes is None:
            return

        new_known_categories = set()
        for gr in new_grammemes:
            cat = self._morph_engine.get_category_for_grammeme(gr)
            if cat:
                if cat in new_known_categories:
                    logger.error('Duplicate grammeme for category %s detected, skipping' % cat)
                else:
                    new_known_categories.add(cat)

        union_grammemes = set()

        for gr in phrase.grammemes:
            cat = self._morph_engine.get_category_for_grammeme(gr)
            if not cat or cat not in new_known_categories:
                union_grammemes.add(gr)

        union_grammemes.update(new_grammemes)
        phrase.grammemes = union_grammemes

        if phrase.root:
            self._update_grammemes(phrase.root, new_grammemes)

    def _find_deepest_root(self, phrase):
        if phrase.lex:
            return phrase

        if phrase.root:
            return self._find_deepest_root(phrase.root)

        # in that case we are dealing with zero
        return phrase

    def _calculate_morph_an_results(self, phrase):
        if phrase.lex and phrase.morph_an_results is None:
            phrase.morph_an_results = self._morph_engine.analyse(phrase.lex)

            if not phrase.morph_an_results:
                self._detected_unknown_words.add(phrase.lex)

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

            self._calculate_morph_an_results(agree_host)

            if agree_host.morph_an_results:
                extracted_values = {category: None for category in categories_to_extract}
                for morph_an_result in agree_host.morph_an_results:
                    for gr in morph_an_result.gramm:
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
                            'Unable to extract the value of category "%s", word "%s"' % (c, agree_host.lex)
                        )

                extracted_values = {gr for gr in extracted_values.values() if gr}

                if extracted_values:
                    new_grammemes.update(extracted_values)

        if new_grammemes:
            self._update_grammemes(phrase, new_grammemes)

        if phrase.lex:
            if phrase.grammemes is not None:
                self._calculate_morph_an_results(phrase)
            if phrase.grammemes is not None and phrase.morph_an_results:
                inflection_variants = []
                for morph_an_result in phrase.morph_an_results:
                    inflection_variants += self._morph_engine.synthesise(
                        morph_an_result.paradigm_id,
                        phrase.grammemes or set()
                    )

                if inflection_variants:
                    phrase.inflected_form = random.choice(inflection_variants)
                else:
                    logger.warning('Unable to inflect "%s" with gramemmes %s' % (
                        phrase.lex,
                        phrase.grammemes
                    ))
                    phrase.inflected_form = phrase.lex
            else:
                phrase.inflected_form = phrase.lex

        if phrase.root:
            self._inflect(phrase.root)

        for child_phrases in phrase.children.values():
            for child_phrase in child_phrases:
                self._inflect(child_phrase)

    def render_string(self, phrase):
        if phrase.inflected_form:
            result = phrase.inflected_form
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
    def _lookup(context, keys):
        current_dict = context
        for key in keys:
            current_dict = current_dict.get(key, None)
            if not current_dict:
                return

        return current_dict

    @staticmethod
    def _preprocess_onto(onto):
        if isinstance(onto, list):
            return zip(range(len(onto)), onto)

        if isinstance(onto, dict):
            return {k: PhraseGenerator._preprocess_onto(v) for k, v in onto.items()}

        return onto

    @staticmethod
    def _onto_match(current_onto, check_onto):
        # check if current ontology context contains some fragment

        if isinstance(current_onto, list):
            for _, sub_onto in current_onto:
                if PhraseGenerator._onto_match(sub_onto, check_onto):
                    return True
            return False

        if not isinstance(current_onto, dict):
            return not check_onto

        for k, v in check_onto.items():
            current_v = current_onto.get(k)
            if not current_v:
                return False

            if not v:
                continue

            if isinstance(v, dict):
                if not isinstance(current_v, dict) or not PhraseGenerator._onto_match(current_v, v):
                    return False
            elif v != current_v:
                return False

        return True

    @staticmethod
    def _clashes(context1, context2):
        # TODO
        return False

    @staticmethod
    def _update_context(source_context, other_context):
        # TODO
        return False

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
    def _filter_variants(context, list_of_phrase_descr_with_ref):
        result = []
        for phrase_descr_with_ref in list_of_phrase_descr_with_ref:
            reference = phrase_descr_with_ref.reference
            phrase_description = phrase_descr_with_ref.phrase_description

            sub_context = PhraseGenerator._process_reference(reference, context)
            if sub_context is None:
                continue

            if PhraseGenerator._onto_match(sub_context, phrase_description.onto):
                result.append((sub_context, phrase_description, reference))
        return result

    _INTERNAL_RETRIES_COUNT = 10

    def _filter_and_choose(self, list_of_phrase_descr_with_ref, context, already_matched=None):
        filtered_variants = PhraseGenerator._filter_variants(context, list_of_phrase_descr_with_ref)

        if not filtered_variants:
            return None, None
        i = PhraseGenerator._INTERNAL_RETRIES_COUNT
        while i >= 0:
            sub_context, phrase_variant, reference = random.choice(filtered_variants)
            result = self._choose(phrase_variant, sub_context)
            if result:
                matched_context = PhraseGenerator.get_matched_context(result, reference)
                if not PhraseGenerator._clashes(already_matched, matched_context):
                    return result, matched_context
            i -= 1

        return None, None

    @staticmethod
    def get_matched_context(phrase, reference):
        current = deepcopy(phrase.onto)
        for ref in reference.reverse():
            current = {ref: current}

        return current

    def _choose(self, phrase_description, context):
        already_matched = deepcopy(phrase_description.onto)
        if isinstance(phrase_description, PhraseDescriptionDisjunctive):
            result, variant_matched_context = self._filter_and_choose(
                phrase_description.phrases_variants, context, already_matched
            )
            if not result:
                return None

            PhraseGenerator._update_context(already_matched, variant_matched_context)
        else:
            result = Phrase(phrase_description, grammemes=set(phrase_description.grammemes))

            if isinstance(phrase_description, PhraseDescriptionLex):
                result.lex = random.choice(phrase_description.lex_variants)
            elif isinstance(phrase_description, PhraseDescriptionLookup):
                result.lex = self._lookup(context, phrase_description.lookup)
            else:
                assert isinstance(phrase_description, PhraseDescriptionComposite)

                if phrase_description.root_variants:
                    result.root, root_matched_context = self._filter_and_choose(
                        phrase_description.root_variants, context, already_matched
                    )

                    if not result.root:
                        return None

                    PhraseGenerator._update_context(already_matched, root_matched_context)
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
                                children_phrases_and_contexts = chosen_children[semantic_role]
                            else:
                                children_phrases_and_contexts = []
                                chosen_children[semantic_role] = children_phrases_and_contexts

                            if len(children_phrases_and_contexts) < gov_model.max_cardinality:
                                candidates_left = True

                                child_phrase, child_matched_context = self._filter_and_choose(
                                    children_variants, context, already_matched
                                )

                                if not child_phrase and not children_phrases_and_contexts and not gov_model.optional:
                                    return None

                                if child_phrase:
                                    PhraseGenerator._update_context(already_matched, child_matched_context)

                                children_phrases_and_contexts.append((child_phrase, child_matched_context))

                    for semantic_role, children_phrases_and_contexts in chosen_children.items():
                        child_phrases = []
                        for child_phrase, child_matched_context in children_phrases_and_contexts:
                            if not child_phrase:
                                continue

                            gov_model = result.root.phrase_description.gov_models.get(semantic_role)
                            child_phrase.agree_categories = set(gov_model.agree_categories)
                            if gov_model.indecl:
                                child_phrase.grammemes = None
                            else:
                                self._update_grammemes(child_phrase, gov_model.grammemes)
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

        result.onto = already_matched
        return result
