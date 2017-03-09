/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.scape.TrnTemplate;

import java.util.HashMap;
import java.util.Map;

public class Syllabizator extends Resource {
    int[] graphArr;
    int[] mappingArr;
    TrnType syllTp;
    TrnType inputType;
    TrnTemplate template;
    HashMap<Character, Integer> codeTable;

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        TypeIteratorInterface it;
        try {
            it = storage.typeIterator(getTrnContext().getTypes().get("Token"));
        } catch (TreetonModelException e) {
            throw new ExecutionException("Error with model", e);
        }
        while (it.hasNext()) {
            Treenotation trn = (Treenotation) it.next();
            if (template.match(trn)) {
                String word = trn.getText();
                ensureCapacity(word);
                int len = convertWordIntoGraph(word);

                Token from = trn.getStartToken();

                for (int i = 0; i < len; i++) {
                    if (graphArr[i] == 4) {
                        int j = i + 1;
                        while (j < len && graphArr[j] != 4) j++;
                        if (j < len) {
                            int minS = -1;
                            int minK = -1;
                            for (int k = i + 1; k <= j; k++) {
                                if (mappingArr[k] == -1)
                                    continue;
                                int hLeft = graphArr[k - 1];
                                int hRight = graphArr[k];
                                int S;
                                S = hLeft + hRight;
                                if (minS == -1 || S < minS) {
                                    minS = S;
                                    minK = k;
                                }
                            }
                            Token tok = trn.getStartToken();
                            int offs = (tok.getStartNumerator() / tok.getStartDenominator()) + mappingArr[minK];
                            while (true) {
                                int endoffs = tok.getEndNumerator() / tok.getEndDenominator();
                                if (offs < endoffs) {
                                    break;
                                }
                                tok = tok.getNextToken();
                            }
                            int start = tok.getStartNumerator() / tok.getStartDenominator();
                            Token beforeSplit;
                            Token afterSplit;
                            if (start == offs) {
                                beforeSplit = tok.getPreviousToken();
                                afterSplit = tok;
                            } else {
                                afterSplit = storage.splitToken(tok, offs, 1, tok.getText().substring(0, offs - start), tok.getText().substring(offs - start));
                                beforeSplit = tok;
                            }

                            Treenotation ntrn = TreetonFactory.newTreenotation(from, beforeSplit, syllTp);
                            storage.add(ntrn);

                            from = afterSplit;
                        } else {
                            Treenotation ntrn = TreetonFactory.newTreenotation(from, from, syllTp);
                            storage.add(ntrn);
                        }
                    }
                }
            }
        }
        return null;
    }

    private int convertWordIntoGraph(String word) {
        int q = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));

            if ("еёюя".indexOf(c) >= 0) {
                if (i == 0 || "аиоуэыеёюяъь".indexOf(word.charAt(i - 1)) >= 0) {
                    graphArr[q] = codeTable.get('й');
                    mappingArr[q++] = i;
                    graphArr[q] = codeTable.get(c);
                    mappingArr[q++] = -1;
                } else {
                    graphArr[q] = codeTable.get(c);
                    mappingArr[q++] = i;
                }
            } else if ("ъь".indexOf(c) < 0) {
                Integer integer = codeTable.get(c);
                graphArr[q] = integer == null ? 2 : integer; //затычка, чтобы не падало
                mappingArr[q++] = i;
            }
        }
        return q;
    }

    private void ensureCapacity(String word) {
        if (graphArr.length < word.length() * 2) {
            graphArr = new int[word.length() * 2];
        }
        if (mappingArr.length < word.length() * 2) {
            mappingArr = new int[word.length() * 2];
        }
    }

    public void init() throws ResourceInstantiationException {
        try {
            String templateString = (String) getInitialParameters().get("inputTemplate");
            template = new TrnTemplate();
            template.readIn(getTrnContext(), templateString.toCharArray(), 0, templateString.length() - 1, null);
            inputType = null;
            for (Treenotation trn : template) {
                if (inputType == null) {
                    inputType = trn.getType();
                } else if (!inputType.equals(trn.getType())) {
                    throw new ResourceInstantiationException("Input must contain treenotations of same TrnType");
                }
            }

            syllTp = getTrnContext().getTypes().get((String) getInitialParameters().get("targetType"));
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        } catch (ParseException e) {
            throw new ResourceInstantiationException("Error when parsing inputTemplate", e);
        }

        codeTable = new HashMap<Character, Integer>();

        codeTable.put('а', 4);
        codeTable.put('б', 2);
        codeTable.put('в', 3);
        codeTable.put('г', 2);
        codeTable.put('д', 2);
        codeTable.put('е', 4);
        codeTable.put('ё', 4);
        codeTable.put('ж', 1);
        codeTable.put('з', 1);
        codeTable.put('и', 4);
        codeTable.put('й', 3);
        codeTable.put('к', 2);
        codeTable.put('л', 3);
        codeTable.put('м', 3);
        codeTable.put('н', 3);
        codeTable.put('о', 4);
        codeTable.put('п', 2);
        codeTable.put('р', 3);
        codeTable.put('с', 1);
        codeTable.put('т', 2);
        codeTable.put('у', 4);
        codeTable.put('ф', 1);
        codeTable.put('х', 1);
        codeTable.put('ц', 2);
        codeTable.put('ч', 2);
        codeTable.put('ш', 1);
        codeTable.put('щ', 1);
        //codeTable.put('ъ',3);
        codeTable.put('ы', 4);
        //codeTable.put('ь',0);
        codeTable.put('э', 4);
        codeTable.put('ю', 4);
        codeTable.put('я', 4);

        graphArr = new int[200];
        mappingArr = new int[200];

    }

    public void deInit() {
    }

    public void stop() {
    }

    public void processTerminated() {
    }
}
