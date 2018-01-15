# coding: utf-8
import logging
import os
import attr
import random
import ruamel.yaml as yaml

from copy import deepcopy

from label_formula import LabelFormula


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
    tag = attr.ib(default=None)
    is_inline = attr.ib(default=False)
    predecessors = attr.ib(default=attr.Factory(list))
    gov_models = attr.ib(default=attr.Factory(dict))
    punctuation_info = attr.ib(default=attr.Factory(dict))
    grammemes = attr.ib(default=attr.Factory(set))
    label_formula = attr.ib(default=None)

    def calculate_label_formulae(self):
        raise NotImplemented

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
    phrases_variants = attr.ib(default=attr.Factory(list))

    def calculate_label_formulae(self):
        self.label_formula = LabelFormula('or', *[phrase.label_formula for phrase in self.phrases_variants])


@attr.s
class PhraseDescriptionComposite(PhraseDescription):
    root_variants = attr.ib(default=attr.Factory(list))
    root_formulae = attr.ib(default=attr.Factory(list))
    children_info = attr.ib(default=attr.Factory(dict))
    children_formulae = attr.ib(default=attr.Factory(dict))

    def calculate_label_formulae(self):
        self.root_formulae = []

        for root_phrase_big in self.root_variants:
            gov_models_info = {}
            for root_phrase in PhraseDescription.disjunctive_generator(root_phrase_big):
                for semantic_role in self.children_info.keys():
                    gov_model = root_phrase.gov_models.get(semantic_role)
                    if not gov_model:
                        logger.warning(
                            'semantic role %s is not defined for phrase %s '
                            '(mentioned within phrase %s, root variant %s)' % (
                                semantic_role, root_phrase.name, self.name, root_phrase_big.name
                            )
                        )
                        continue

                    optional, max_cardinality = gov_models_info.get(semantic_role, (False, 0))
                    gov_models_info[semantic_role] = (
                        gov_model.optional or optional,
                        max(max_cardinality, gov_model.max_cardinality)
                    )

            child_formulae = []
            for semantic_role, children_variants in self.children_info.items():
                optional, max_cardinality = gov_models_info.get(semantic_role, (False, 0))
                if not max_cardinality:
                    continue

                inner_child_formula = LabelFormula('or', *[phrase.label_formula for phrase in children_variants])
                children_variants_formulae = []

                if optional:
                    children_variants_formulae.append(LabelFormula('atomic', set()))

                for i in range(max_cardinality):
                    children_variants_formulae.append(LabelFormula('and', *([inner_child_formula] * (i + 1))))

                child_formula = LabelFormula('or', *children_variants_formulae)
                child_formulae.append(child_formula)
                self.children_formulae[semantic_role] = child_formula

            child_formulae.append(root_phrase_big.label_formula)
            self.root_formulae.append(LabelFormula('and', *child_formulae))

        self.label_formula = (
            LabelFormula('or', *self.root_formulae)
            if self.root_formulae else LabelFormula('atomic', set())
        )


@attr.s
class PhraseDescriptionLookup(PhraseDescription):
    label = attr.ib(default=attr.Factory(list))

    def calculate_label_formulae(self):
        self.label_formula = LabelFormula('atomic', {self.lookup[0]})


@attr.s
class PhraseDescriptionLex(PhraseDescription):
    lex_variants = attr.ib(default=attr.Factory(list))

    def calculate_label_formulae(self):
        self.label_formula = LabelFormula('atomic', set())


@attr.s
class GovernmentModel(object):
    name = attr.ib()
    optional = attr.ib(default=False)
    orientation = attr.ib(default=None)
    max_cardinality = attr.ib(default=1)
    grammemes = attr.ib(default=attr.Factory(set))
    agree_categories = attr.ib(default=attr.Factory(set))
    prefixes = attr.ib(default=attr.Factory(set))
    indecl = attr.ib(default=False)


