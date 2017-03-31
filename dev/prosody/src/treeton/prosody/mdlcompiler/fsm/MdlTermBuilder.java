/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.custom.Term;
import treeton.prosody.mdlcompiler.MdlConstants;
import treeton.prosody.mdlcompiler.grammar.ast.SyllBasicPatternElement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MdlTermBuilder {
    private Map<Term,Term> packer = new HashMap<Term, Term>();

    public Set<Term> getTerms(SyllBasicPatternElement element) {
        SyllablePatternType type = null;
        Object value = element.getSyllablePattern().getSymbolValue();
        if (MdlConstants.OBLIGATORY_STRESSED.equals(value)) {
            type = SyllablePatternType.OBLIGATORY_STRESSED;
        } else if (MdlConstants.STRESSED.equals(value)) {
            type = SyllablePatternType.STRESSED;
        } else if (MdlConstants.OBLIGATORY_UNSTRESSED.equals(value)) {
            type = SyllablePatternType.OBLIGATORY_UNSTRESSED;
        } else if (MdlConstants.UNSTRESSED.equals(value)) {
            type = SyllablePatternType.UNSTRESSED;
        }

        HashSet<Term> res = new HashSet<Term>();
        res.add(update(new SyllTerm(type)));

        return res;
    }

    private Term update(Term term) {
        Term nterm = packer.get(term);
        if (nterm == null) {
            nterm = term;
            packer.put(term,nterm);
        }

        return nterm;
    }

    public Term copyTerm(Term term) {
        return update(term);
    }
}
