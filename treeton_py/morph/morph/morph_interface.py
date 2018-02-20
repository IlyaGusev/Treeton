import attr

from abc import ABCMeta, abstractmethod


@attr.s
class MorphAnResult:
    paradigm_id = attr.ib()
    lemma = attr.ib()
    accent = attr.ib()
    gramm = attr.ib()


@attr.s
class SynthResult:
    form = attr.ib()
    gramm = attr.ib()


class MorphEngine:
    __metaclass__ = ABCMeta

    @abstractmethod
    def analyse(self, word):
        return None

    @abstractmethod
    def synthesise(self, paradigm_id, gramm):
        return None

    @abstractmethod
    def get_possible_grammemes(self, category):
        return None

    @abstractmethod
    def get_category_for_grammeme(self, grammeme):
        return None