class PhraseGrammar(object):
    # TODO: safe loop detection

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
            self.get_phrase_description(full_name)

    def get_phrase_description(self, full_name, base_phrase=None):
        phrase_description = None if base_phrase else self._phrase_descriptions.get(full_name)

        if phrase_description:
            return phrase_description

        raw_phrase_description = self._raw_phrase_data.get(full_name)

        if raw_phrase_description is None:
            raise LookupError('Unable to find phrase %s' % full_name)

        phrase_description = self._load_phrase_description(full_name, raw_phrase_description, base_phrase)
        if not base_phrase:
            self._phrase_descriptions[full_name] = phrase_description
        return phrase_description

    @staticmethod
    def _globalize_name(context_name, name):
        if '.' not in name:
            name = context_name.split('.')[0] + '.' + name

        return name

    def _load_phrase_description(self, full_name, data, base_phrase=None):
        if isinstance(data, dict):
            lex = data.get('lex')
            lookup = data.get('lookup')
            content_variants = data.get('content_variants')

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

            if base_phrase:
                phrase_description.predecessors = list(base_phrase.predecessors)
                phrase_description.gov_models = deepcopy(base_phrase.gov_models)
                phrase_description.tag = base_phrase.tag
                phrase_description.punctuation_info = dict(base_phrase.punctuation_info)
                phrase_description.grammemes = set(base_phrase.grammemes)

            predecessors = data.get('extends')
            if predecessors:
                if not isinstance(predecessors, list):
                    predecessors = [predecessors]

                for predecessor_name in predecessors:
                    assert isinstance(predecessor_name, str)
                    predecessor_name = self._globalize_name(full_name, predecessor_name)
                    predecessor_phrase_descr = self.get_phrase_description(predecessor_name)

                    if predecessor_phrase_descr.tag:
                        phrase_description.tag = predecessor_phrase_descr.tag
                    if predecessor_phrase_descr.punctuation_info:
                        phrase_description.punctuation_info = dict(predecessor_phrase_descr.punctuation_info)

                    phrase_description.grammemes.update(predecessor_phrase_descr.grammemes)

                    self._merge_gov_models(phrase_description, predecessor_phrase_descr.gov_models)
                    phrase_description.predecessors.append(predecessor_phrase_descr)

            raw_gov_models = data.get('gov')

            if raw_gov_models:
                self._load_gov_models(phrase_description, raw_gov_models)

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
                if predecessors or raw_gov_models or tag or base_phrase or punctuation_info:
                    base_phrase = phrase_description
                else:
                    base_phrase = None
                phrase_description.phrases_variants = self._load_phrases_from_list(
                    full_name, content_variants, base_phrase
                )
            else:
                self._load_composite_phrase(phrase_description, data)
        else:
            phrase_description = PhraseDescriptionDisjunctive(full_name)
            assert data

            phrase_description.phrases_variants = self._load_phrases_from_list(full_name, data, base_phrase)

        phrase_description.calculate_label_formulae()
        logger.info('%s successfully loaded.' % full_name)
        logger.debug('Label formula is %s' % phrase_description.label_formula)

        return phrase_description

    def _load_phrases_from_list(self, context_name, phrase_list, base_phrase=None):
        if not isinstance(phrase_list, list):
            phrase_list = [phrase_list]

        result = []

        for raw_description in phrase_list:
            if isinstance(raw_description, str):
                phrase_description = self.get_phrase_description(
                    self._globalize_name(context_name, raw_description),
                    base_phrase
                )
            else:
                assert isinstance(raw_description, dict), 'raw_description is not a dict, phrase %s' % context_name
                # reading inline phrase_description

                phrase_description = self._load_phrase_description(
                    '%s.inline_%d' % (context_name, self._inline_counter),
                    raw_description,
                    base_phrase
                )

                phrase_description.is_inline = True

                self._inline_counter += 1

            result.append(phrase_description)

        return result

    def _load_composite_phrase(self, phrase_description, data):
        root_variants = data.get('root')

        if root_variants:
            phrase_description.root_variants = self._load_phrases_from_list(phrase_description.name, root_variants)

            if not phrase_description.root_variants:
                raise ValueError('root section for phrase %s is empty' % phrase_description.name)

        for semantic_role, raw_children_variants in data.items():
            if semantic_role in {'root', 'extends', 'gov', 'tag', 'punctuation', 'gramm'}:
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

                gov_model.agree_categories = set(agree)

            gramm = raw_gov_model.get('gramm')

            if gramm:
                assert isinstance(gramm, list)
                gov_model.grammemes = set(gramm)

            prefix = raw_gov_model.get('prefix')
            if prefix:
                assert isinstance(prefix, list)
                gov_model.prefixes = set(prefix)

            gov_models[semantic_role] = gov_model
        self._merge_gov_models(phrase_description, gov_models)

    @staticmethod
    def _merge_gov_models(phrase_description, gov_models):
        for semantic_role, gov_model in gov_models.items():
            old_model = phrase_description.gov_models.get(semantic_role)
            if not old_model:
                old_model = GovernmentModel(semantic_role)
                phrase_description.gov_models[semantic_role] = old_model

            old_model.optional = gov_model.optional
            old_model.orientation = gov_model.orientation
            old_model.max_cardinality = gov_model.max_cardinality
            old_model.grammemes.update(gov_model.grammemes)
            old_model.agree_categories.update(gov_model.agree_categories)
            old_model.prefixes.update(gov_model.prefixes)
            old_model.indecl = gov_model.indecl


