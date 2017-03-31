/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class LoadAndSaveTest {
    public static void main(String[] argv) throws Exception {
        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        Corpus corpus = new Corpus( "./corpus_small",trnContext );
        corpus.load();
        corpus.createFolder( "New Folder", corpus.getRootCorpusFolders().iterator().next() );
    }
}
