/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.core.config.context.resources.xmlimpl.ResourcesContextXMLImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.gui.TreetonMainFrame;

import java.io.File;

public class ImportAndSaveTest {
    public static void main(String[] argv) throws Exception {
        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        ResourcesContextXMLImpl resContext = (ResourcesContextXMLImpl)
                ContextConfiguration.resourcesManager().get(ContextUtil.getFullName(trnContext));

        ResourceChain preprocessingChain = new ResourceUtils().createResourceChain(resContext.getResourceChainModel("BasicProsodyChain",true));

        preprocessingChain.setProgressListener(TreetonMainFrame.getMainFrame());
        preprocessingChain.addLogListener(TreetonMainFrame.getMainFrame());
        preprocessingChain.initialize(trnContext);
        preprocessingChain.removeLogListener(TreetonMainFrame.getMainFrame());
        preprocessingChain.setProgressListener(null);

        ResourceChain postprocessingChain = new ResourceUtils().createResourceChain(resContext.getResourceChainModel("AccentDisambiguationChain",true));
        postprocessingChain.setProgressListener(TreetonMainFrame.getMainFrame());
        postprocessingChain.addLogListener(TreetonMainFrame.getMainFrame());
        postprocessingChain.initialize(trnContext);
        postprocessingChain.removeLogListener(TreetonMainFrame.getMainFrame());
        postprocessingChain.setProgressListener(null);

        CorpusImportEngine importEngine = new CorpusImportEngine( preprocessingChain, postprocessingChain );

        Corpus corpus = new Corpus( "./domains/Russian.Prosody/store/esenin/corpus_final",trnContext );
        corpus.setCorpusLabel("Тестовый корпус Есенина");
        CorpusFolder folder = corpus.createFolder("От Песни о собаке до конца (лирика)",null);
        importEngine.Import( folder, new File("./domains/Russian.Prosody/doc/esenin_examples.txt"), TreetonMainFrame.getMainFrame() );

        postprocessingChain.deInitialize();
    }
}