@attr.s
class Phrase(object):
    phrase_description = attr.ib()
    root = attr.ib(default=None)
    children = attr.ib(default=attr.Factory(dict))
    lex = attr.ib(default=None)
    morph_an_results = attr.ib(default=None)
    inflected_form = attr.ib(default=None)
    tag = attr.ib(default=None)
    left_punctuator = attr.ib(default=None)
    right_punctuator = attr.ib(default=None)
    prefix = attr.ib(default=None)
    orientation = attr.ib(default=None)
    grammemes = attr.ib(default=attr.Factory(set))
    agree_categories = attr.ib(default=attr.Factory(set))
    parent = attr.ib(default=None)

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

    def generate(self, full_phrase_name, context, limit=1):
        phrase_description = self._grammar.get_phrase_description(full_phrase_name)
        if not phrase_description:
            raise LookupError('Phrase %s is not defined' % full_phrase_name)

        logger.debug('Generating phrase %s' % full_phrase_name)

        target_labels = frozenset(context.keys())
        while limit:
            if phrase_description.label_formula.match(target_labels):
                phrase = self._choose(phrase_description, context, target_labels)
                assert phrase
            else:
                break

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

    def _choose(self, phrase_description, context, target_labels):
        if isinstance(phrase_description, PhraseDescriptionDisjunctive):
            valid_variants = [
                v
                for v in phrase_description.phrases_variants
                if v.label_formula.match(target_labels)
            ]
            assert valid_variants

            phrase_variant = random.choice(valid_variants)
            result = self._choose(phrase_variant, context, target_labels)
            assert result
        else:
            result = Phrase(phrase_description, grammemes=set(phrase_description.grammemes))

            if isinstance(phrase_description, PhraseDescriptionLex):
                result.lex = random.choice(phrase_description.lex_variants)
            elif isinstance(phrase_description, PhraseDescriptionLookup):
                result.lex = self._lookup(context, phrase_description.lookup)
            else:
                assert isinstance(phrase_description, PhraseDescriptionComposite)

                if phrase_description.root_variants:
                    valid_root_variants = []
                    for root_variant, root_variant_formula in zip(
                        phrase_description.root_variants, phrase_description.root_formulae
                    ):
                        if root_variant_formula.match(target_labels):
                            valid_root_variants.append(root_variant)

                    assert valid_root_variants

                    success = False
                    children_subsets_choosing = None
                    while not success:
                        target_labels_copy = set(target_labels)

                        root_variant = random.choice(valid_root_variants)
                        possible_root_label_subsets = root_variant.label_formula.get_possible_subsets(
                            target_labels_copy
                        )

                        assert possible_root_label_subsets

                        root_label_subset = random.choice(possible_root_label_subsets)
                        result.root = self._choose(root_variant, context, root_label_subset)
                        assert result.root

                        result.root.parent = result

                        target_labels_copy.difference_update(root_label_subset)

                        children_subsets_choosing = {}

                        success = True
                        for semantic_role, children_variants in phrase_description.children_info.items():
                            gov_model = result.root.phrase_description.gov_models.get(semantic_role)

                            if not gov_model:
                                continue

                            children_formula = phrase_description.children_formulae[semantic_role]

                            possible_children_label_subsets = children_formula.get_possible_subsets(
                                target_labels_copy
                            )
                            if not possible_children_label_subsets:
                                success = False
                                break

                            children_label_subset = random.choice(possible_children_label_subsets)
                            children_subsets_choosing[semantic_role] = children_label_subset
                            target_labels_copy.difference_update(children_label_subset)

                        if target_labels_copy:
                            success = False

                    extended_children_choosing = {}
                    for semantic_role, children_labels in children_subsets_choosing.items():
                        gov_model = result.root.phrase_description.gov_models.get(semantic_role)
                        assert gov_model

                        success = False
                        while not success:
                            children_labels_copy = set(children_labels)

                            min_cardinality = 0 if gov_model.optional else 1
                            child_count = random.choice(range(min_cardinality, gov_model.max_cardinality + 1))

                            child_choosing = []
                            extended_children_choosing[semantic_role] = child_choosing
                            success = True
                            child_variants = phrase_description.children_info[semantic_role]
                            for j in range(child_count):
                                child_variant = random.choice(child_variants)
                                child_formula = child_variant.label_formula

                                possible_child_label_subsets = child_formula.get_possible_subsets(
                                    children_labels_copy
                                )
                                if not possible_child_label_subsets:
                                    success = False
                                    break

                                child_label_subset = random.choice(possible_child_label_subsets)
                                child_choosing.append((child_variant, child_label_subset))

                                children_labels_copy.difference_update(child_label_subset)

                            if children_labels_copy:
                                success = False

                    for semantic_role, child_choosing in extended_children_choosing.items():
                        child_phrases = []
                        for child_variant, child_label_subset in child_choosing:
                            child_phrase = self._choose(child_variant, context, child_label_subset)

                            assert child_phrase

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
        return result
