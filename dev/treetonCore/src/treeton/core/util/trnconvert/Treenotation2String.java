/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.trnconvert;


import treeton.core.TString;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.util.ObjectPair;

import java.util.*;

/**
 * Класс для конвертации тринотаций в строку.
 */
public class Treenotation2String {
    private final TrnRelWriter trnrelWriter;
    private final boolean collapseBoxes;

    public Treenotation2String() {
        this(new DefaultTrnRelWriter(), true);
    }

    public Treenotation2String(TrnRelWriter trnrelWriter, boolean collapseBoxes) {
        this.trnrelWriter = trnrelWriter;
        this.collapseBoxes = collapseBoxes;
    }

    /**
     * Заменяет "коробочки" (тринотации, из которых нет root-связей, но есть null-связи, ведущие к тривиальным
     * тринотациям, покрывающим один и тот же фрагмент предложения) на первую из содержащихся внутри тринотаций
     *
     * @param map
     */
    private static Map<Treenotation, Treenotation> collapseBoxes(Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> map) throws TreetonModelException {
        Map<Treenotation, Treenotation> subst = new HashMap<Treenotation, Treenotation>();
        for (Treenotation trn : map.keySet()) {
            if (!isBox(trn, map))
                continue;
            // вместо коробочки будем подставлять первый ее элемент (первую тринотацию связанную с коробочкой null-связью)
            for (ObjectPair<Treenotation, TrnRelationType> pair : map.get(trn)) {
                if (pair.getSecond() != null)
                    continue;
                subst.put(trn, pair.getFirst());
                break;
            }
        }

        //во входящих в коробочку связях заменяем коробочку на первый её элемент
        for (Map.Entry<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> entry : map.entrySet()) {
            for (ObjectPair<Treenotation, TrnRelationType> pair : entry.getValue()) {
                final Treenotation s = subst.get(pair.getFirst());
                if (s != null)
                    pair.setObjects(s, pair.getSecond());
            }
        }

        //из исходящих связей убираем null-связи
        for (Map.Entry<Treenotation, Treenotation> entry : subst.entrySet()) {
            final SortedSet<ObjectPair<Treenotation, TrnRelationType>> set = map.remove(entry.getKey());
            final Iterator<ObjectPair<Treenotation, TrnRelationType>> it = set.iterator();
            while (it.hasNext()) {
                final ObjectPair<Treenotation, TrnRelationType> pair = it.next();
                if (pair.getSecond() == null)
                    it.remove();
            }
            if (!set.isEmpty()) // если из коробочки исходили обычные связи, теперь они будут исходить из её первого элемента
                map.put(entry.getValue(), set);
        }

        return subst;
    }

    private static boolean isBox(Treenotation trn, Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> map) throws TreetonModelException {
        final SortedSet<ObjectPair<Treenotation, TrnRelationType>> set = map.get(trn);
        if (set == null)
            return false; // листовые тринотации не являются "коробочками"

        Token start = null, end = null;
        for (ObjectPair<Treenotation, TrnRelationType> pair : set) {
            final TrnRelationType rel = pair.getSecond();

            if (rel == null) {
                final Treenotation child = pair.getFirst();
                if (map.get(child) != null)
                    return false; // внутри "коробочки" лежат только тривиальные тринотации
                if (start != null && !start.equals(child.getStartToken()) || end != null && !end.equals(child.getEndToken()))
                    return false; // все элементы "коробочки" должны начинаться и заканчиваться одним токеном
                start = child.getStartToken();
                end = child.getEndToken();
            } else if (rel.isRoot())
                return false;
        }
        return true;
    }

    public String convert(Treenotation trn) throws Exception {
        StringBuffer buf = new StringBuffer();
        final Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> map = TreenotationConverter.getTrnMap(trn);
        if (collapseBoxes) {
            final Map<Treenotation, Treenotation> substs = collapseBoxes(map);
            final Treenotation subst = substs.get(trn);
            if (subst != null)
                trn = subst;
        }

        appendStringRepresentation(trn, buf, map);
        return buf.toString();
    }

    private void appendStringRepresentation(Treenotation trn, StringBuffer buf, Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> map) throws TreetonModelException {
        trnrelWriter.appendTrn(trn, buf);

        final SortedSet<ObjectPair<Treenotation, TrnRelationType>> children = map.get(trn);
        if (children != null) {
            Collection<ObjectPair<Treenotation, TrnRelationType>> internals = null, externals = null;

            for (ObjectPair<Treenotation, TrnRelationType> child : children) {
                final TrnRelationType rel = child.getSecond();
                if (rel == null || rel.isRoot()) {
                    if (internals == null)
                        internals = new ArrayList<ObjectPair<Treenotation, TrnRelationType>>();
                    internals.add(child);
                } else {
                    if (externals == null)
                        externals = new ArrayList<ObjectPair<Treenotation, TrnRelationType>>();
                    externals.add(child);
                }
            }
            if (internals != null) {
                buf.append('[');
                for (ObjectPair<Treenotation, TrnRelationType> internal : internals) {
                    trnrelWriter.appendRel(internal.getSecond(), buf);
                    appendStringRepresentation(internal.getFirst(), buf, map);
                }
                buf.append(']');
            }
            if (externals != null) {
                buf.append('(');
                for (ObjectPair<Treenotation, TrnRelationType> external : externals) {
                    trnrelWriter.appendRel(external.getSecond(), buf);
                    appendStringRepresentation(external.getFirst(), buf, map);
                }
                buf.append(')');
            }
        }
    }

    public interface TrnRelWriter {
        void appendTrn(Treenotation trn, StringBuffer buf) throws TreetonModelException;

        void appendRel(TrnRelationType rel, StringBuffer buf) throws TreetonModelException;
    }

    public static class DefaultTrnRelWriter implements TrnRelWriter {
        public void appendTrn(Treenotation trn, StringBuffer buf) throws TreetonModelException {
            TString form = (TString) trn.get("WORDFORM");
            if (form != null) {
//            buf.append(trn.getUri()).append(":").append(form);
                buf.append(form);
            } else {
//            buf.append(trn.getUri()).append(":").append(trn.getType().getName());
            }
        }

        public void appendRel(TrnRelationType rel, StringBuffer buf) throws TreetonModelException {
            //buf.append("--").append(rel==null ? "null" : rel.getName()).append("->");
            if (rel != null && !rel.isRoot())
                buf.append("--").append(rel.getName()).append("->");
        }
    }


}
