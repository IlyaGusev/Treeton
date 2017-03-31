/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler;

import treeton.prosody.mdlcompiler.grammar.ast.CompilationUnit;
import treeton.prosody.mdlcompiler.grammar.ast.MeterDescription;
import treeton.prosody.mdlcompiler.fsm.MdlFSMBuilder;
import treeton.prosody.mdlcompiler.fsm.Meter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MdlEngine implements Iterable<Meter> {
    private List<Meter> meters = new ArrayList<Meter>();
    private boolean reverseAutomata;

    public MdlEngine(String grammarPath, boolean reverseAutomata) throws Exception {
        this.reverseAutomata = reverseAutomata;
        MdlCompiler compiler = MdlCompiler.parseArgs(new String[]{"-s",grammarPath});
        if (compiler != null) {
           // BaseNode parseTree = compiler.parse();
            compiler.parse();
            // System.out.println(parseTree.dumpParseTree(""));
            compiler.doSemanticCheck();
            // print errors
            compiler.printMessages(System.err);

            MdlFSMBuilder fsmBuilder = new MdlFSMBuilder();

            CompilationUnit compilationUnit = compiler.getSourceFileUnits().values().iterator().next();

            int i=0;
            for (MeterDescription description : compilationUnit.getMeterDescriptionList()) {
                meters.add(new Meter(description,fsmBuilder,reverseAutomata,i++));
            }
        }
    }

    public Iterator<Meter> iterator() {
        return meters.iterator();
    }

    public boolean isReverseAutomata() {
        return reverseAutomata;
    }
}
