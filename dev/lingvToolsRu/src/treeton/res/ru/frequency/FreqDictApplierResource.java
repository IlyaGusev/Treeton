/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.frequency;

import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.TypeIteratorInterface;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TrnType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FreqDictApplierResource extends Resource {
    private TrnType mtype;
    private FreqDict freqDict;
    private int baseKey, posKey, wordformKey, ipmKey;

    private Map<String, Double> base2maxipm = new HashMap<String, Double>();
    private Map<String, Token> base2start = new HashMap<String, Token>();
    private Map<String, Token> base2end = new HashMap<String, Token>();

    @Override
    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        TypeIteratorInterface it = storage.typeIterator(mtype);

        try {
            try {
                while (it.hasNext()) {
                    Treenotation trn = (Treenotation) it.next();
                    Object _base = trn.get(baseKey);
                    Object _pos = trn.get(posKey);
                    if (_base == null || _pos == null)
                        continue;
                    String base = _base.toString();
                    String pos = _pos.toString();

                    double ipm = freqDict.getIPM(base, pos);
                    trn.put(ipmKey, ipm);

                    Double maxIpm = base2maxipm.get(base);
                    if (maxIpm == null || maxIpm < ipm) {
                        base2maxipm.put(base, ipm);
                        base2start.put(base, trn.getStartToken());
                        base2end.put(base, trn.getEndToken());
                    }
                }
            } finally {
                it.close();
            }

            for (Map.Entry<String, Double> entry : base2maxipm.entrySet()) {
                String base = entry.getKey();
                Double maxIpm = entry.getValue();
                double maxIpmBase = freqDict.getMaxIPM(base);
                if (maxIpmBase > 0.0 && maxIpmBase > maxIpm) { //среди вариантов анализа нет самого частотного
                    it = storage.typeIterator(mtype, base2start.get(base), base2end.get(base));
                    try {
                        Treenotation maxIpmTrn = null;
                        while (it.hasNext()) {
                            Treenotation trn = (Treenotation) it.next();
                            Object _base = trn.get(baseKey);
                            if (_base == null)
                                continue;

                            // если словоформа не совпадает с начальной формой, не можем ничего сделать
                            if (!base.equals(_base.toString()) || !base.equals(_base.toString()))
                                continue;
                            // если есть вариант анализа с ipm=0.0 (отсутствующий в словаре с данным pos), считаем
                            // что просто pos не смэтчился и присваиваем ему максимальный ipm для данной начальной формы;
                            // если все варианты смэтчились, увеличиваем максимальный ipm до максимального для данной начальной формы
                            //todo: по идее тут надо порождать новый вариант анализа
                            double ipm = Double.valueOf(trn.get(ipmKey).toString());
                            if (ipm == 0.0) {
                                trn.put(ipmKey, maxIpmBase);
                                maxIpmTrn = null;
                                break;
                            } else if (ipm == maxIpm) {
                                maxIpmTrn = trn;
                            }
                        }
                        if (maxIpmTrn != null)
                            maxIpmTrn.put(ipmKey, maxIpmBase);
                    } finally {
                        it.close();
                    }
                }
            }
        } finally {
            base2maxipm.clear();
            base2start.clear();
            base2end.clear();
        }

        return null;
    }

    @Override
    protected void stop() {
        //do nothing
    }

    @Override
    protected void processTerminated() {
        //do nothing
    }

    @Override
    protected void init() throws ResourceInstantiationException {
        try {
            Object morph_type_name = getInitialParameters().get("Morph_type");
            mtype = getTrnContext().getType((String) morph_type_name);
            if (mtype == null)
                throw new ResourceInstantiationException("Unknown type: " + morph_type_name);
            baseKey = mtype.getFeatureIndex("base");
            posKey = mtype.getFeatureIndex("POS");
            wordformKey = mtype.getFeatureIndex("WORDFORM");
            ipmKey = mtype.getFeatureIndex("ipm");
            URL path;
            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("path"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Problem with " + getInitialParameters().get("path"));
            } catch (ContextException e) {
                throw new ResourceInstantiationException("Problem with " + getInitialParameters().get("path"));
            }

            freqDict = new ModernFreqDict(path.openStream());
        } catch (Exception e) {
            throw new ResourceInstantiationException("Invalid parameter value.", e);
        }
    }

    @Override
    protected void deInit() {
        freqDict = null;
    }
}
