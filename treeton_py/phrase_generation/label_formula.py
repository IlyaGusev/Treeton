# coding: utf-8
from copy import deepcopy


# noinspection PyProtectedMember
class LabelDisjunction(object):
    def __init__(self, *label_sets):
        assert all(isinstance(label_set, set) for label_set in label_sets)
        self._prepare_disjunction(label_sets)
        self._all_labels = set.union(*label_sets)

    def _prepare_disjunction(self, label_sets):
        self._disjunction_of_label_sets = [frozenset(s) for s in label_sets]
        self._sort_disjunction()

    def _sort_disjunction(self):
        self._disjunction_of_label_sets.sort(
            key=lambda item: (-len(item), sorted(item))
        )

    def __str__(self):
        return '|'.join([str(sorted(label_set)) for label_set in self._disjunction_of_label_sets])

    def __repr__(self):
        return self.__str__()

    def is_empty(self):
        return len(self._disjunction_of_label_sets) == 1 and not self._disjunction_of_label_sets[0]

    def has_conflict(self, other_disjunction):
        return not self._all_labels.isdisjoint(other_disjunction._all_labels)

    def multiply(self, other_disjunction, label_filter=None):
        new_disjunction_of_label_sets = set()

        for label_set in self._disjunction_of_label_sets:
            for other_label_set in other_disjunction._disjunction_of_label_sets:
                if label_set.isdisjoint(other_label_set):
                    newset = frozenset.union(label_set, other_label_set)

                    if not label_filter or newset.issubset(label_filter):
                        new_disjunction_of_label_sets.add(frozenset(newset))

        self._prepare_disjunction(new_disjunction_of_label_sets)
        self._all_labels = set.union(self._all_labels, other_disjunction._all_labels)

    def extend(self, other_disjunction):
        new_disjunction_of_label_sets = set(self._disjunction_of_label_sets)
        new_disjunction_of_label_sets.update(other_disjunction._disjunction_of_label_sets)

        self._prepare_disjunction(new_disjunction_of_label_sets)
        self._all_labels = set.union(self._all_labels, other_disjunction._all_labels)

    def equals(self, other):
        return self._disjunction_of_label_sets == other._disjunction_of_label_sets

    def max_subset(self, test_label_set):
        for label_set in self._disjunction_of_label_sets:
            if label_set.issubset(test_label_set):
                return label_set

        return None


# noinspection PyProtectedMember
class LabelFormula(object):
    def __init__(self, mode, *formulae):
        self._conjunction_of_disjunctions = None
        self._is_nullable = False

        if mode in ('and', 'or'):
            assert all(isinstance(formula, LabelFormula) for formula in formulae)
            assert len(formulae)

            self._conjunction_of_disjunctions = []
            self._is_nullable = mode == 'and'

            for formula in formulae:
                if mode == 'and':
                    self._and(formula)
                else:
                    self._or(formula)
        elif mode == 'atomic':
            self._conjunction_of_disjunctions = [LabelDisjunction(*formulae)]
            self._is_nullable = set() in formulae
        else:
            assert False, 'unsupported mode %s for LabelFormula' % mode

    def __str__(self):
        result = '*'.join(['{%s}' % disjunction for disjunction in self._conjunction_of_disjunctions])
        if self._is_nullable:
            result += '(nullable)'
        return result

    def __repr__(self):
        return self.__str__()

    def _and(self, other_formula):
        if len(self._conjunction_of_disjunctions) == 1 and self._conjunction_of_disjunctions[0].is_empty():
            self._conjunction_of_disjunctions = deepcopy(other_formula._conjunction_of_disjunctions)
        else:
            for other_disjunction in other_formula._conjunction_of_disjunctions:
                found_conflict = False
                for disjunction in self._conjunction_of_disjunctions:
                    if disjunction.has_conflict(other_disjunction):
                        disjunction.multiply(other_disjunction)
                        found_conflict = True
                        break
                if not found_conflict:
                    self._conjunction_of_disjunctions.append(deepcopy(other_disjunction))

        self._is_nullable = self._is_nullable and other_formula._is_nullable

    def _or(self, other_formula):
        common_disjunctions = []
        uncommon_other_disjunctions = []

        matched_disjunctions = set()

        for other_disjunction in other_formula._conjunction_of_disjunctions:
            match_found = False
            for disjunction in self._conjunction_of_disjunctions:
                if disjunction in matched_disjunctions:
                    continue

                if other_disjunction.equals(disjunction):
                    common_disjunctions.append(other_disjunction)
                    matched_disjunctions.add(disjunction)
                    match_found = True
                    break

            if not match_found:
                uncommon_other_disjunctions.append(other_disjunction)

        uncommon_self_disjunctions = []

        for disjunction in self._conjunction_of_disjunctions:
            if disjunction not in matched_disjunctions:
                uncommon_self_disjunctions.append(disjunction)

        self._conjunction_of_disjunctions = common_disjunctions

        if uncommon_self_disjunctions:
            multiplied_self_disjunction = LabelDisjunction(set())
            for disjunction in uncommon_self_disjunctions:
                multiplied_self_disjunction.multiply(disjunction)
        else:
            multiplied_self_disjunction = None

        if uncommon_other_disjunctions:
            multiplied_other_disjunction = LabelDisjunction(set())
            for other_disjunction in uncommon_other_disjunctions:
                multiplied_other_disjunction.multiply(other_disjunction)
        else:
            multiplied_other_disjunction = None

        if multiplied_self_disjunction:
            if multiplied_other_disjunction:
                multiplied_self_disjunction.extend(multiplied_other_disjunction)
        else:
            multiplied_self_disjunction = multiplied_other_disjunction

        if multiplied_self_disjunction:
            self._conjunction_of_disjunctions.append(multiplied_self_disjunction)

        self._is_nullable = self._is_nullable or other_formula._is_nullable

    def match(self, label_set):
        if not label_set:
            return self._is_nullable

        test_set = set(label_set)

        for disjunction in self._conjunction_of_disjunctions:
            max_subset = disjunction.max_subset(test_set)

            if max_subset is None:
                return False

            test_set.difference_update(max_subset)

        return not test_set

    def get_possible_subsets(self, label_set):
        if not label_set:
            return [frozenset()] if self._is_nullable else []

        result = LabelDisjunction(set())
        for disjunction in self._conjunction_of_disjunctions:
            result.multiply(disjunction, label_filter=label_set)

        return result._disjunction_of_label_sets
