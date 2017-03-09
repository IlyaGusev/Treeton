/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.*;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationXMLImpl;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.LoggerLogListener;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.LoggerProgressListener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BigTextProcessor {
    private static final Logger logger = Logger.getLogger(BigTextProcessor.class);

    private ResourceChain chain;
    private File bigFile;
    private int linesPerBlock;
    private BigTextProcessorListener listener;
    private TreenotationStorage tStorage;
    private int startBlockNumber = -1;

    public BigTextProcessor(ResourceChain chain, File bigFile, int linesPerBlock, BigTextProcessorListener listener) {
        this.chain = chain;
        this.bigFile = bigFile;
        this.linesPerBlock = linesPerBlock;
        this.listener = listener;

        tStorage = TreetonFactory.newTreenotationStorage(chain.getTrnContext());
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        //Logger.getLogger("treeton.morph._native.NativeRusMorphEngine").setLevel(Level.TRACE);


        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationXMLImpl.class);
        ContextConfiguration.createInstance();
        ResourcesContext resContext = ContextConfiguration.getInstance().getResourcesContextManager().get("Common.Russian");

        ResourceChain chain = new ResourceUtils().createResourceChain(resContext, "NativeMorphChain");
        chain.initialize(ContextConfiguration.getInstance().getTreenotationsContextManager().get("Common.Russian"));
        chain.setProgressListener(new LoggerProgressListener("BigTextProcessor", logger));
        chain.addLogListener(new LoggerLogListener(logger));

        final FileWriter out = new FileWriter(new File("C:\\projects\\morphparsers\\rueval_2010_main\\output1_Green.txt"));

        BigTextProcessor bigTextProcessor = new BigTextProcessor(
                chain,
//              new File("C:\\projects\\treeton\\dev\\lingvToolsRu\\src\\treeton\\morph\\_native\\NativeRusMorphEngine.java"),250,
                new File("C:\\projects\\morphparsers\\rueval_2010_main\\rueval_2010_main_corrected4.txt"), 250,
                new BigTextProcessorListener() {
                    int nBlock = 1;

                    public void blockProcessed(String blockSource, String result, TreenotationStorage storage) {
                        System.out.println("Finished block " + nBlock++ + ": " + blockSource.substring(0, 20) + "...");
                        try {
                            TrnType tokenType = storage.getTypes().get("Token");
                            TrnType morphType = storage.getTypes().get("Gramm");

                            export(storage, out, tokenType, morphType);

                            FileWriter writer = new FileWriter(new File("lastBlockNumber"));
                            writer.append(Integer.toString(nBlock));
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        TypeIteratorInterface it;
//                        try {
//                            it = storage.typeIterator(storage.getTypes().get("Gramm"));
//                            while (it.hasNext()) {
//                                System.out.println(it.next());
//                            }
//                        } catch (TreetonModelException e) {
//                            e.printStackTrace();
//                        }
                    }

                    public void exceptionDuringExecution(String blockSource, ExecutionException e) {
                        e.printStackTrace();
                    }
                }
        );

        File f = new File("lastBlockNumber");
        if (f.exists()) {
            FileReader reader = new FileReader(f);
            char[] arr = new char[(int) f.length()];
            reader.read(arr, 0, (int) f.length());
            reader.close();
            bigTextProcessor.setStartBlockNumber(Integer.valueOf(new String(arr)));
        }

        bigTextProcessor.process();

        out.close();
    }

    private static void export(TreenotationStorage storage, FileWriter out, TrnType tokenType, TrnType morphType) throws TreetonModelException, IOException {
        TypeIteratorInterface it = storage.typeIterator(tokenType);

        Token rightEdge = null;

        while (it.hasNext()) {
            Treenotation tok = (Treenotation) it.next();

            if (rightEdge != null && tok.getEndToken().compareTo(rightEdge) <= 0)
                continue;

            List<Treenotation> morphs = getMorphs(storage, tok, morphType);

            out.append(tok.getText()).append("\n");

            if (morphs.size() > 0) {
                rightEdge = morphs.get(0).getEndToken();
                appendMorphs(out, morphs);
            }
        }
    }

    private static void appendMorphs(FileWriter out, List<Treenotation> morphs) throws IOException {
        for (Treenotation morph : morphs) {
            String pos = morph.get("POS").toString();
            if ("N".equals(pos)) {
                pos = "S";
            } else if ("A".equals(pos)) {
            } else if ("V".equals(pos)) {
            } else if ("PREP".equals(pos)) {
                pos = "PR";
            } else if ("CONJ".equals(pos)) {
            } else if ("INTJ".equals(pos)) {
                pos = "ADV";
            } else if ("PCL".equals(pos)) {
                pos = "ADV";
            }

            String base = morph.get("base").toString();

            out.append("\t").append(base).append("\t").append(pos);
            if ("S".equals(pos) || "A".equals(pos) || "V".equals(pos)) {
                out.append("\t");

                Object _tns = morph.get("TNS");
                String tns = _tns == null ? null : _tns.toString();
                if ("fut".equals(tns)) {
                    tns = "pres";
                }

                Object _repr = morph.get("REPR");
                String repr = _repr == null ? null : _repr.toString();

                if ("gern".equals(repr)) {
                    repr = "ger";
                } else if ("part".equals(repr)) {
                    repr = "partcp";
                } else if ("fin".equals(repr)) {
                    repr = null;
                }

                Object _md = morph.get("MD");
                String md = _md == null ? null : _md.toString();
                if ("imp".equals(md)) {
                    md = "imper";
                } else {
                    md = null;
                }

                Object _prs = morph.get("PRS");
                String prs = _prs == null ? null : _prs.toString();
                if ("1".equals(prs)) {
                    prs = "1p";
                } else if ("2".equals(prs)) {
                    prs = "2p";
                } else if ("3".equals(prs)) {
                    prs = "3p";
                }

                Object _vox = morph.get("VOX");
                String vox = "partcp".equals(repr) ? (_vox == null ? "act" : _vox.toString()) : null;

                Object _gend = morph.get("GEND");
                String gend = _gend == null ? null : _gend.toString();
                if ("mf".equals(gend)) {
                    gend = "m";
                }

                Object _cas = morph.get("CAS");
                String cas = _cas == null ? null : _cas.toString();
                if ("inst".equals(cas)) {
                    cas = "ins";
                } else if ("gen2".equals(cas)) {
                    cas = "gen";
                } else if ("prp2".equals(cas)) {
                    cas = "loc";
                } else if ("prp".equals(cas)) {
                    cas = "loc";
                }

                Object _nmb = morph.get("NMB");
                String nmb = _nmb == null ? null : _nmb.toString();

                appendCategories(out, tns, repr, md, prs, vox, gend, nmb, cas);
            }

            out.append("\n");
        }
    }

    private static void appendCategories(FileWriter out, String... categories) throws IOException {
        boolean b = false;
        for (String category : categories) {
            if (category == null)
                continue;

            if (b) {
                out.append(",");
            }
            out.append(category);
            b = true;
        }
    }

    private static List<Treenotation> getMorphs(TreenotationStorage storage, Treenotation tok, TrnType morphType) {
        List<Treenotation> res = new ArrayList<Treenotation>();

        Token min = tok.getStartToken();
        Token max = tok.getEndToken();

        TypeIteratorInterface it = storage.typeIterator(morphType, tok.getStartToken(), tok.getEndToken());
        while (it.hasNext()) {
            Treenotation morph = (Treenotation) it.next();

            if (morph.get("POS") == null || morph.get("base") == null)
                continue;

            if (morph.getStartToken().compareTo(min) < 0 || morph.getEndToken().compareTo(max) > 0) {
                res.clear();
                min = morph.getStartToken();
                max = morph.getEndToken();
            }

            res.add(morph);
        }

        return res;
    }

    public void process() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(bigFile));
        int n = 0;
        int curBlock = 1;
        StringBuffer buf = new StringBuffer();
        while (reader.ready()) {
            String s = reader.readLine().trim();
            buf.append(s).append("\n");
            n++;

            if (n == linesPerBlock) {
                processBlock(buf.toString(), curBlock++);
                n = 0;
                buf.setLength(0);
            }
        }

        if (n > 0) {
            processBlock(buf.toString(), curBlock);
        }

        reader.close();
    }

    private void processBlock(String s, int blockNumber) {
        try {
            String res = (startBlockNumber == -1 || startBlockNumber < blockNumber) ? chain.execute(s, tStorage, new HashMap<String, Object>()) : "";
            listener.blockProcessed(s, res, tStorage);
        } catch (ExecutionException e) {
            listener.exceptionDuringExecution(s, e);
        }
    }

    public void setStartBlockNumber(int startBlockNumber) {
        this.startBlockNumber = startBlockNumber;
    }
}
