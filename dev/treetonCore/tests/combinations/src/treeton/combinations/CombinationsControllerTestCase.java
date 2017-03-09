/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.combinations;

import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.util.IdProvider;
import treeton.core.util.combinator.*;

import java.util.Comparator;

public class CombinationsControllerTestCase extends TestCase {
    private Comparator<DefaultCombinableObject> comparator = new Comparator<DefaultCombinableObject>() {
        public int compare(DefaultCombinableObject o1, DefaultCombinableObject o2) {
            double d = o1.c - o2.c;
            return d < 0 ? -1 : d > 0 ? 1 : 0;
        }
    };

    public static void assertEquals(Combination<? extends PriorityProvider> c, double... values) {
        for (int i = 0; i < c.getSize(); i++) {
            Entry e = c.getValue(i);
            assertEquals(e.getPriority(), values[i]);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

    }

    public void test() {
        CombinationsController cc = new CombinationsControllerBitArrayImpl(new int[]{8, 8});
        assertFalse(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 5;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 5;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertFalse(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 9;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        int[] sizes = ((CombinationsControllerBitArrayImpl) cc).getSizes();
        assertEquals(sizes[0], 16);
        assertEquals(sizes[1], 8);
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 9;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 5;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertFalse(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 9;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 15;
                            }
                        }
                )
        );
        sizes = ((CombinationsControllerBitArrayImpl) cc).getSizes();
        assertEquals(16, sizes[0]);
        assertEquals(24, sizes[1]);
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 9;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 15;
                            }
                        }
                )
        );
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 5;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertFalse(
                cc.combinationIsMarked(
                        false,
                        new IdProvider() {
                            public int getId() {
                                return 6;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertTrue(
                cc.combinationIsMarked(
                        true,
                        new IdProvider() {
                            public int getId() {
                                return 5;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
        assertFalse(
                cc.combinationIsMarked(
                        false,
                        new IdProvider() {
                            public int getId() {
                                return 6;
                            }
                        },
                        new IdProvider() {
                            public int getId() {
                                return 7;
                            }
                        }
                )
        );
    }

    public void testSortedEntries() {
        SortedEntries<DefaultCombinableObject> entries = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(new ExtendedComparator<DefaultCombinableObject>() {
            public double getPriority(DefaultCombinableObject object) {
                return object.getPriority();
            }

            public int compare(DefaultCombinableObject o1, DefaultCombinableObject o2) {
                return o1.compareTo(o2);
            }
        });

        entries.add(new DefaultCombinableObject(1.5, 0));
        assertEquals(entries.getFirst().getPriority(), 1.5);
        entries.add(new DefaultCombinableObject(7.5, 0));
        assertEquals(entries.getFirst().getPriority(), 1.5);
        entries.add(new DefaultCombinableObject(1.5, 0));
        assertEquals(entries.getFirst().getPriority(), 1.5);
        entries.add(new DefaultCombinableObject(3.5, 0));
        assertEquals(entries.getFirst().getPriority(), 1.5);
        entries.add(new DefaultCombinableObject(1.0, 0));
        assertEquals(entries.getFirst().getPriority(), 1.0);
        entries.add(new DefaultCombinableObject(0.0, 0));
        assertEquals(entries.getFirst().getPriority(), 0.0);
        entries.add(new DefaultCombinableObject(0.1, 0));
        assertEquals(entries.getFirst().getPriority(), 0.0);

        Entry<DefaultCombinableObject> e = entries.getFirst();

        double[] expected = new double[]{0.0, 0.1, 1.0, 1.5, 1.5, 3.5, 7.5};
        int i = 0;
        while (e != null) {
            assertEquals(e.getPriority(), expected[i++]);
            e = e.getSuccessor();
        }
        assertEquals(i, expected.length);

        System.out.println(entries);
    }

    public void testSameSortedEntries() {
        SortedEntries<DefaultCombinableObject> entries = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        entries.add(new DefaultCombinableObject(1.5, 0));
        entries.add(new DefaultCombinableObject(1.5, 0));
        entries.add(new DefaultCombinableObject(1.5, 0));
        entries.add(new DefaultCombinableObject(1.5, 0));
        entries.add(new DefaultCombinableObject(1.5, 0));
        entries.add(new DefaultCombinableObject(1.5, 0));

        Entry<DefaultCombinableObject> first = entries.getFirst();
        assertEquals(0, first.getId());

        Entry<DefaultCombinableObject> second = first.getSuccessor();
        assertTrue(first.compareTo(second) < 0);


        System.out.println(entries);
    }

    public void testCombinator() {
        ExtendedComparator<DefaultCombinableObject> c = new ExtendedComparator<DefaultCombinableObject>() {
            public double getPriority(DefaultCombinableObject object) {
                return object.getPriority();
            }

            public int compare(DefaultCombinableObject o1, DefaultCombinableObject o2) {
                return o1.compareTo(o2);
            }
        };
        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(c);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(c);

        Combinator<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2);
        combinator.start();

        assertNull(combinator.getCurrentCombination());
        entries1.add(new DefaultCombinableObject(2.0, 0));
        assertNull(combinator.getCurrentCombination());
        entries2.add(new DefaultCombinableObject(3.0, 0));
        assertEquals(combinator.getCurrentCombination(), 2.0, 3.0);
        combinator.combinationUsed(combinator.getCurrentCombination());
        assertNull(combinator.getCurrentCombination());
        entries1.add(new DefaultCombinableObject(1.0, 0));
        entries2.add(new DefaultCombinableObject(1.0, 0));
        assertEquals(combinator.getCurrentCombination(), 1.0, 1.0);
        combinator.next();
        assertEquals(combinator.getCurrentCombination(), 2.0, 1.0);
        combinator.next();
        assertEquals(combinator.getCurrentCombination(), 1.0, 3.0);
        assertFalse(combinator.next());
    }

    public void testCombinatorDim3() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);

        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries3 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        Combinator<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2, entries3);

        int size1 = 50;
        int size2 = 50;
        int size3 = 50;
        combinator.start();

        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(Math.random(), 0));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(Math.random(), 0));
        }

        for (int i = 0; i < size3; i++) {
            entries3.add(new DefaultCombinableObject(Math.random(), 0));
        }


        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);

        /*entries1.add(new DefaultCombinableObject(0));
        entries2.add(new DefaultCombinableObject(0));
        entries3.add(new DefaultCombinableObject(0));

        prev = combinator.getCurrentCombination();
        assertEquals(prev,0,0,0);
        System.out.println(j+": "+ prev);*/

        boolean afterAdd = false;

        while (combinator.next()) {
            Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
            if (!afterAdd && ((CombinationImpl) prev).getNorm() > ((CombinationImpl) c).getNorm()) {
                fail("Found mistake in order");
            }
            if (!afterAdd && prev.equals(c)) {
                fail("Found double");
            }

            prev = c;

            if (++j % 100 == 0) {
                System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                if (entries1.size() < size1 * 2) {
                    entries1.add(new DefaultCombinableObject(Math.random() / 2, 0));
                    afterAdd = true;
                }
            }
            if (j % 150 == 0 && entries2.size() < size2 * 2) {
                entries2.add(new DefaultCombinableObject(Math.random() / 2, 0));
                afterAdd = true;
            }

            if (j % 170 == 0 && entries3.size() < size3 * 2) {
                entries3.add(new DefaultCombinableObject(Math.random() / 2, 0));
                afterAdd = true;
            }

        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size() * entries3.size(), j);
    }

    public void testCombinatorDim3Sames() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);

        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries3 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        Combinator<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2, entries3);

        int size1 = 20;
        int size2 = 20;
        int size3 = 20;
        combinator.start();

        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(5.0, 0));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(5.0, 0));
        }

        for (int i = 0; i < size3; i++) {
            entries3.add(new DefaultCombinableObject(5.0, 0));
        }


        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);

        /*entries1.add(new DefaultCombinableObject(0));
        entries2.add(new DefaultCombinableObject(0));
        entries3.add(new DefaultCombinableObject(0));

        prev = combinator.getCurrentCombination();
        assertEquals(prev,0,0,0);
        System.out.println(j+": "+ prev);*/

        boolean afterAdd = false;

        while (combinator.next()) {
            Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
            if (!afterAdd && ((CombinationImpl) prev).getNorm() > ((CombinationImpl) c).getNorm()) {
                fail("Found mistake in order");
            }
            if (!afterAdd && prev.equals(c)) {
                fail("Found double");
            }

            prev = c;

            if (++j % 100 == 0) {
                System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                if (entries1.size() < size1 * 2) {
                    entries1.add(new DefaultCombinableObject(5.0, 0));
                    afterAdd = true;
                }
            }
            if (j % 150 == 0 && entries2.size() < size2 * 2) {
                entries2.add(new DefaultCombinableObject(5.0, 0));
                afterAdd = true;
            }

            if (j % 170 == 0 && entries3.size() < size3 * 2) {
                entries3.add(new DefaultCombinableObject(5.0, 0));
                afterAdd = true;
            }

        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size() * entries3.size(), j);
    }

    public void testCombinatorDim3SamesFillBefore() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);

        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries3 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        Combinator<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2, entries3);

        int size1 = 20;
        int size2 = 20;
        int size3 = 20;

        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(5.0, 0));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(5.0, 0));
        }

        for (int i = 0; i < size3; i++) {
            entries3.add(new DefaultCombinableObject(5.0, 0));
        }

        combinator.start();

        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);

        /*entries1.add(new DefaultCombinableObject(0));
        entries2.add(new DefaultCombinableObject(0));
        entries3.add(new DefaultCombinableObject(0));

        prev = combinator.getCurrentCombination();
        assertEquals(prev,0,0,0);
        System.out.println(j+": "+ prev);*/

        boolean afterAdd = false;

        while (combinator.next()) {
            Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
            if (!afterAdd && ((CombinationImpl) prev).getNorm() > ((CombinationImpl) c).getNorm()) {
                fail("Found mistake in order");
            }
            if (!afterAdd && prev.equals(c)) {
                fail("Found double");
            }

            prev = c;

            if (++j % 100 == 0) {
                System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                if (entries1.size() < size1 * 2) {
                    entries1.add(new DefaultCombinableObject(5.0, 0));
                    afterAdd = true;
                }
            }
            if (j % 150 == 0 && entries2.size() < size2 * 2) {
                entries2.add(new DefaultCombinableObject(5.0, 0));
                afterAdd = true;
            }

            if (j % 170 == 0 && entries3.size() < size3 * 2) {
                entries3.add(new DefaultCombinableObject(5.0, 0));
                afterAdd = true;
            }

        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size() * entries3.size(), j);
    }

    public void testCombinatorDim2() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);

        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        CombinatorWithFront<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2);

        CombinatorVisualizerFrame frame = new CombinatorVisualizerFrame(combinator, 40);
        frame.setVisible(true);

        int size1 = 100;
        int size2 = 100;


        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(Math.random(), 0));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(Math.random(), 0));
        }

        combinator.start();

        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);

        boolean afterAdd = false;
        while (combinator.next()) {
            Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
            if (!afterAdd && prev.getNorm() > c.getNorm()) {
                fail("Found mistake in order");
            }
            if (!afterAdd && prev.equals(c)) {
                fail("Found double");
            }

            afterAdd = false;

            prev = c;
            if (++j % 100 == 0) {
                System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                if (entries1.size() < size1 * 2) {
                    entries1.add(new DefaultCombinableObject(Math.random() / 2, 0));
                    afterAdd = true;
                }
            }
            if (j % 150 == 0 && entries2.size() < size2 * 2) {
                entries2.add(new DefaultCombinableObject(Math.random() / 2, 0));
                afterAdd = true;
            }
            frame.validate();
//            try {
            for (int i = 0; i < 150000; i++) ;
//            } catch (InterruptedException e) {
//            }
        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size(), j);
        //System.out.println("finish");
    }

    public void testCombinator2D() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);
        Comparator<DefaultCombinableObject> comparator = new ExtendedComparator<DefaultCombinableObject>() {
            public double getPriority(DefaultCombinableObject object) {
                return object.getPriority();
            }

            public int compare(DefaultCombinableObject o1, DefaultCombinableObject o2) {
                return o1.compareTo(o2);
            }
        };


        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

        int size1 = 100;
        int size2 = 100;

        Combinator2D<DefaultCombinableObject> combinator = new Combinator2D<DefaultCombinableObject>(entries1, entries2, new Comparator<Combination<DefaultCombinableObject>>() {
            public int compare(Combination<DefaultCombinableObject> o1, Combination<DefaultCombinableObject> o2) {
                double d1 = dest(o1);
                double d2 = dest(o2);
                double d = d1 - d2;
                return d < 0 ? -1 : d > 0 ? 1 : 0;
            }
        });

        CombinatorVisualizerFrame frame = new CombinatorVisualizerFrame(combinator, 40);
        frame.setVisible(true);

        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(Math.random(), Math.random()));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(Math.random(), Math.random()));
        }

        System.out.println(entries1);
        System.out.println(entries2);


        combinator.start();

        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);
        combinator.combinationUsed(prev);

        boolean afterAdd = false;
        Combination<DefaultCombinableObject> c;
        while ((c = combinator.getCurrentCombination()) != null) {
            if (!afterAdd && dest(prev) > dest(c)) {
                fail("Found mistake in order");
            }
            if (!afterAdd && prev.equals(c)) {
                fail("Found double");
            }

            afterAdd = false;

            prev = c;
            if (++j % 100 == 0) {
                System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                if (entries1.size() < size1 * 2) {
                    DefaultCombinableObject с = new DefaultCombinableObject(Math.random() / 2, Math.random() / 2);
                    System.out.println("Adding to 1 " + entries1.add(с));
                    afterAdd = true;
                }
            }
            if (j % 150 == 0 && entries2.size() < size2 * 2) {
                DefaultCombinableObject с = new DefaultCombinableObject(Math.random() / 2, Math.random() / 2);
                System.out.println("Adding to 2 " + entries2.add(с));
                afterAdd = true;
            }
            frame.validate();
//            try {
            for (int i = 0; i < 15000; i++) ;
//            } catch (InterruptedException e) {
//            }
            combinator.combinationUsed(c);
        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size(), j);
    }

    private double dest(Combination<DefaultCombinableObject> comb) {
        return Math.abs(comb.getValue(0).getObject().getPriority() - comb.getValue(1).getObject().getPriority());
    }

    public void testCombinatorDim2Doubles() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);
        for (int n = 0; n < 20; n++) {
            SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);
            SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(comparator);

            Combinator<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2);

            int size1 = 100;
            int size2 = 100;

            for (int i = 0; i < size1; i++) {
                entries1.add(new DefaultCombinableObject(Math.random(), 0));
            }

            for (int i = 0; i < size2; i++) {
                entries2.add(new DefaultCombinableObject(Math.random(), 0));
            }

            combinator.start();

            int j = 0;
            Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
            System.out.println(j++ + ": " + prev);

            boolean afterAdd = false;
            while (combinator.next()) {
                Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
                if (!afterAdd && ((CombinationImpl) prev).getNorm() > ((CombinationImpl) c).getNorm()) {
                    fail("Found mistake in order");
                }
                if (!afterAdd && prev.equals(c)) {
                    fail("Found double. afterAdd is " + afterAdd);
                }

                afterAdd = false;

                prev = c;
                if (++j % 100 == 0) {
                    System.out.println(j + ": " + c + ", " + combinator.getStatistics());

                    if (entries1.size() < size1 * 2) {
                        entries1.add(new DefaultCombinableObject(Math.random() / 2, 0));
                        afterAdd = true;
                    }
                }
                if (j % 150 == 0 && entries2.size() < size2 * 2) {
                    entries2.add(new DefaultCombinableObject(Math.random() / 2, 0));
                    afterAdd = true;
                }
            }
            assertEquals(entries1.size() * entries2.size(), j);
        }
    }

    public void testCombinatorDim2SamePriorities() {
        //Logger.getLogger(Combinator.class).setLevel(Level.TRACE);

        ExtendedComparator<DefaultCombinableObject> cmp = new ExtendedComparator<DefaultCombinableObject>() {
            public double getPriority(DefaultCombinableObject object) {
                return object.getPriority();
            }

            public int compare(DefaultCombinableObject o1, DefaultCombinableObject o2) {
                return o1.compareTo(o2);
            }
        };

        SortedEntries<DefaultCombinableObject> entries1 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(cmp);
        SortedEntries<DefaultCombinableObject> entries2 = new SortedEntriesRBTreeImpl<DefaultCombinableObject>(cmp);

        CombinatorWithFront<DefaultCombinableObject> combinator = new CombinatorWithFront<DefaultCombinableObject>(entries1, entries2);

        CombinatorVisualizerFrame frame = new CombinatorVisualizerFrame(combinator, 40);
        frame.setVisible(true);

        int size1 = 50;
        int size2 = 50;


        for (int i = 0; i < size1; i++) {
            entries1.add(new DefaultCombinableObject(i % 2 == 0 ? 0.5 : 0.2, Math.random()));
        }

        for (int i = 0; i < size2; i++) {
            entries2.add(new DefaultCombinableObject(i % 2 == 0 ? 0.7 : 0.1, Math.random()));
        }


        combinator.start();

        int j = 0;
        Combination<DefaultCombinableObject> prev = combinator.getCurrentCombination();
        System.out.println(j++ + ": " + prev);

        while (combinator.next()) {
            Combination<DefaultCombinableObject> c = combinator.getCurrentCombination();
            if (((CombinationImpl) prev).getNorm() > ((CombinationImpl) c).getNorm()) {
                fail("Found mistake in order");
            }
            if (prev.equals(c)) {
                fail("Found double");
            }

            prev = c;
            System.out.println(j++ + ": " + c + ", " + combinator.getStatistics());
            frame.validate();
            for (int i = 0; i < 30000; i++) ;
        }
        //assertEquals((size1+1)*(size2+1)*(size3+1),j);
        assertEquals(entries1.size() * entries2.size(), j);
    }

}
