/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.BlockStack;
import treeton.core.util.FileMapper;
import treeton.core.util.LinkedTrns;
import treeton.core.util.sut;
import treeton.morph.MorphException;
import treeton.morph.MorphInterface;
import treeton.morph._native.NativeRusMorphEngine;
import treeton.scape.ScapePhase;
import treeton.scape.ScapeResult;
import treeton.util.SpecialWords;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class StarlingMorphApplier extends Resource { //TODO: add conversion table
    private TrnType targetType;

    private int ID_feature;
    private int DICTID_feature;
    private int kind_feature;
    private int lang_feature;
    private int orth_feature;

    private ScapePhase predMorphPhase;
    private SpecialWords specialWords;

    private LinkedTrns.TrnsIterator linkedItr;
    private BlockStack stack;
    private StringBuffer tbuf;

    private MorphInterface morphEngine;
    private BlackBoard localBoard;

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;

        ArrayList<Treenotation> buffer = new ArrayList<Treenotation>();

        predMorphPhase.reset(storage, storage.firstToken(), storage.lastToken());
        Token rightEdge = null;

        while (predMorphPhase.nextStartPoint(rightEdge) != null) {
            rightEdge = null;
            ScapeResult r;

            while ((r = predMorphPhase.nextResult()) != null) {

                linkedItr.reset(r.getLastMatched());

                while (linkedItr.hasNext()) {
                    stack.push(linkedItr.nextTrn());
                }

                tbuf.setLength(0);

                while (!stack.isEmpty()) {
                    Treenotation trn = (Treenotation) stack.pop();
                    tbuf.append(trn.getText());
                }

                String source = tbuf.toString();

                if (morphEngine instanceof StarlingLowlevelMorphApi) {
                    StarlingLowlevelMorphApi engine = (StarlingLowlevelMorphApi) morphEngine;

                    String word = source.toLowerCase();
                    ArrayList<String[]> l = null;
                    try {
                        l = engine.processOneWord(word);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    if (l == null || l.size() == 0) {
                        //fillBlackBoard(null,null,source,null,null);
                        //localBoard.put(AGGROTYPE_feature,"unknown");
                        //buffer.add(TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), targetType, localBoard));

                        continue;
                    }

                    int i = buffer.size();
                    for (String[] arr : l) {
                        engine.processArray(buffer, arr, word, r.getStartToken(), r.getEndToken());
                    }

                    for (; i < buffer.size(); i++) {
                        Treenotation trn = buffer.get(i);

                        trn.put(ID_feature, -1);
                        trn.put(DICTID_feature, -1);
                        trn.put(orth_feature, sut.detectOrth(source));
                        trn.put(kind_feature, sut.detectKind(source));
                        trn.put(lang_feature, sut.detectLang(source));
                    }
                } else {
                    String word = source.toLowerCase().replace("ё", "е");

                    Collection<Properties> l = null;

                    ArrayList special = specialWords.getHypos(word);
                    for (Object o : special) {
                        if (l == null) {
                            l = new ArrayList<Properties>();
                        }
                        l.add((Properties) o);
                    }

                    if (l == null) {
                        try {
                            l = morphEngine.processOneWord(word, null);
                        } catch (MorphException e) {
                            Properties props = new Properties();
                            String s = e.toString();
                            props.setProperty("string", "Exception!!!!" + (s.length() > 20 ? s.substring(0, 20) : s));
                            l = Arrays.asList(props);
                        }
                    }

                    if (l == null || l.size() == 0) {
                        continue;
                    }

                    for (Object aL : l) {
                        localBoard.clean();
                        fillBlackBoard(source, (Properties) aL);
                        Treenotation trn = TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), targetType, localBoard);
                        buffer.add(trn);
                    }
                }
            }

            for (Treenotation t : buffer) {
                storage.addPostFactum(t);
            }
            buffer.clear();
        }

        storage.applyPostFactumTrns();

        /*if (morphEngine instanceof GrammAndZindexLogger) {
            try {
                ((GrammAndZindexLogger) morphEngine).logZindexesGrammsAndErrors();
            } catch (ContextException e) {
                throw new ExecutionException("Problem with Context",e);
            }
        }*/

        return null;
    }

    private void fillBlackBoard(String source, Properties properties) {
        localBoard.fill(targetType, properties);

        localBoard.put(orth_feature, sut.detectOrth(source));
        localBoard.put(kind_feature, sut.detectKind(source));
        localBoard.put(lang_feature, sut.detectLang(source));

        localBoard.put(ID_feature, -1);
        localBoard.put(DICTID_feature, -1);
    }

    public void stop() {
    }

    public void processTerminated() {
        predMorphPhase.reset(null);

        linkedItr.reset(null);
        stack = new BlockStack(5);
        tbuf.setLength(0);

        if (morphEngine instanceof GrammAndZindexLogger) {
            if (((GrammAndZindexLogger) morphEngine).isLogging()) {
                try {
                    ((GrammAndZindexLogger) morphEngine).logZindexesGrammsAndErrors();
                } catch (ContextException e) {
                    throw new RuntimeException("Context problem", e);
                }
            }
        }
        morphEngine.reset();
    }


    public void init() throws ResourceInstantiationException {
        try {
            targetType = getTrnContext().getTypes().get((String) getInitialParameters().get("targetType"));
            try {
                if (morphEngine != null)
                    morphEngine.deInit();
                URL lexpath = new URL(getResContext().getFolder(), (String) getInitialParameters().get("conversionLexRules"));
                URL inflpath = new URL(getResContext().getFolder(), (String) getInitialParameters().get("conversionInflRules"));

                Object o = getInitialParameters().get("port");
                if (o != null) {
                    int port = Integer.valueOf(o.toString());
                    morphEngine = new StarlingMorphEngine();
                    ((StarlingMorphEngine) morphEngine).init(getTrnContext(), targetType, lexpath.getPath(), inflpath.getPath(), port);
                    ((StarlingMorphEngine) morphEngine).setLoggingFolder(Boolean.TRUE.equals(getInitialParameters().get("logging")) ? getResContext().getFolder() : null);
                } else {
                    morphEngine = new NativeRusMorphEngine();
                    URL dictpath = new URL(getResContext().getFolder(), (String) getInitialParameters().get("nativeMorphDictPath"));
                    ((NativeRusMorphEngine) morphEngine).init(new File(dictpath.getPath()).getPath(), lexpath.getPath(), getTrnContext(), targetType);
                }
            } catch (Exception e) {

                throw new ResourceInstantiationException("Problem with model!!!", e);
            }


            stack = new BlockStack(5);
            tbuf = new StringBuffer();

            linkedItr = LinkedTrns.newTrnsIterator(null);
            URL path;
            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("predMorphPhase"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation", e);
            }
            predMorphPhase = new ScapePhase(getTrnContext().getTypes());
            try {
                char[] arr = FileMapper.map2memory(path.getPath());
                predMorphPhase.readIn(arr, 0, arr.length - 1);
                predMorphPhase.initialize();
            } catch (IOException e) {
                throw new ResourceInstantiationException("Wrong predmorph phase", e);
            } catch (ParseException e) {
                throw new ResourceInstantiationException("Wrong predmorph phase", e);
            }

            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("specialWords"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation", e);
            }

            specialWords = new SpecialWords(path.getPath());

            ID_feature = targetType.getFeatureIndex("ID");
            DICTID_feature = targetType.getFeatureIndex("DICTID");
            kind_feature = targetType.getFeatureIndex("kind");
            lang_feature = targetType.getFeatureIndex("lang");
            orth_feature = targetType.getFeatureIndex("orth");

            localBoard = TreetonFactory.newBlackBoard(50, false);
        } catch (ContextException e) {
            e.printStackTrace();
        } catch (TreetonModelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deInit() {
        morphEngine.deInit();
        morphEngine = null;
        predMorphPhase = null;
        linkedItr = null;
        stack = null;
        tbuf = null;
        localBoard = null;
    }


}
