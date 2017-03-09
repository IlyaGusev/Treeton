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
import treeton.scape.ScapePhase;
import treeton.scape.ScapeResult;
import treeton.util.SpecialWords;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class StarlingOnlyAccentMorphApplier extends Resource { //TODO: add conversion table
    private TrnType targetType;

    private int ID_feature;
    private int DICTID_feature;
    private int kind_feature;
    private int lang_feature;
    private int orth_feature;

    private ScapePhase predMorphPhase;
    private SpecialWords specialWords;
    private Map<String, Integer> specialAccents;

    private LinkedTrns.TrnsIterator linkedItr;
    private BlockStack stack;
    private StringBuffer tbuf;

    private BagOfWordsAccentDetector accentDetector;
    private BlackBoard localBoard;

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;

        ArrayList<Treenotation> buffer = new ArrayList<Treenotation>();

        predMorphPhase.reset(storage, storage.firstToken(), storage.lastToken());

        while (predMorphPhase.nextStartPoint((Token) null) != null) {
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

                String source = tbuf.toString().toLowerCase();

                if (source.endsWith("-то")) {
                    source = source.substring(0, source.length() - 3);
                } else if (source.endsWith("-либо")) {
                    source = source.substring(0, source.length() - 5);
                } else if (source.endsWith("-нибудь")) {
                    source = source.substring(0, source.length() - 7);
                }

                String word = source.replace("ё", "е");

                byte[] accentPlaces;
                if (specialAccents.containsKey(source)) {
                    accentPlaces = new byte[1];
                    accentPlaces[0] = (byte) specialAccents.get(source).byteValue();
                } else {
                    int yoPlace = source.indexOf('ё');

                    if (yoPlace >= 0) {
                        accentPlaces = new byte[1];
                        accentPlaces[0] = (byte) yoPlace;
                    } else {
                        accentPlaces = accentDetector.getAccentPlaces(word);
                    }
                }

                Collection<Properties> l = null;
                if (accentPlaces == null) {
                    ArrayList special = specialWords.getHypos(word);
                    for (Object o : special) {
                        if (l == null) {
                            l = new ArrayList<>();
                        }
                        l.add((Properties) o);
                    }
                } else {
                    BagOfWordsAccentDetector.KlitikInfo klitikInfo = accentDetector.getKlitikInfo(word);

                    l = new ArrayList<>();
                    for (byte accentPlace : accentPlaces) {
                        Properties props = new Properties();

                        if (klitikInfo == BagOfWordsAccentDetector.KlitikInfo.YES ||
                                klitikInfo == BagOfWordsAccentDetector.KlitikInfo.AMBIG && accentPlace == -1) {
                            props.put("Klitik", "klitik");
                        }

                        props.put("ACCPL", Integer.toString(accentPlace));
                        l.add(props);
                    }
                }

                if (l == null || l.size() == 0) {
                    continue;
                }

                for (Object props : l) {
                    localBoard.clean();
                    fillBlackBoard(source, (Properties) props);
                    Treenotation trn = TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), targetType, localBoard);
                    buffer.add(trn);
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
    }


    public void init() throws ResourceInstantiationException {
        try {
            targetType = getTrnContext().getTypes().get((String) getInitialParameters().get("targetType"));
            try {
                if (accentDetector != null) {
                    accentDetector = null;
                }
                URL bagOfWordsPath = new URL(getResContext().getFolder(), (String) getInitialParameters().get("bagOfWordsPath"));
                byte[] data = FileMapper.map2bytes(bagOfWordsPath.getPath());
                accentDetector = new BagOfWordsAccentDetector(data, 0);
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
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("specialAccents"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation", e);
            }

            specialAccents = readSpecialAccents(path.getPath());

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

    private Map<String, Integer> readSpecialAccents(String filePath) throws IOException {
        Map<String, Integer> result = new HashMap<>();

        System.out.println("specAccentsFile: " + filePath);
        InputStream ist = new FileInputStream(filePath);
        InputStreamReader isr = new InputStreamReader(ist);

        BufferedReader brd = new BufferedReader(isr);
        String line;
        while ((line = brd.readLine()) != null) {
            line = line.trim().toLowerCase();

            if (!line.isEmpty() && !line.startsWith("#")) {
                int quotePl = line.indexOf('\'');

                if (quotePl > 0) {
                    result.put(line.substring(0, quotePl) + line.substring(quotePl + 1), quotePl - 1);
                } else {
                    result.put(line, -1);
                }
            }
        }

        brd.close();
        isr.close();
        ist.close();

        return result;
    }

    public void deInit() {
        accentDetector = null;
        predMorphPhase = null;
        linkedItr = null;
        stack = null;
        tbuf = null;
        localBoard = null;
    }


}
