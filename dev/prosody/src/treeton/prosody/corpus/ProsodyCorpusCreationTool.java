/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.LoggerLogListener;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.xmlimpl.ResourcesContextXMLImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.util.LoggerProgressListener;

import java.io.*;

public class ProsodyCorpusCreationTool {
    private static final Logger logger = Logger.getLogger(ProsodyCorpusCreationTool.class);
    private static final String LOGGER_CONFIGURATION_FILE = "logger.config";

    public static void main(String[] argv) throws Exception {
        if (argv.length < 4 || argv.length > 5) {
            System.out.println("Usage: ProsodyCorpusCreationTool sourceTextFile outputFolder FirstPassResourceChainName corpusLabel [SecondPassResourceChainName]");
            return;
        }

        String sourceTextPath = argv[0];
        String outputPath = argv[1];
        String firstPassResourceChainName = argv[2];
        String corpusLabel = argv[3];
        String secondPassResourceChainName = argv.length == 5 ? argv[4] : null;


        if (!new File(sourceTextPath).exists()) {
            System.out.println(sourceTextPath + " doesn't exist");
            return;
        }
        if (!new File(outputPath).exists()) {
            if (!new File(outputPath).mkdir()) {
                System.out.println("Unable to create folder " + outputPath);
                return;
            }
        }

        if (new File(LOGGER_CONFIGURATION_FILE).exists()) {
            System.out.println("Using logging configuration from "+new File(LOGGER_CONFIGURATION_FILE).getPath());
            PropertyConfigurator.configure(LOGGER_CONFIGURATION_FILE);
        } else {
            System.out.println("Working according to default logging policy");
            BasicConfigurator.resetConfiguration();
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.INFO);
        }

        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        ResourcesContextXMLImpl resContext = (ResourcesContextXMLImpl)
                ContextConfiguration.resourcesManager().get(ContextUtil.getFullName(trnContext));
        ResourceChainModel chainModel = resContext.getResourceChainModel(firstPassResourceChainName, true);

        if (chainModel == null) {
            System.out.println("Unable to find resource chain " + firstPassResourceChainName);
            return;
        }

        ResourceChain firstChain = new ResourceUtils().createResourceChain(chainModel);

        LoggerProgressListener progressListener = new LoggerProgressListener("Corpus creation tool", logger);
        firstChain.setProgressListener(progressListener);
        LoggerLogListener loggerLogListener = new LoggerLogListener(logger);
        firstChain.addLogListener(loggerLogListener);
        firstChain.initialize(trnContext);
        firstChain.removeLogListener(loggerLogListener);
        firstChain.setProgressListener(null);

        ResourceChain secondChain = null;

        if (secondPassResourceChainName != null) {
            chainModel = resContext.getResourceChainModel(secondPassResourceChainName, true);

            if (chainModel == null) {
                System.out.println("Unable to find resource chain " + secondPassResourceChainName);
                return;
            }

            secondChain = new ResourceUtils().createResourceChain(chainModel);

            progressListener = new LoggerProgressListener("Corpus creation tool", logger);
            secondChain.setProgressListener(progressListener);
            loggerLogListener = new LoggerLogListener(logger);
            secondChain.addLogListener(loggerLogListener);
            secondChain.initialize(trnContext);
            secondChain.removeLogListener(loggerLogListener);
            secondChain.setProgressListener(null);
        }

        CorpusImportEngine importEngine = new CorpusImportEngine(firstChain,secondChain);

        Corpus corpus = new Corpus(outputPath, trnContext);
        corpus.setCorpusLabel(corpusLabel);
        CorpusFolder folder = corpus.createFolder(corpusLabel, null);
        importEngine.Import(folder, new File(sourceTextPath), progressListener);
    }
}
