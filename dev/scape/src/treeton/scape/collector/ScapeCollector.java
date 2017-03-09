/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape.collector;

import treeton.core.IntFeatureMapStaticLog;
import treeton.core.collector.TreetonCollector;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.fsm.ScapeTreenotationClassTree;
import treeton.core.fsm.logicset.*;
import treeton.core.util.StringArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScapeCollector extends TreetonCollector {
    public ScapeCollector(ByteBuffer buf, TreenotationsContext runtimeContext) {
        super(buf, runtimeContext);
    }

    public ScapeCollector(File f, TreenotationsContext runtimeContext) throws IOException {
        super(f, runtimeContext);
    }

    public ScapeCollector(TreenotationsContext runtimeContext) {
        super(runtimeContext);
    }

    protected void loadCollectables() {
        super.loadCollectables();
        loadCollectable(ScapeTreenotationClassTree.class, new ScapeTreenotationClassTree.ScapeTreenotationClassTreeCollectable());
        loadCollectable(ScapeTreenotationClassTree.FinalNode.class, new ScapeTreenotationClassTree.ScapeTreenotationClassTreeFinalNodeCollectable());
        loadCollectable(ScapeTreenotationClassTree.Distributer.class, new ScapeTreenotationClassTree.ScapeTreenotationClassTreeDistributerCollectable());
        loadCollectable(ScapeTreenotationClassTree.LogicFSMDistributer.class, new ScapeTreenotationClassTree.ScapeTreenotationClassTreeLogicFSMDistributerCollectable());
        loadCollectable(ScapeTreenotationClassTree.SequenceReader.class, new ScapeTreenotationClassTree.ScapeTreenotationClassTreeSequenceReaderCollectable());
        loadCollectable(IntFeatureMapStaticLog.class, new IntFeatureMapStaticLog.IntFeatureMapStaticLogCollectable());
        loadCollectable(StringArray.class, new StringArray.StringArrayCollectable());
        loadCollectable(LogicPair.class, new LogicPair.LogicPairCollectable());
        loadCollectable(LogicState.class, new LogicState.LogicStateCollectable());
        loadCollectable(LogicTerm.class, new LogicTerm.LogicTermCollectable());
        loadCollectable(LogicTerm.LogicTermEPS.class, new LogicTerm.LogicTermEPSCollectable());
        loadCollectable(SingleCharLogicSet.class, new SingleCharLogicSet.SingleCharLogicSetCollectable());
        loadCollectable(LogicFSM.class, new LogicFSM.LogicFSMCollectable());
    }


}
