/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.trnconvert;

import treeton.core.RelationsIterator;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.TrnIterator;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;
import treeton.core.util.ObjectPair;

import java.util.*;

/**
 * Различные представления тринотации. Созданы на основе методов {@link TreenotationStorage}. Нужны для удобного
 * обхода синтаксических структур.
 */
public class TreenotationConverter {
    private static final String systemType = "System";
    private static final String grammType = "Gramm";


    private static final Comparator<? super ObjectPair<Treenotation, TrnRelationType>> comparator = new Comparator<ObjectPair<Treenotation, TrnRelationType>>() {
        public int compare(ObjectPair<Treenotation, TrnRelationType> o1, ObjectPair<Treenotation, TrnRelationType> o2) {
            int st = o1.getFirst().getStartNumerator() - o2.getFirst().getStartNumerator();
            return st != 0 ? st : o1.getFirst().getUri().compareTo(o2.getFirst().getUri());
        }
    };
    private static final Comparator<? super Treenotation> trnPosLenComparator = new Comparator<Treenotation>() {
        public int compare(Treenotation o1, Treenotation o2) {
            int res = o1.getStartNumerator() - o2.getStartNumerator();
            if (res != 0)
                return res; // сначала тринотация, кот. начинается левее
            res = o1.getEndNumerator() - o2.getEndNumerator();
            if (res != 0)
                return res; // если начала совпадают, сначала тринотация, кот. заканчивается правее (больше по размеру)
            return o1.getUri().compareTo(o2.getUri()); // если привязаны совпадают начала и концы, сравниваем по uri
        }
    };

    /**
     * Строит мап, отражающий внутреннюю структуру тринотации. Для каждой нелистовой тринотации parent (из которой исходят связи)
     * в мапе хранится отображение parent -> множество пар {child, связь от parent к child}; листовые вершины в мап
     * в качестве ключей не входят.
     *
     * @param trn тринотация, для которой будет строится мап; должно выполняться условие: trn.getStorage()!=null
     * @return мап с родительской тринотации на множество дочерних; дочерние упорядочены по положению их левой границы
     * в тексте, а если левые границы совпадают (это бывает только в случае, когда тринотации находятся в одной
     * "коробочке", т.е. покрывают один и тот же отрезок текста) - по uri;
     * <p>для каждой дочерней тринотации указана тип ее связи с родительской:
     * <ul>
     * <li> null-связь: к некорневой внутренней тринотации; используется в следующих случаях (TODO: перечислены все случаи?)
     * <ul>
     * <li>1) от "коробочки" к содержащимся в ней тринотациям
     * <li>2) от System к содержащейся в ней тринотации
     * <li>3) в случае, когда в правилах указано С[A,B] от новой тринотации C к A будет проведена root-связь, а к B - null-связь
     * </ul>
     * <li> root=связь: к корневой внутренней тринотации (TrnRelationType.isRoot()==true)
     * <li> обычная (синтаксическая) связь (TrnRelationType.isRoot()==false)
     * </ul>
     * @throws Exception
     */
    public static Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> getTrnMap(Treenotation trn) throws Exception {
        final TreenotationStorage storage = trn.getStorage();
        if (storage == null)
            throw new Exception("Treenotation doesn't belong to any TreenotationStorage.");

        Map<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>> map = new HashMap<Treenotation, SortedSet<ObjectPair<Treenotation, TrnRelationType>>>();

        Stack<Treenotation> todo = new Stack<Treenotation>();
        todo.push(trn);

        while (!todo.isEmpty()) {
            Treenotation curr = todo.pop();
            final RelationsIterator relit = storage.internalRelationsIterator(curr, curr.getStartToken(), curr.getEndToken());
            while (relit.next()) {
                final Treenotation host = relit.getHost();
                SortedSet<ObjectPair<Treenotation, TrnRelationType>> set = map.get(host);
                if (set == null)
                    map.put(host, set = new TreeSet<ObjectPair<Treenotation, TrnRelationType>>(comparator));

                final Treenotation slave = relit.getSlave();
                set.add(new ObjectPair<Treenotation, TrnRelationType>(slave, relit.getType()));
                if (!curr.equals(slave))
                    todo.push(slave);
            }
        }

        return map;
    }

    public static NavigableMap<Treenotation, ObjectPair<Treenotation, TrnRelationType>> getTrnParentMap(Treenotation trn, boolean expandBoxes) throws Exception {
        final TreenotationStorage storage = trn.getStorage();
        if (storage == null)
            throw new Exception("Treenotation doesn't belong to any TreenotationStorage.");

        Stack<Treenotation> todo = new Stack<Treenotation>();
        todo.push(trn);

        TreeMap<Treenotation, ObjectPair<Treenotation, TrnRelationType>> map = new TreeMap<Treenotation, ObjectPair<Treenotation, TrnRelationType>>(
                trnPosLenComparator
        );

        while (!todo.isEmpty()) {
            Treenotation curr = todo.pop();
            final RelationsIterator relit = storage.internalRelationsIterator(curr, curr.getStartToken(), curr.getEndToken());
            while (relit.next()) {
                final Treenotation host = relit.getHost();
                final Treenotation slave = relit.getSlave();
                assert !(map.containsKey(slave)); // у каждой тринотации только один хозяин, значит, одна входящая связь

                TrnRelationType rel = relit.getType();

                map.put(slave, new ObjectPair<Treenotation, TrnRelationType>(host, rel));

                if (!curr.equals(slave) && (expandBoxes || !isBox(slave)))
                    todo.push(slave);
            }
        }

        return map;
    }

    /**
     * Коробочкой считается тринотация типа System, у которой вложенные тринотации имеют тип Gramm.
     *
     * @param trn
     * @return является ли тринотация коробочкой
     * @throws treeton.core.model.TreetonModelException
     */
    public static boolean isBox(Treenotation trn) throws TreetonModelException {
        TreenotationStorage storage = trn.getStorage();
        TrnType system = storage.getTypes().get(systemType);
        if (system == null)
            throw new TreetonModelException("Couldn't find type " + systemType);

        if (!system.equals(trn.getType()))
            return false;

        TrnType gramm = storage.getTypes().get(grammType);
        if (gramm == null)
            throw new TreetonModelException("Couldn't find type " + grammType);

        TrnIterator it = storage.internalTrnsIterator(trn.getStartToken(), trn.getEndToken(), trn);
        while (it.hasNext()) {
            Treenotation t = (Treenotation) it.next();
            if (!gramm.equals(t.getType()))
                return false;
        }

        return true;
    }
}
