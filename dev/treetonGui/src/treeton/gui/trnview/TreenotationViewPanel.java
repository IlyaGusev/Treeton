/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import com.sun.glf.goodies.WaveStroke;
import treeton.core.*;
import treeton.core.config.GuiConfiguration;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnRelationTypeStorage;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;
import treeton.core.util.NumeratedObject;
import treeton.gui.GuiResources;
import treeton.gui.labelgen.TrnLabelGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

//TODO: сделать корректное рисование связей от объектов к другим объектам,
//TODO: если первые вложены во вторые (по горизонтали).

public class TreenotationViewPanel extends TreenotationViewPanelAbstract implements ActionListener, AdjustmentListener {
    private static final Color gridNumbersColor = new Color(0, 0, 190);
    private static final Color gridNumbersBackGroundColor = new Color(185, 185, 185);
    private static final Color textBackGroundColor = new Color(195, 195, 195);
    private static final Color mainBackGroundColor = new Color(210, 210, 210);
    private static final Color emptyAreaColor = new Color(170, 170, 170);
    private static final Color cursorColor = new Color(165, 165, 165);
    private static final Color selectionColor = new Color(180, 180, 180);
    private static final Color cursorWithSelectionColor = new Color(155, 155, 155);
    private static final Color textCursorColor = new Color(150, 150, 150);
    private static final Color textSelectionColor = new Color(165, 165, 165);
    private static final Color cursorWithTextSelectionColor = new Color(140, 140, 140);
    private static final Color trnSelectionColorTransparent = new Color(255, 0, 0, 127);
    private static final Color trnSelectionColorTransparentWeak = new Color(255, 0, 0, 31);
    private static final Color relationLabelBackGroundColor = new Color(200, 200, 200);
    private static final Treenotation TRN_RELATION = TreetonFactory.newTreenotation(null, null, (TrnType) null);
    private static BlockStack freeIterators = new BlockStack();
    private TreenotationStorage treenotations;
    private char[] document;
    private TrnType[] tokenTypes;
    private TrnType[] commonTypes;
    private HashMap<String, TrnLabelGenerator> labelGenerators;
    private JScrollBar horScroll;
    private JScrollBar verScroll;
    private ZoomButton horZoomButton;
    private GridButton gridButton;
    private DrawingPane dp;
    //ниже используется double - потому что символы могут отображаться не целиком (заходить за края).
    //Например, шаг скроллирования - пол символа
    private double from, to; //область текста, которую мы визуализируем. В символах. Всегда кратны horStep
    private double cur; //точка, с которой начинается отрисовка. В символьных координатах. Всегда кратен horStep
    private double focus;
    private int curPos;
    private int textSelectionIntervalStart = -1;
    private int textSelectionIntervalEnd = -1;
    private boolean resetTextSelectionInterval = false;
    private boolean showCursor;
    private int maxOffset;
    private int maxOffsetLen;
    private Fraction fractionHorStep;
    private double horStep; //шаг скроллера (размерность - количество символов)
    private double verStep;
    private double currentHeight;
    private double fake;
    private Fraction fakeFraction;
    private char[] spaces;
    private ArrayList<TrnManipulationListener> trnListeners;
    private HashMap<Object, Treenotation> selected;
    private ArrayList<Object> selectedArray;
    private boolean gridIsOn;
    private HashSet<Treenotation> hidden;
    private java.util.List<Treenotation> alwaysShown = new ArrayList<Treenotation>();

    public TreenotationViewPanel(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        super();
        treenotations = trns;
        dp = null;
        verScroll = null;
        horScroll = null;
        horZoomButton = null;
        gridButton = null;
        selected = new HashMap<Object, Treenotation>();
        selectedArray = new ArrayList<Object>();
        gridIsOn = false;
        hidden = new HashSet<Treenotation>();
        trnListeners = new ArrayList<TrnManipulationListener>();
        document = (doc.toString().replace('\r', ' ').replace('\n', ' ')).toCharArray();
        setCursorPosition(_curPos);
        horStep = 0.5;
        fractionHorStep = new Fraction(1, 2);
        verStep = 0.5;

        int cn = 0, tn = 0;
        if (tp != null) {
            for (int i = 0; i < tp.length; i++) {
                try {
                    if (tp[i].isTokenType()) {
                        tn++;
                    } else {
                        cn++;
                    }
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
        }
        this.labelGenerators = labelGenerators == null ? new HashMap() : labelGenerators;
        commonTypes = cn == 0 ? null : new TrnType[cn];
        tokenTypes = tn == 0 ? null : new TrnType[tn];
        cn = 0;
        tn = 0;
        if (tp != null) {
            for (int i = 0; i < tp.length; i++) {
                TrnType cur = tp[i];
                try {
                    if (cur.isTokenType()) {
                        tokenTypes[tn++] = cur;
                    } else {
                        commonTypes[cn++] = cur;
                    }
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
        }

        from = _from == null ? 0 : _from.toDouble();
        to = _to == null ? document.length : _to.toDouble();
        fake = from / horStep;
        from = Math.floor(fake) * horStep;
        fake = to / horStep;
        to = Math.ceil(fake) * horStep;

        focus = _focus == null ? (double) (curPos * 2 + 1) / 2 : _focus.toDouble();

        if (focus < from)
            focus = from;
        if (focus > to)
            focus = to;

        cur = 0;
        currentHeight = 0;
        maxOffset = (int) to;
        maxOffsetLen = Integer.toString(maxOffset).length();
        horScroll = null;
        spaces = new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
        init();

        fakeFraction = new Fraction(0, 1);
    }

    private static LocalTrnIterator getTrnIterator() {
        if (freeIterators.isEmpty()) {
            return new LocalTrnIterator();
        } else {
            return (LocalTrnIterator) freeIterators.pop();
        }
    }

    private static void freeTrnIterator(LocalTrnIterator it) {
        freeIterators.push(it);
    }

    public double getFocus() {
        return focus;
    }

    public void selectTrn(Treenotation trn) {
        updateSelection(false, trn, null);
        notifyListeners(0);
    }

    private void updateSelection(boolean shiftKey, Treenotation trn, Treenotation parent) {
        if (shiftKey) {
            if (trn == null)
                return;
            if (selected.containsKey(trn)) {
                selected.remove(trn);
                selectedArray.remove(trn);
            } else {
                selected.put(trn, parent);
                selectedArray.add(trn);
            }
        } else {
            if (trn == null) {
                selected.clear();
                selectedArray.clear();
            } else {
                selected.clear();
                selectedArray.clear();
                selected.put(trn, parent);
                selectedArray.add(trn);
            }
        }
    }

    public void reset(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        treenotations = trns;
        remove(dp);
        remove(horScroll);
        remove(verScroll);
        remove(horZoomButton);
        remove(gridButton);

        alwaysShown.clear();
        dp = null;
        verScroll = null;
        horScroll = null;
        horZoomButton = null;
        gridButton = null;
        selected = new HashMap<Object, Treenotation>();
        selectedArray = new ArrayList<Object>();
        gridIsOn = false;
        hidden = new HashSet<Treenotation>();
        document = (doc.toString().replace('\r', ' ').replace('\n', ' ')).toCharArray();
        setCursorPosition(_curPos);
        horStep = 0.5;
        fractionHorStep = new Fraction(1, 2);
        verStep = 0.5;

        int cn = 0, tn = 0;
        if (tp != null) {
            for (int i = 0; i < tp.length; i++) {
                if (tp[i] == null)
                    continue;

                try {
                    if (tp[i].isTokenType()) {
                        tn++;
                    } else {
                        cn++;
                    }
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
        }
        this.labelGenerators = labelGenerators == null ? new HashMap() : labelGenerators;
        commonTypes = cn == 0 ? null : new TrnType[cn];
        tokenTypes = tn == 0 ? null : new TrnType[tn];
        cn = 0;
        tn = 0;
        if (tp != null) {
            for (int i = 0; i < tp.length; i++) {
                TrnType cur = tp[i];
                if (cur == null)
                    continue;
                try {
                    if (cur.isTokenType()) {
                        tokenTypes[tn++] = cur;
                    } else {
                        commonTypes[cn++] = cur;
                    }
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
        }

        from = _from == null ? 0 : _from.toDouble();
        to = _to == null ? document.length : _to.toDouble();
        fake = from / horStep;
        from = Math.floor(fake) * horStep;
        fake = to / horStep;
        to = Math.ceil(fake) * horStep;

        focus = _focus == null ? (double) (curPos * 2 + 1) / 2 : _focus.toDouble();

        if (focus < from)
            focus = from;
        if (focus > to)
            focus = to;

        cur = 0;
        currentHeight = 0;
        maxOffset = (int) to;
        maxOffsetLen = Integer.toString(maxOffset).length();
        horScroll = null;
        spaces = new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};

        init();

        fakeFraction = new Fraction(0, 1);
    }

    public void paint(Graphics g) {
        super.paint(g);
        Dimension dim = this.getSize();
        Dimension hsDim = horScroll.getPreferredSize();
        Dimension vsDim = verScroll.getPreferredSize();
        g.setColor(Color.DARK_GRAY);
        g.drawRect(dim.width - vsDim.width, dim.height - 1 - hsDim.height - dp.additionAreaHeight, vsDim.width - 1, dp.additionAreaHeight - 1);
        if (gridIsOn) { //белая вертикальная линия сетки
            g.setColor(Color.WHITE);
            g.drawLine(dim.width - 1, dim.height - 1 - hsDim.height - dp.additionAreaHeight, dim.width - 1, dim.height - 1 - hsDim.height);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (dp != null) {
            dp.setEnabled(enabled);
        }
    }

    protected void init() {
        dp = new TreenotationViewPanel.DrawingPane();
        horScroll = new JScrollBar(JScrollBar.HORIZONTAL);
        verScroll = new JScrollBar(JScrollBar.VERTICAL);
        horZoomButton = new ZoomButton();
        gridButton = new GridButton();

        setLayout(null);
        add(dp);
        dp.setBackground(mainBackGroundColor);
        dp.setToolTipText("");
        setOpaque(true);
        add(horScroll);
        add(verScroll);
        add(horZoomButton);
        add(gridButton);
        horScroll.addAdjustmentListener(this);
        verScroll.addAdjustmentListener(this);
        horScroll.setOpaque(true);
        horZoomButton.setFocusable(false);
        horZoomButton.addMouseListener(horZoomButton);
        gridButton.addMouseListener(gridButton);

        addComponentListener(this);
        dp.addMouseListener(dp);
        dp.addMouseMotionListener(dp);

        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        JScrollBar source = (JScrollBar) e.getSource();
        if (source.getOrientation() == JScrollBar.HORIZONTAL) {
            Dimension dim = dp.getSize();
            fake = (double) dim.width / (double) (dp.cellWidth * 2);
            cur = from + e.getValue() * horStep;
            focus = cur + fake;
            fake = focus + fake;
            if (fake > to) {
                fake = to - (double) dim.width / (double) (dp.cellWidth);
                if (fake < from) {
                    cur = from;
                    focus = (to - from) / 2;
                } else {
                    fake /= horStep;
                    cur = Math.ceil(fake) * horStep;
                    focus = cur + (double) dim.width / (double) (dp.cellWidth * 2);
                }
            } else {
                fake = focus - (double) dim.width / (double) (dp.cellWidth * 2);
                fake /= horStep;
                cur = Math.ceil(fake) * horStep;
                focus = cur + (double) dim.width / (double) (dp.cellWidth * 2);
            }

            try {
                dp.fillGrid();
            } catch (TreetonModelException e1) {
                e1.printStackTrace();
            }
            recountVerScroll();
        } else {
            currentHeight = (source.getMaximum() - e.getValue() - 1) * verStep;
        }
        dp.repaint();
    }

    public void componentHidden(ComponentEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        Dimension dim = getSize();
        Dimension hsDim = horScroll.getPreferredSize();
        Dimension vsDim = verScroll.getPreferredSize();
        if (dp.initialized) {
            horScroll.setBounds(0, dim.height - hsDim.height, dim.width - vsDim.width, hsDim.height);
            verScroll.setBounds(dim.width - vsDim.width, 0, vsDim.width, dim.height - hsDim.height - dp.additionAreaHeight);
            horZoomButton.setBounds(dim.width - vsDim.width, dim.height - hsDim.height, vsDim.width, (int) Math.round(hsDim.height * 0.6));
            gridButton.setBounds(dim.width - vsDim.width, dim.height - (int) Math.round(hsDim.height * 0.4), vsDim.width, (int) Math.round(hsDim.height * 0.4));
            dp.setBounds(0, 0, dim.width - vsDim.width, dim.height - hsDim.height);

            recountHorScroll();
            try {
                dp.fillGrid();
            } catch (TreetonModelException e1) {
                e1.printStackTrace();
            }
            recountVerScroll();

            horScroll.revalidate();
            verScroll.revalidate();
            horZoomButton.revalidate();
            gridButton.revalidate();
        } else {
            dp.setBounds(0, 0, dim.width - vsDim.width, dim.height - hsDim.height);
        }
        repaint();
    }

    public void recountHorScroll() {
        Dimension dim = dp.getSize();

        fake = (double) dim.width / (double) (dp.cellWidth * 2);
        fake = focus - fake;
        if (fake < from) {
            cur = from;
            focus = from + fake;
            if (focus > to) {
                focus = (to - from) / 2;
            }
        } else {
            fake = focus + (double) dim.width / (double) (dp.cellWidth * 2);
            if (fake > to) {
                fake = to - (double) dim.width / (double) (dp.cellWidth);
                if (fake < from) {
                    cur = from;
                    focus = (to - from) / 2;
                } else {
                    fake /= horStep;
                    cur = Math.ceil(fake) * horStep;
                    focus = cur + (double) dim.width / (double) (dp.cellWidth * 2);
                }
            } else {
                fake = focus - (double) dim.width / (double) (dp.cellWidth * 2);
                fake /= horStep;
                cur = Math.ceil(fake) * horStep;
                focus = cur + (double) dim.width / (double) (dp.cellWidth * 2);
            }
        }

        fake = to - (double) dim.width / (double) (dp.cellWidth) - from;
        if (fake < 0) {
            fake = 0;
        }
        fake /= horStep;
        int max = (int) Math.ceil(fake) + 1;
        fake = (cur - from) / horStep;
        horScroll.setValues((int) Math.floor(fake), 1, 0, max);
    }

    public void recountVerScroll() {
        Dimension dim = dp.getSize();

        fake = (double) (dim.height - dp.additionAreaHeight) / (double) dp.cellHeight;
        fake = (double) dp.gridHeight - fake;
        if (fake < 0) {
            fake = 0;
        }
        int v = verScroll.getMaximum() - verScroll.getValue() - 1;
        fake /= verStep;
        int max = (int) Math.ceil(fake) + 1;
        if (v >= max) {
            v = max - 1;
        }
        verScroll.setValues(max - v - 1, 1, 0, max);
    }

    public void componentShown(ComponentEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isCursorVisible() {
        return showCursor;
    }

    public void setCursorVisible(boolean visible) {
        if (this.showCursor != visible) {
            this.showCursor = visible;
            dp.repaint();
        }
    }

    public void scrollToPosition(Fraction pos) throws TreetonModelException {
        scrollToPosition(pos == null ? (double) (curPos * 2 + 1) / 2 : pos.toDouble());
    }

    public void scrollToPosition(double position) throws TreetonModelException {
        focus = position;
        if (focus < from)
            focus = from;
        if (focus > to)
            focus = to;
        if (dp.initialized) {
            recountHorScroll();
            dp.fillGrid();
            recountVerScroll();
            dp.repaint();
        }
    }

    public void scrollToPosition(Token token) throws TreetonModelException {
        scrollToPosition(new Fraction(token.getStartNumerator(), token.getStartDenominator()));
    }

    public void fitToView() {
        if (!dp.initialized) {
            dp.cellWidth = 0;
        } else {
            int pixelWidth = dp.getWidth();
            double width = to - from;

            int c = dp.bigFontWidth + 2;

            while (width * c <= pixelWidth) {
                c += 2;
            }
            c -= 2;

            dp.cellWidth = c;
        }
        componentResized(null);
        dp.repaint();
    }

    public int getCursorPosition() {
        return curPos;
    }

    public void setCursorPosition(int _curPos) {
        if (dp != null) {
            int oldPos = curPos;
            if (_curPos < (int) Math.floor(from)) {
                curPos = (int) Math.floor(from);
            } else if (_curPos >= (int) Math.floor(to)) {
                curPos = (int) Math.floor(to) - 1;
            } else {
                curPos = _curPos;
            }

            if (oldPos == curPos)
                return;

            boolean repaint = false;

            Dimension dim = dp.getSize();
            fake = cur + (double) dim.width / (double) dp.cellWidth;
            if (oldPos >= (int) Math.floor(cur) && oldPos <= (int) Math.floor(fake) ||
                    curPos >= (int) Math.floor(cur) && curPos <= (int) Math.floor(fake)) {
                repaint = true;
            }

            if (repaint) {
                dp.repaint();
            }
        } else {
            curPos = _curPos;
        }
    }

    public void addTrnManipulationListener(TrnManipulationListener l) {
        trnListeners.add(l);
    }

    public void removeTrnManipulationListener(TrnManipulationListener l) {
        trnListeners.remove(l);
    }

    public String getDocument() {
        return new String(document);
    }

    public void hide(Treenotation trn) {
        if (selected.containsKey(trn)) {
            selected.remove(trn);
            selectedArray.remove(trn);
        }
        hidden.add(trn);
    }

    public void hideAll(TrnType type) {
        TypeIteratorInterface tit = treenotations.typeIterator(type);
        while (tit.hasNext()) {
            hide((Treenotation) tit.next());
        }
    }

    public void showAll(TrnType type) {
        TypeIteratorInterface tit = treenotations.typeIterator(type);
        while (tit.hasNext()) {
            show((Treenotation) tit.next());
        }
    }

    public void showAll() {
        hidden.clear();
    }

    public void show(Treenotation trn) {
        hidden.remove(trn);
    }

    public boolean isTrnShown(Treenotation trn) {
        if (hidden.contains(trn)) {
            return false;
        } else {
            return true;
        }
    }

    private void filterSelection() {
        for (int i = 0; i < selectedArray.size(); ) {
            Treenotation t = (Treenotation) selectedArray.get(i);
            if (t.getId() < 0) {
                selectedArray.remove(i);
                selected.remove(t);
            } else {
                i++;
            }
        }
    }

    private void notifyListeners(int clickCount) {
        filterSelection();
        Iterator<TrnManipulationListener> it = trnListeners.iterator();
        while (it.hasNext()) {
            (it.next()).trnClicked(new TrnManipulationEvent(this, selectedArray, clickCount));
        }
    }

    public void selectNone() {
        selected.clear();
        selectedArray.clear();
        notifyListeners(0);
    }

    private boolean trnIsSolid(Treenotation trn) {
        return trn.isLocked() && trn.isEmpty();
    }

    public Object getSelectedObject(MouseEvent e) {
        DrawingPane.Cell newSelection = dp.getCell(e.getPoint());

        if (newSelection != null) {
            return newSelection.trn;
        }
        return null;
    }

    public String getToolTipText(MouseEvent e) {
        Object o = getSelectedObject(e);
        String rslt = null;
        if (o != null) {
            TrnLabelGenerator lg;
            String typeName;
            try {
                typeName = ((Treenotation) o).getType().getName();
            } catch (TreetonModelException e1) {
                typeName = "Exception!!!";
            }
            if ((lg = labelGenerators.get(typeName)) != null) {
                String lbl = lg.generateLabel((Treenotation) o);
                rslt = "<html><p align=center>" + lg.generateCaption((Treenotation) o) + ((lbl != null && lbl.length() > 0) ? ("<br>" + lbl) : "") + "</p></html>";
            } else {
                String lbl = "";
                if (TrnLabelGenerator.DEBUG) {
                    lbl += "[nView: " + ((TreenotationImpl) o).getNView() + " id: " + ((Treenotation) o).getId() + "]";
                }
                Treenotation trn = (Treenotation) o;
                String s = null;
                try {
                    s = trn.getId() + ": " + trn.getType().getName();
                } catch (TreetonModelException e1) {
                    s = trn.getId() + ": " + "Problem with model";
                }
                rslt = "<html><p align=center>" + s + ((lbl.length() > 0) ? ("<br>" + lbl) : "") + "</p></html>";
            }
        }

        return rslt;
    }

    public int getSelectedIntervalStart() {
        return textSelectionIntervalStart;
    }

    public int getSelectedIntervalEnd() {
        return textSelectionIntervalEnd;
    }

    public void addAlwaysShownTrn(Treenotation trn) {
        alwaysShown.add(trn);

        Collections.sort(alwaysShown, new Comparator<Treenotation>() {
            public int compare(Treenotation o1, Treenotation o2) {
                return o1.getStartToken().compareTo(o2.getStartToken());
            }
        });
    }

    public void removeAlwaysShownTrn(Treenotation trn) {
        alwaysShown.remove(trn);
    }

    public void removeAlwaysShownTrns() {
        alwaysShown.clear();
    }

    public List<Treenotation> getAlwaysShownTrns() {
        return alwaysShown;
    }

    private static class LocalTrnIterator implements TrnIterator {
        LocalLengthComparator cmp = new LocalLengthComparator();
        NumeratedObject[] basesAndTrns = new NumeratedObject[10];
        int i;
        int n;

        void reset(TrnIterator it) {
            n = 0;
            while (it.hasNext()) {
                if (n == basesAndTrns.length) {
                    NumeratedObject[] tarr = new NumeratedObject[basesAndTrns.length * 3 / 2];
                    System.arraycopy(basesAndTrns, 0, tarr, 0, basesAndTrns.length);
                    basesAndTrns = tarr;
                }
                if (basesAndTrns[n] == null) {
                    basesAndTrns[n] = new NumeratedObject();
                }
                basesAndTrns[n].n = it.getTokenNumber();
                basesAndTrns[n++].o = it.next();
            }
            Arrays.sort(basesAndTrns, 0, n, cmp);
            i = 0;
        }

        public int getTokenNumber() {
            return basesAndTrns[i].n;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return i < n;
        }

        public Object next() {
            return basesAndTrns[i++].o;
        }

        private class LocalLengthComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                Treenotation t1 = (Treenotation) (((NumeratedObject) o1).o);
                Treenotation t2 = (Treenotation) (((NumeratedObject) o2).o);
                return t2.getTokenLength() - t1.getTokenLength();
            }
        }
    }

    class DrawingPane extends JComponent implements MouseListener, MouseMotionListener {
        private static final double transp_koef = 0.6;
        boolean up2date;
        boolean initialized; //флаг для инициализации данных при первом вызове paint()
        ArrayList<TokenCell> grid; //сетка, в которой хранится информация о том, что рисуется в пространстве окна
        int gridHeight, gridWidth; //реальные(используемые) размеры сетки в текущий момент (измеряется количеством ячеек-токенов)
        int gridAvailableHeight, gridAvailableWidth; //доступные(алоцированные) размеры сетки в текущий момент (измеряется количеством ячеек)
        int gridWidthToClean; //количество измеряется кол-вом ячеек
        int gridNumbersHeight; //px
        int cellWidth, cellHeight; //px
        int cornerWidth, cornerHeight; //px
        ArrayList<LabelInGrid> labels; //список всех label
        int nLabels;
        RelationInfo[] infoArr;
        RelationInfoComparator infoComparator;
        RelationInGrid[] relations;
        int nRelations;
        HashMap<Treenotation, RelationToSort> tHash;
        RelComparator relComparator;
        int additionAreaHeight;
        Font fontBig, fontSmall, fontNumbers, fontRelations;
        int bigDescent, smallDescent, numbersDescent, relationsDescent;
        int bigFontWidth, numbersFontHeight;
        AffineTransform standartTransform, rotatedTransform;
        Treenotation ROOT = TreetonFactory.newTreenotation(null, null, (TrnType) null);
        Stroke dashedStroke =
                new BasicStroke(
                        1,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,
                        0,
                        new float[]{3, 3},
                        0
                );
        Stroke waveStroke =
                new WaveStroke(1, 5, 3);
        private BlockStack relationsToSort = new BlockStack();
        private int lastDropX;
        private int lastDropY;
        private int[] xTrPoints = new int[3];
        private int[] yTrPoints = new int[3];

        DrawingPane() {
            super();
            fontBig = null;
            fontSmall = null;
            fontNumbers = null;
            fontRelations = null;
            bigDescent = -1;
            bigFontWidth = -1;
            numbersFontHeight = -1;
            smallDescent = -1;
            relationsDescent = -1;
            up2date = false;
            initialized = false;
            grid = new ArrayList<TokenCell>(128); //{TokenCell}*
            labels = new ArrayList<LabelInGrid>(128); //{LabelInGrid}*
            nLabels = 0;
            infoArr = new RelationInfo[20];
            infoComparator = new RelationInfoComparator();
            relations = new RelationInGrid[20];
            nRelations = 0;
            tHash = new HashMap<Treenotation, RelationToSort>(30);
            relComparator = new RelComparator();
            cellWidth = cellHeight = -1;
            cornerWidth = cornerHeight = -1;
            gridHeight = gridWidth = 0;
            gridWidthToClean = 0;
            gridAvailableHeight = 1;
            gridAvailableWidth = 0;
            additionAreaHeight = -1;
            gridNumbersHeight = -1;
            standartTransform = null;
            rotatedTransform = null;

            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }

        public void processMouseEvent(MouseEvent evt) {
            if (evt.getID() == MouseEvent.MOUSE_CLICKED || evt.getID() == MouseEvent.MOUSE_RELEASED || evt.getID() == MouseEvent.MOUSE_DRAGGED) {
                super.processMouseEvent(evt);
            } else {
                getParent().dispatchEvent(evt);
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (resetTextSelectionInterval) {
                textSelectionIntervalEnd = textSelectionIntervalStart = -1;
                resetTextSelectionInterval = false;
            }

            int px = e.getPoint().x;

            if (px > dp.getVisibleRect().getMaxX()) {
                horScroll.setValue(horScroll.getValue() + 1);
                horScroll.updateUI();
            }

            if (px < dp.getVisibleRect().getMinX()) {
                horScroll.setValue(horScroll.getValue() - 1);
                horScroll.updateUI();
            }


            int q = (int) (cur + (double) px / cellWidth);
            if (textSelectionIntervalStart == -1) {
                textSelectionIntervalEnd = textSelectionIntervalStart = q;
            } else {
                if (q > textSelectionIntervalStart) {
                    textSelectionIntervalEnd = q;
                }

                if (q < textSelectionIntervalEnd) {
                    textSelectionIntervalStart = q;
                }
            }

            if (textSelectionIntervalStart < 0)
                textSelectionIntervalStart = 0;
            if (textSelectionIntervalEnd >= document.length)
                textSelectionIntervalEnd = document.length - 1;

            dp.repaint();
        }

        public void mouseMoved(MouseEvent e) {
        }

        private RelationToSort newRelationToSort() {
            if (relationsToSort.isEmpty()) {
                return new RelationToSort();
            } else {
                RelationToSort rel = (RelationToSort) relationsToSort.pop();
                rel.nRelations = 0;
                rel.startCellX = -1;
                rel.endCellX = -1;
                return rel;
            }
        }

        private void freeRelationToSort(RelationToSort rel) {
            relationsToSort.push(rel);
        }

        private void increaseGridHeight(int height) {
            for (int i = 0; i < gridAvailableWidth; i++) {
                TokenCell tc = grid.get(i);
                tc.expand(height);
            }
        }

        private void clearGrid() {
            clearGrid(0, gridWidthToClean - 1);
            nLabels = 0;
            nRelations = 0;
        }

        private void clearGrid(int from, int to) {
            for (int i = from; i <= to; i++) {
                TokenCell tc = grid.get(i);
                tc.firstFree = -1;
                for (int j = 0; j < gridHeight; j++) {
                    Cell c = tc.cells.get(j);
                    c.reset();
                }
            }
        }

        private void fillGrid() throws TreetonModelException {
            clearGrid();
            Dimension dim = this.getSize();
            Fraction.assign(fakeFraction, (int) (cur / horStep), fractionHorStep.denominator);
            final Token first = treenotations.getTokenByOffset(fakeFraction, true);
            fake = cur + (double) dim.width / (double) cellWidth;
            if (fake > to) {
                fake = to;
            }
            Iterator it = treenotations.tokenIterator(first);
            int i = 0;
            Token t = null;
            Token last = null;
            while (it.hasNext()) { //Сперва бросаем токены
                t = (Token) it.next();
                if (t.compareTo(fake) >= 0) {
                    break;
                }
                last = t;
                TokenCell tc;
                if (i == gridAvailableWidth) { //расширяем сетку
                    tc = new TokenCell(gridAvailableHeight);
                    grid.add(tc);
                    gridAvailableWidth++;
                } else {
                    tc = grid.get(i);
                }
                tc.trn = t;
                tc.beginning = true;
                tc.ending = true;
                tc.firstFree = 0;
                i++;
            }
            gridWidth = i;
            gridWidthToClean = gridWidth;
            gridHeight = 0;
            if (gridWidth == 0)
                return;
            TrnIterator iter = treenotations.sortedTypeIterator(commonTypes, tokenTypes, first, last);

            final Token _last = last;

            dropTrns(
                    new TrnIterator() {
                        private int curNumber;
                        private Iterator<Treenotation> trnIt;
                        private Treenotation cur = findNext();

                        private Treenotation findNext() {
                            if (trnIt == null) {
                                trnIt = alwaysShown.iterator();
                            }

                            while (trnIt.hasNext()) {
                                Treenotation trn = trnIt.next();
                                if ((_last == null || trn.getStartToken().compareTo(_last) <= 0) && (first == null || first.compareTo(trn.getEndToken()) <= 0)) {
                                    curNumber = 0;
                                    Token ft = treenotations.firstToken();
                                    while (ft != null && ft != trn.getStartToken()) {
                                        ft = ft.getNextToken();
                                        curNumber++;
                                    }

                                    return trn;
                                }
                            }

                            return null;
                        }

                        public int getTokenNumber() {
                            return curNumber;
                        }

                        public boolean hasNext() {
                            return cur != null;
                        }

                        public Object next() {
                            Treenotation res = cur;
                            cur = findNext();
                            return res;
                        }

                        public void remove() {
                        }
                    },
                    0, gridWidth, -1, -1, -1, false);

            dropTrns(iter, 0, gridWidth, -1, -1, -1, true);

        }

        private int dropTrns(TrnIterator iter, int startGridShift, int tokenCellCount, int bg_r, int bg_g, int bg_b, boolean checkAlwaysShown) throws TreetonModelException {
            int maxHeight = 0;
            while (iter.hasNext()) {
                int base = iter.getTokenNumber();
                Treenotation trn = (Treenotation) iter.next();

                if (startGridShift == 0 && hidden.contains(trn) || checkAlwaysShown && alwaysShown.contains(trn)) {
                    continue;
                }

                if (!trnIsSolid(trn)) {
                    int shift = tokenCellCount;
                    int j = base + startGridShift;

                    TokenCell tc = grid.get(startGridShift);
                    Token startToken = (Token) tc.trn;
                    tc = grid.get(startGridShift + tokenCellCount - 1);
                    Token endToken = (Token) tc.trn;

                    while (j > startGridShift) {
                        tc = grid.get(j);

                        if ((Token) tc.trn == trn.getStartToken()) {
                            startToken = (Token) tc.trn;
                            break;
                        }
                        j--;
                    }

                    int startN = j;

                    while (j < startGridShift + tokenCellCount) {
                        TokenCell tcOld = grid.get(j);

                        if (startGridShift + shift + j - startN == gridAvailableWidth) {
                            tc = new TokenCell(gridAvailableHeight);
                            grid.add(tc);
                            gridAvailableWidth++;
                        } else {
                            tc = grid.get(startGridShift + shift + j - startN);
                        }
                        tc.trn = tcOld.trn;
                        tc.beginning = true;
                        tc.ending = true;
                        tc.firstFree = 0;
                        j++;

                        if ((Token) tcOld.trn == trn.getEndToken()) {
                            endToken = (Token) tc.trn;
                            break;
                        }
                    }
                    int currentWidth = j - startN;
                    if (startGridShift + shift + currentWidth > gridWidthToClean) {
                        gridWidthToClean = startGridShift + shift + currentWidth;
                    }
                    int oldLabelArrLen = nLabels;
                    int oldRelationsArrLen = nRelations;
                    Color clr = null;
                    try {
                        clr = GuiConfiguration.getInstance().getColorForType(trn.getType().getName());
                    } catch (TreetonModelException e) {
                        clr = Color.RED;
                    }
                    clearGrid(startGridShift + shift, startGridShift + shift + currentWidth - 1);
                    int height1;
                    LocalTrnIterator localTrnIterator = getTrnIterator();
                    localTrnIterator.reset(treenotations.internalTrnsIterator(startToken, endToken, trn));
                    height1 = dropTrns(localTrnIterator, startGridShift + shift, currentWidth, clr.getRed(), clr.getGreen(), clr.getBlue(), false);
                    freeTrnIterator(localTrnIterator);
                    int height = Math.max(height1, dropRelations(trn, treenotations.internalRelationsIterator(trn, startToken, endToken), startGridShift + shift, currentWidth, clr.getRed(), clr.getGreen(), clr.getBlue()));
                    int newLabelArrLen = nLabels;
                    int newRelationsArrLen = nRelations;
                    dropSingleTrn(trn, base, height + 2, startGridShift, tokenCellCount, bg_r, bg_g, bg_g);
                    if (lastDropY + height + 2 > maxHeight) {
                        maxHeight = lastDropY + height + 2;
                    }
                    for (int i = 0; i < currentWidth; i++) {
                        tc = grid.get(lastDropX + i);
                        TokenCell ntc = grid.get(startGridShift + shift + i);
                        Cell c = ((tc).cells.get(lastDropY));
                        c.trn = trn;
                        c.bottom = true;
                        mixColors(c, clr.getRed(), clr.getGreen(), clr.getBlue());

                        c = ((tc).cells.get(lastDropY + height + 1));
                        c.trn = trn;
                        c.top = true;
                        mixColors(c, clr.getRed(), clr.getGreen(), clr.getBlue());

                        for (int k = 0; k < height; k++) {
                            c = ((tc).cells.get(lastDropY + k + 1));
                            Cell nc = ((ntc).cells.get(k));
                            if (nc.trn == null || nc.trn == TRN_RELATION) {
                                c.trn = trn;
                                nc.bgr_beg = c.beginning ? false : true;
                                if (nc.trn == null) {
                                    nc.background_r = clr.getRed();
                                    nc.background_g = clr.getGreen();
                                    nc.background_b = clr.getBlue();
                                }
                                c.beginning = nc.beginning;
                                c.ending = nc.ending;
                            } else {
                                c.trn = nc.trn;
                                if (nc.parent == null) {
                                    c.parent = trn;
                                } else {
                                    c.parent = nc.parent;
                                }
                                c.beginning = nc.beginning;
                                c.ending = nc.ending;
                                c.bottom = nc.bottom;
                                c.top = nc.top;
                            }
                            mixColors(c, nc);
                            c.bgr_beg = nc.bgr_beg;
                            c.bgr_end = nc.bgr_end;
                        }
                    }
                    for (int k = oldLabelArrLen; k < newLabelArrLen; k++) {
                        LabelInGrid label = labels.get(k);
                        label.Y = label.Y + lastDropY + 1;
                        label.startCellX = label.startCellX - startGridShift - shift + lastDropX;
                        label.endCellX = label.endCellX - startGridShift - shift + lastDropX;
                    }
                    for (int k = oldRelationsArrLen; k < newRelationsArrLen; k++) {
                        RelationInGrid relation = relations[k];
                        if (relation.topY == -1) {
                            relation.topY = lastDropY + height + 1;
                        } else {
                            relation.topY += lastDropY + 1;
                        }
                        if (relation.leftCellY != -1)
                            relation.leftCellY += lastDropY + 1;
                        if (relation.rightCellY != -1)
                            relation.rightCellY += lastDropY + 1;
                        if (relation.leftCellStartX != -1)
                            relation.leftCellStartX -= startGridShift + shift - lastDropX;
                        if (relation.leftCellEndX != -1)
                            relation.leftCellEndX -= startGridShift + shift - lastDropX;
                        if (relation.rightCellStartX != -1)
                            relation.rightCellStartX -= startGridShift + shift - lastDropX;
                        if (relation.rightCellEndX != -1)
                            relation.rightCellEndX -= startGridShift + shift - lastDropX;
                    }

                } else {
                    dropSingleTrn(trn, base, 2, startGridShift, tokenCellCount, bg_r, bg_g, bg_b);
                    if (lastDropY + 2 > maxHeight) {
                        maxHeight = lastDropY + 2;
                    }
                }
            }
            return maxHeight;
        }

        private int dropRelations(Treenotation trn, RelationsIterator relationsIterator, int startGridShift, int tokenCellCount, int bg_r, int bg_g, int bg_b) {
            int maxHeight = 0;

            int len = 0;
            while (relationsIterator.next()) {
                if (len == infoArr.length) {
                    RelationInfo[] tarr = new RelationInfo[(infoArr.length * 3) / 2];
                    System.arraycopy(infoArr, 0, tarr, 0, infoArr.length);
                    infoArr = tarr;
                }
                if (infoArr[len] == null)
                    infoArr[len] = new RelationInfo();
                infoArr[len].host = relationsIterator.getHost() == trn ? ROOT : relationsIterator.getHost();
                infoArr[len].slave = relationsIterator.getSlave();
                infoArr[len++].type = relationsIterator.getType();
            }

            Arrays.sort(infoArr, 0, len, infoComparator);

            for (int i = 0; i < len; i++) {
                dropSingleRelation(infoArr[i], startGridShift, tokenCellCount, bg_r, bg_g, bg_b);
                if (lastDropY + 1 > maxHeight) {
                    maxHeight = lastDropY + 1;
                }
            }

            Iterator<RelationToSort> it = tHash.values().iterator();
            while (it.hasNext()) {
                RelationToSort rel = it.next();
                rel.sortRelations();
                freeRelationToSort(rel);
            }

            tHash.clear();

            return maxHeight;
        }

        private void dropSingleRelation(RelationInfo info, int startGridShift, int tokenCellCount, int bg_r, int bg_g, int bg_b) {
            if (info.host == ROOT) {
                Token min = info.slave.getStartToken();
                Token max = info.slave.getEndToken();

                int startj = startGridShift;
                for (int i = startGridShift; i < startGridShift + tokenCellCount; i++) {
                    TokenCell tc = grid.get(i);
                    if (tc.trn == min) {
                        startj = i;
                        break;
                    }
                }

                int endj = startGridShift + tokenCellCount - 1;
                for (int i = startGridShift + tokenCellCount - 1; i >= startGridShift; i--) {
                    TokenCell tc = grid.get(i);
                    if (tc.trn == max) {
                        endj = i;
                        break;
                    }
                }

                TokenCell tc = grid.get(startj);
                int i = gridHeight - 1;
                for (; i >= 0; i--) {
                    Cell cell = tc.cells.get(i);
                    if (cell.trn == info.slave) {
                        i++;
                        break;
                    }
                }


                RelationInGrid relation;
                if (nRelations == relations.length) {
                    RelationInGrid[] tarr = new RelationInGrid[(relations.length * 3) / 2];
                    System.arraycopy(relations, 0, tarr, 0, relations.length);
                    relations = tarr;
                }
                if (relations[nRelations] == null)
                    relations[nRelations] = new RelationInGrid();
                relation = relations[nRelations];
                relation.reset();

                nRelations++;

                relation.info.copyFrom(info);
                //relation.label.fillIn(info.type.getName());
                relation.leftCellStartX = startj;
                relation.leftCellEndX = -1;
                relation.leftCellY = i;
                relation.rightCellStartX = -1;
                relation.rightCellEndX = endj;
                relation.rightCellY = i;

                RelationToSort rts = tHash.get(info.slave);
                if (rts == null) {
                    rts = newRelationToSort();
                    rts.startCellX = startj;
                    rts.endCellX = endj;
                    tHash.put(info.slave, rts);
                }
                rts.addRelation(relation);

                relation.arrow = 1;
                relation.topY = -1;
                lastDropY = 0;
                return;
            }


            boolean openStart = false;
            boolean openEnd = false;

            TokenCell start = grid.get(startGridShift);
            TokenCell end = grid.get(startGridShift + tokenCellCount - 1);

            Treenotation minTrn;
            Treenotation maxTrn;
            Token min;
            Token endOfMin;
            Token max;
            Token startOfMax;

            int arrDir;

            int c1 = info.host.getStartToken().compareTo(info.slave.getStartToken());
            int c2 = info.host.getEndToken().compareTo(info.slave.getStartToken());
            int c3 = info.slave.getEndToken().compareTo(info.host.getStartToken());
            int c4 = info.host.getEndToken().compareTo(info.slave.getEndToken());

            if (c1 < 0) {
                minTrn = info.host;
                if (c2 < 0) { //1
                    min = info.host.getStartToken();
                    endOfMin = info.host.getEndToken();
                    startOfMax = info.slave.getStartToken();
                    max = info.slave.getEndToken();
                    maxTrn = info.slave;
                    arrDir = 1;
                } else { //2,3,4,5
                    min = info.host.getStartToken();
                    endOfMin = info.slave.getStartToken().getPreviousToken();
                    startOfMax = info.slave.getStartToken();
                    max = info.slave.getEndToken();
                    maxTrn = info.slave;
                    arrDir = 1;
                }
            } else if (c1 == 0) {
                minTrn = info.host;
                if (c4 < 0) { //6
                    min = info.host.getStartToken();
                    endOfMin = info.host.getEndToken();
                    startOfMax = info.host.getEndToken().getNextToken();
                    max = info.slave.getEndToken();
                    maxTrn = info.slave;
                    arrDir = 1;
                } else if (c4 == 0) {
                    if (c2 == 0) {
                        throw new UnsupportedOperationException("Unable to link two treenotations of the same length on the same offsets because the length of these annotations is equal to one token"); //todo: доделать
                    } else { //7
                        min = info.host.getStartToken();
                        endOfMin = info.host.getStartToken();
                        startOfMax = info.slave.getEndToken();
                        max = info.slave.getEndToken();
                        maxTrn = info.slave;
                        arrDir = 1;
                    }
                } else { //8
                    min = info.slave.getStartToken();
                    endOfMin = info.slave.getEndToken();
                    startOfMax = info.slave.getEndToken().getNextToken();
                    max = info.host.getEndToken();
                    maxTrn = info.host;
                    arrDir = -1;
                }
            } else {
                minTrn = info.slave;
                maxTrn = info.host;
                if (c3 < 0) { //9
                    min = info.slave.getStartToken();
                    endOfMin = info.slave.getEndToken();
                    startOfMax = info.host.getStartToken();
                    max = info.host.getEndToken();
                    arrDir = -1;
                } else { //10,11,12
                    min = info.slave.getStartToken();
                    endOfMin = info.host.getStartToken().getPreviousToken();
                    startOfMax = info.host.getStartToken();
                    max = info.host.getEndToken();
                    arrDir = -1;
                }
            }

            //minTrn = info.host.getStartToken().compareTo(info.slave.getStartToken()) < 0 ? info.host : info.slave;
            //maxTrn = info.host.getEndToken().compareTo(info.slave.getEndToken()) < 0 ? info.slave : info.host;

            /*min = info.host.getStartToken().compareTo(info.slave.getStartToken()) < 0 ? info.host.getStartToken() : info.slave.getStartToken();
    endOfMin = info.host.getStartToken().compareTo(info.slave.getStartToken()) < 0 ? info.host.getEndToken() : info.slave.getEndToken();
    max = info.host.getEndToken().compareTo(info.slave.getEndToken()) < 0 ? info.slave.getEndToken() : info.host.getEndToken();
    startOfMax = info.host.getEndToken().compareTo(info.slave.getEndToken()) < 0 ? info.slave.getStartToken() : info.host.getStartToken();*/

            if (((Token) start.trn).compareTo(endOfMin) > 0) {
                openStart = true;
            }
            if (((Token) end.trn).compareTo(startOfMax) < 0) {
                openEnd = true;
            }

            int startj = -1;
            int endOfStartj = -1;
            if (!openStart) {
                for (int i = startGridShift; i < startGridShift + tokenCellCount; i++) {
                    TokenCell tc = grid.get(i);
                    if (tc.trn == endOfMin) {
                        endOfStartj = i;
                        while (true) {
                            if (i == startGridShift) {
                                break;
                            }
                            tc = grid.get(i);
                            if (tc.trn == min) {
                                break;
                            }
                            i--;
                        }
                        startj = i;
                        break;
                    }
                }
                if (endOfStartj == -1) {
                    endOfStartj = startGridShift + tokenCellCount - 1;
                    int i = endOfStartj;
                    while (true) {
                        if (i == startGridShift) {
                            break;
                        }
                        TokenCell tc = grid.get(i);
                        if (tc.trn == min) {
                            break;
                        }
                        i--;
                    }
                    startj = i;
                }
            } else {
                startj = startGridShift;
            }

            int endj = -1;
            int startOfEndj = -1;
            if (!openEnd) {
                for (int i = startGridShift + tokenCellCount - 1; i >= startGridShift; i--) {
                    TokenCell tc = grid.get(i);
                    if (tc.trn == startOfMax) {
                        startOfEndj = i;
                        while (true) {
                            if (i == startGridShift + tokenCellCount - 1) {
                                break;
                            }
                            tc = grid.get(i);
                            if (tc.trn == max) {
                                break;
                            }
                            i++;
                        }
                        endj = i;
                        break;
                    }
                }
                if (startOfEndj == -1) {
                    startOfEndj = startGridShift;
                    int i = startOfEndj;
                    while (true) {
                        if (i == startGridShift + tokenCellCount - 1) {
                            break;
                        }
                        TokenCell tc = grid.get(i);
                        if (tc.trn == max) {
                            break;
                        }
                        i++;
                    }
                    endj = i;
                }
            } else {
                endj = startGridShift + tokenCellCount - 1;
            }

            int h = 0;
            while (true) {
                boolean enoughSpace = true;
                if (h + 1 > gridAvailableHeight) {
                    gridAvailableHeight = h + 1;
                    increaseGridHeight(gridAvailableHeight);
                }
                int nextH = -1;
                int j = startj;
                while (j <= endj) {
                    TokenCell tc = grid.get(j);
                    if (h < tc.firstFree) {
                        enoughSpace = false;
                        nextH = tc.firstFree;
                        break;
                    }
                    Cell c = (tc).cells.get(h);
                    if (
                            c.trn != null &&
                                    !(
                                            c.trn == TRN_RELATION &&
                                                    (
                                                            j == startj && !openStart && !c.beginning ||
                                                                    j == endj && !openEnd && !c.ending
                                                    )
                                    )
                            ) {
                        nextH = h + 1;
                        enoughSpace = false;
                        break;
                    }
                    j++;
                }
                if (enoughSpace == false) {
                    h = nextH;
                    if (h + 1 > gridAvailableHeight) {
                        gridAvailableHeight = h + 1;
                        increaseGridHeight(gridAvailableHeight);
                    }
                    continue;
                }
                j = startj;
                while (j <= endj) {
                    TokenCell tc = grid.get(j);
                    Cell c = ((tc).cells.get(h));
                    c.trn = TRN_RELATION;
                    c.top = true;
                    c.bgr_end = false;
                    c.bgr_beg = true;
                    c.background_r = bg_r;
                    c.background_g = bg_g;
                    c.background_b = bg_b;
                    if (j != startj || openStart) {
                        c.ending = true;
                    }
                    if (j != endj || openEnd) {
                        c.beginning = true;
                    }

                    if (tc.firstFree == h && !(j == startj && !openStart || j == endj && !openEnd))
                        tc.firstFree++;

                    j++;
                }

                int startHeight = -1;
                if (!openStart) {
                    TokenCell tc = grid.get(endOfStartj);
                    for (j = h - 1; j >= 0; j--) {
                        Cell c = ((tc).cells.get(j));
                        if (c.trn == minTrn) {
                            startHeight = j + 1;
                            break;
                        }
                    }
                }

                int endHeight = -1;
                if (!openEnd) {
                    TokenCell tc = grid.get(startOfEndj);
                    for (j = h - 1; j >= 0; j--) {
                        Cell c = ((tc).cells.get(j));
                        if (c.trn == maxTrn) {
                            endHeight = j + 1;
                            break;
                        }
                    }
                }

                RelationInGrid relation;
                if (nRelations == relations.length) {
                    RelationInGrid[] tarr = new RelationInGrid[(relations.length * 3) / 2];
                    System.arraycopy(relations, 0, tarr, 0, relations.length);
                    relations = tarr;
                }
                if (relations[nRelations] == null)
                    relations[nRelations] = new RelationInGrid();
                relation = relations[nRelations];
                relation.reset();

                nRelations++;

                relation.info.copyFrom(info);
                try {
                    relation.label.fillIn(info.type.getName());
                } catch (TreetonModelException e) {
                    relation.label.fillIn("Problem with model!!!");
                }
                if (openStart) {
                    relation.leftCellStartX = -1;
                    relation.leftCellEndX = -1;
                    relation.leftCellY = -1;
                } else {
                    relation.leftCellStartX = startj;
                    relation.leftCellEndX = endOfStartj;
                    relation.leftCellY = startHeight;
                    RelationToSort rts = tHash.get(minTrn);
                    if (rts == null) {
                        rts = newRelationToSort();
                        rts.startCellX = startj;
                        rts.endCellX = endOfStartj;
                        tHash.put(minTrn, rts);
                    }
                    rts.addRelation(relation);
                }

                if (openEnd) {
                    relation.rightCellStartX = -1;
                    relation.rightCellEndX = -1;
                    relation.rightCellY = -1;
                } else {
                    relation.rightCellStartX = startOfEndj;
                    relation.rightCellEndX = endj;
                    relation.rightCellY = endHeight;
                    RelationToSort rts = tHash.get(maxTrn);
                    if (rts == null) {
                        rts = newRelationToSort();
                        rts.startCellX = startOfEndj;
                        rts.endCellX = endj;
                        tHash.put(maxTrn, rts);
                    }
                    rts.addRelation(relation);
                }

                //if (maxTrn==info.slave) {
                relation.arrow = arrDir;
                /*} else {
                 relation.arrow = -1;
               } */

                relation.topY = h;

                if (h + 1 > gridHeight)
                    gridHeight = h + 1;
                lastDropY = h;
                break;
            }
        }

        private void mixColors(Cell dest, Cell src) {
            if (dest.background_r == -1) {
                dest.background_r = src.background_r;
            } else {
                dest.background_r = (int) ((dest.background_r * transp_koef + src.background_r) / 2);
            }
            if (dest.background_g == -1) {
                dest.background_g = src.background_g;
            } else {
                dest.background_g = (int) ((dest.background_g * transp_koef + src.background_g) / 2);
            }
            if (dest.background_b == -1) {
                dest.background_b = src.background_b;
            } else {
                dest.background_b = (int) ((dest.background_b * transp_koef + src.background_b) / 2);
            }
        }

        private void mixColors(Cell dest, int r, int g, int b) {
            if (dest.background_r == -1) {
                dest.background_r = r;
            } else {
                dest.background_r = (int) ((dest.background_r * transp_koef + r) / 2);
            }
            if (dest.background_g == -1) {
                dest.background_g = g;
            } else {
                dest.background_g = (int) ((dest.background_g * transp_koef + g) / 2);
            }
            if (dest.background_b == -1) {
                dest.background_b = b;
            } else {
                dest.background_b = (int) ((dest.background_b * transp_koef + b) / 2);
            }
        }

        private void dropSingleTrn(Treenotation trn, int base, int height, int startGridShift, int tokenCellCount, int bg_r, int bg_g, int bg_b) throws TreetonModelException {
            int h = 0;
            TokenCell baseCell = grid.get(base + startGridShift);
            while (true) {
                boolean enoughSpace = true;
                int j = base + startGridShift;
                if (h + height > gridAvailableHeight) {
                    gridAvailableHeight = h + height;
                    increaseGridHeight(gridAvailableHeight);
                }
                int nextH = -1;
                while (j >= startGridShift) {
                    TokenCell tc = grid.get(j);
                    if (h < tc.firstFree) {
                        enoughSpace = false;
                        nextH = tc.firstFree;
                        break;
                    }

                    for (int k = 0; k < height; k++) {
                        if (((tc).cells.get(h + k)).trn != null) {
                            nextH = h + k + 1;
                            enoughSpace = false;
                            break;
                        }
                    }
                    if ((Token) tc.trn == trn.getStartToken())
                        break;
                    j--;
                }
                if (enoughSpace == false) {
                    h = nextH;
                    if (h + height > gridAvailableHeight) {
                        gridAvailableHeight = h + height;
                        increaseGridHeight(gridAvailableHeight);
                    }
                    continue;
                } else {
                    j = base + 1 + startGridShift;
                    if ((Token) baseCell.trn != trn.getEndToken()) {
                        while (j < startGridShift + tokenCellCount) {
                            TokenCell tc = grid.get(j);
                            if (h < tc.firstFree) {
                                enoughSpace = false;
                                nextH = tc.firstFree;
                                break;
                            }
                            for (int k = 0; k < height; k++) {
                                if (((tc).cells.get(h + k)).trn != null) {
                                    nextH = h + k + 1;
                                    enoughSpace = false;
                                    break;
                                }
                            }
                            if ((Token) tc.trn == trn.getEndToken())
                                break;
                            j++;
                        }
                    }
                    if (enoughSpace == false) {
                        h = nextH;
                        if (h + height > gridAvailableHeight) {
                            gridAvailableHeight = h + height;
                            increaseGridHeight(gridAvailableHeight);
                        }
                        continue;
                    }
                }
                LabelInGrid label, caption;
                if (nLabels < labels.size()) {
                    label = labels.get(nLabels);
                    label.reset();
                } else {
                    label = new LabelInGrid();
                    labels.add(label);
                }
                nLabels++;
                label.Y = h;
                label.top = false;
                TrnLabelGenerator lg = labelGenerators.get(trn.getType().getName());
                if (lg != null) {
                    label.label.fillIn(trn, lg);
                } else {
                    label.label.fillIn(trn);
                }
                if (nLabels < labels.size()) {
                    caption = labels.get(nLabels);
                    caption.reset();
                } else {
                    caption = new LabelInGrid();
                    labels.add(caption);
                }
                nLabels++;
                caption.Y = h + height - 1;
                if (lg != null) {
                    caption.label.fillInCaption(trn, lg);
                } else {
                    caption.label.fillInCaption(trn);
                }
                j = base + startGridShift;
                while (j >= startGridShift) {
                    TokenCell tc = grid.get(j);
                    int k = 0;
                    for (; k < height - 1; k++) {
                        if (tc.firstFree == h + k)
                            tc.firstFree++;
                        Cell c = ((tc).cells.get(h + k));
                        c.trn = trn;
                        c.background_r = bg_r;
                        c.background_g = bg_g;
                        c.background_b = bg_b;
                        c.bgr_end = false;
                        if ((Token) tc.trn == trn.getStartToken()) {
                            c.beginning = true;
                            c.bgr_beg = false;
                        } else {
                            c.bgr_beg = true;
                        }
                        if (k == 0) {
                            c.bottom = true;
                        }
                    }
                    if (tc.firstFree == h + k)
                        tc.firstFree++;
                    Cell c = ((tc).cells.get(h + k));
                    c.trn = trn;
                    c.background_r = bg_r;
                    c.background_g = bg_g;
                    c.background_b = bg_b;
                    c.top = true;
                    c.bgr_end = false;
                    if ((Token) tc.trn == trn.getStartToken()) {
                        c.beginning = true;
                        c.bgr_beg = false;
                        label.startCellX = j;
                        caption.startCellX = j;
                        break;
                    } else {
                        c.bgr_beg = true;
                    }
                    j--;
                }
                if (label.startCellX == -1) {
                    label.startCellX = startGridShift;
                    caption.startCellX = startGridShift;
                }
                j = base + 1 + startGridShift;
                if ((Token) baseCell.trn != trn.getEndToken()) {
                    while (j < startGridShift + tokenCellCount) {
                        TokenCell tc = grid.get(j);
                        int k = 0;
                        for (; k < height - 1; k++) {
                            if (tc.firstFree == h + k)
                                tc.firstFree++;
                            Cell c = ((tc).cells.get(h + k));
                            c.trn = trn;
                            c.background_r = bg_r;
                            c.background_g = bg_g;
                            c.background_b = bg_b;
                            c.bgr_end = false;
                            c.bgr_beg = true;
                            if ((Token) tc.trn == trn.getEndToken()) {
                                c.ending = true;
                            }
                            if (k == 0) {
                                c.bottom = true;
                            }
                        }
                        if (tc.firstFree == h + k)
                            tc.firstFree++;
                        Cell c = ((tc).cells.get(h + k));
                        c.trn = trn;
                        c.background_r = bg_r;
                        c.background_g = bg_g;
                        c.background_b = bg_b;
                        c.top = true;
                        c.bgr_end = false;
                        c.bgr_beg = true;
                        if ((Token) tc.trn == trn.getEndToken()) {
                            c.ending = true;
                            label.endCellX = j;
                            caption.endCellX = j;
                            break;
                        }
                        j++;
                    }
                    if (label.endCellX == -1) {
                        label.endCellX = startGridShift + tokenCellCount - 1;
                        caption.endCellX = startGridShift + tokenCellCount - 1;
                        ;
                    }
                } else {
                    int k = 0;
                    for (; k < height - 1; k++) {
                        Cell c = ((baseCell).cells.get(h + k));
                        c.ending = true;
                        if (k == 0) {
                            c.bottom = true;
                        }
                    }
                    Cell c = ((baseCell).cells.get(h + k));
                    c.ending = true;
                    c.top = true;
                    label.endCellX = base + startGridShift;
                    caption.endCellX = base + startGridShift;
                }
                if (h + height > gridHeight)
                    gridHeight = h + height;
                lastDropX = label.startCellX;
                lastDropY = h;

                if (lastDropX > startGridShift) {
                    TokenCell tc = grid.get(lastDropX);
                    Cell c = ((tc).cells.get(lastDropY));
                    if (c.beginning) {
                        tc = grid.get(lastDropX - 1);
                        for (int k = 0; k < height; k++) {
                            c = ((tc).cells.get(lastDropY + k));
                            c.bgr_end = true;
                        }
                    }
                }

                break;
            }
        }

        private Cell getCell(Point p) {
            int px = p.x;
            int py = p.y;
            int i;
            TokenCell tc = null;
            for (i = 0; i < gridWidth; i++) {
                tc = grid.get(i);

                int xStart = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                int xEnd = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);

                if (xStart <= px && px <= xEnd) {
                    break;
                }
            }

            if (i < gridWidth && tc != null) {
                Dimension dim = this.getSize();
                for (int j = 0; j < gridHeight; j++) {
                    Cell c = tc.cells.get(j);
                    if (c.trn != null) {
                        int y = dim.height - 1 - additionAreaHeight - cellHeight * (j + 1) + (int) Math.floor(currentHeight * cellHeight);
                        int h = cellHeight;
                        if (y <= py && py <= y + h && py < dim.height - 1 - additionAreaHeight) {
                            return c;
                        }
                    }
                }
            }
            return null;
        }

        public void mouseClicked(MouseEvent e) {
            if (!isEnabled())
                return;

            if (e.getButton() == MouseEvent.BUTTON1) {
                textSelectionIntervalStart = textSelectionIntervalEnd = -1;
                resetTextSelectionInterval = false;
                Cell newSelection = getCell(e.getPoint());

                boolean shiftKey = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;


                Treenotation trn = newSelection == null ? null : (Treenotation) newSelection.trn;
                Treenotation parent = newSelection == null ? null : newSelection.parent;


                updateSelection(shiftKey, trn, parent);


                notifyListeners(e.getClickCount());
                dp.repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public String getToolTipText(MouseEvent e) {
            return TreenotationViewPanel.this.getToolTipText(e);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            resetTextSelectionInterval = true;
            setToolTipText(null);
        }

        private void drawCorner(Graphics2D g, int startx, int starty, int orientation) {
            //orientation is 0 for upper left corner, 1 - for upper right, 2 - for lower right,
            //3 - for lower left
            if (orientation == 0) {
                g.drawArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2, cornerHeight * 2, 0, 90);
            } else if (orientation == 1) {
                g.drawArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2, cornerHeight * 2, 90, 90);
            } else if (orientation == 2) {
                g.drawArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2, cornerHeight * 2, 180, 90);
            } else if (orientation == 3) {
                g.drawArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2, cornerHeight * 2, 270, 90);
            }
        }

        private void fillCorner(Graphics2D g, int startx, int starty, int orientation) {
            //orientation is 0 for upper left corner, 1 - for upper right, 2 - for lower right,
            //3 - for lower left
            if (orientation == 0) {
                g.fillArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2 + 1, cornerHeight * 2 + 1, 0, 90);
            } else if (orientation == 1) {
                g.fillArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2 + 1, cornerHeight * 2 + 1, 90, 90);
            } else if (orientation == 2) {
                g.fillArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2 + 1, cornerHeight * 2 + 1, 180, 90);
            } else if (orientation == 3) {
                g.fillArc(startx - cornerWidth, starty - cornerHeight, cornerWidth * 2 + 1, cornerHeight * 2 + 1, 270, 90);
            }
        }

        private void paintGrid(Graphics2D g) {
            Dimension dim = this.getSize();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if (gridWidth > 0) {  //начинаем отрисовку сетки
                TokenCell tc = grid.get(0);
                int start = ((Treenotation) tc.trn).toInt(); //начало (в символах) первого токена
                tc = grid.get(gridWidth - 1);
                int end = ((Treenotation) tc.trn).endToInt(); //конец (в символах) последнего токена

                fake = to - cur;
                if (fake > 0) { //рисование пустой области на всем пространстве
                    g.setColor(emptyAreaColor);
                    fake *= cellWidth;
                    if (fake < dim.width)
                        g.fillRect((int) Math.ceil(fake), 0, dim.width - 1 - (int) Math.ceil(fake), dim.height - 1 - additionAreaHeight);
                }


                fake = (start - cur) * cellWidth;

                int h = dim.height - 2 - bigDescent - (gridIsOn ? gridNumbersHeight : 0);
                int xl = (int) Math.floor(fake); // x1 всегда <= 0
                for (int i = start; i < end; i++) { // рисуем затемненный фон текстового выделения и курсора
                    boolean textSelection = textSelectionIntervalStart >= 0 && (i >= textSelectionIntervalStart && i <= textSelectionIntervalEnd);
                    boolean cursor = i == curPos && showCursor;
                    if (cursor || textSelection) {
                        g.setColor(textSelection ? (cursor ? cursorWithSelectionColor : selectionColor) : cursorColor);
                        g.fillRect(xl, 0, cellWidth - 1, dim.height - 1 - additionAreaHeight);
                    }
                    if (gridIsOn) {  // белая вертикальная линяи сетки
                        g.setColor(Color.WHITE);
                        g.drawLine(xl, 0, xl, dim.height - 1 - additionAreaHeight);
                    }
                    xl += cellWidth;
                }
                if (gridIsOn) { // белая вертикальная линяи сетки
                    g.setColor(Color.WHITE);
                    g.drawLine(xl, 0, xl, dim.height - 1 - additionAreaHeight);
                }


                for (int i = 0; i < gridWidth; i++) {
                    tc = grid.get(i);

                    int xStart = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                    int xEnd = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);

                    for (int j = 0; j < gridHeight; j++) {
                        Cell c = tc.cells.get(j);
                        if (c.trn != null) {
                            int y = dim.height - 1 - additionAreaHeight - cellHeight * (j + 1) + (int) Math.floor(currentHeight * cellHeight);
                            h = cellHeight - 2;
                            if (c.background_r != -1) { // Отрисовка фона ячейки
                                g.setColor(new Color(c.background_r, c.background_g, c.background_b, 96));
                                boolean ns = c.trn != null && !trnIsSolid((Treenotation) c.trn) && c.parent == null;
                                int top = c.top && ns ? y : y - 1;
                                int height = ns ? c.bottom ? c.top ? h : h + 1 : c.top ? h + 1 : h + 2 : h + 2;
                                if (c.bgr_beg && c.bgr_end) {
                                    g.fillRect(xStart, top, xEnd - xStart + 1, height);
                                } else if (c.bgr_beg) {
                                    g.fillRect(xStart, top, xEnd - xStart, height);
                                } else if (c.bgr_end) {
                                    g.fillRect(xStart + 1, top, xEnd - xStart, height);
                                } else {
                                    g.fillRect(xStart + 1, top, xEnd - xStart - 1, height);
                                }
                            }
                            if (c.trn == TRN_RELATION) { //отрисовка ячейки под синтаксическую связь   (устаревший код!!!)
                                g.setColor(Color.red);
                                if (!c.ending) {
                                    g.drawLine(xStart + (xEnd - xStart) / 2, y + h, xEnd, y - 1);
                                    g.drawLine(xStart + (xEnd - xStart) / 2, y - 1, xEnd, y + h);
                                } else if (!c.beginning) {
                                    g.drawLine(xStart, y + h, xEnd - (xEnd - xStart) / 2, y - 1);
                                    g.drawLine(xStart, y - 1, xEnd - (xEnd - xStart) / 2, y + h);
                                } else {
                                    g.drawLine(xStart, y + h, xEnd, y - 1);
                                    g.drawLine(xStart, y - 1, xEnd, y + h);
                                }
                            } else {
                                try {
                                    g.setColor(GuiConfiguration.getInstance().getColorForType(((Treenotation) c.trn).getType().getName()));
                                } catch (TreetonModelException e) {
                                    g.setColor(Color.RED);
                                }
                                if (c.beginning && c.ending) {
                                    if (c.bottom) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart + 1 + cornerWidth, y - 1, xEnd - xStart - 2 * cornerWidth - 1, h + 1);
                                            g.fillRect(xStart + 1, y - 1, cornerWidth, h + 1 - cornerHeight);
                                            g.fillRect(xEnd - cornerWidth, y - 1, cornerWidth, h + 1 - cornerHeight);

                                            fillCorner(g, xStart + 1 + cornerWidth, y + h - cornerHeight - 1, 2);
                                            fillCorner(g, xEnd - 1 - cornerWidth, y + h - cornerHeight - 1, 3);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1 + cornerWidth, y + h - 1, xEnd - cornerWidth - 1, y + h - 1);
                                            g.drawLine(xStart + 1, y - 1, xStart + 1, y + h - cornerHeight - 1);
                                            g.drawLine(xEnd - 1, y - 1, xEnd - 1, y + h - cornerHeight - 1);

                                            drawCorner(g, xStart + 1 + cornerWidth, y + h - cornerHeight - 1, 2);
                                            drawCorner(g, xEnd - 1 - cornerWidth, y + h - cornerHeight - 1, 3);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1, y + h - 1, xEnd - 1, y + h - 1);
                                            g.drawLine(xStart + 1, y + h - 2, xEnd - 1, y + h - 2);
                                            g.drawLine(xStart + 1, y + h - 3, xStart + 1, y + h - 5);
                                            g.drawLine(xStart + 2, y + h - 3, xStart + 2, y + h - 5);
                                            g.drawLine(xEnd - 1, y + h - 3, xEnd - 1, y + h - 5);
                                            g.drawLine(xEnd - 2, y + h - 3, xEnd - 2, y + h - 5);
                                        }
                                    } else if (c.top) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart + 1 + cornerWidth, y, xEnd - xStart - 2 * cornerWidth - 1, h + 1);
                                            g.fillRect(xStart + 1, y + cornerHeight, cornerWidth, h + 1 - cornerHeight);
                                            g.fillRect(xEnd - cornerWidth, y + cornerHeight, cornerWidth, h + 1 - cornerHeight);

                                            fillCorner(g, xStart + 1 + cornerWidth, y + cornerHeight, 1);
                                            fillCorner(g, xEnd - 1 - cornerWidth, y + cornerHeight, 0);

                                            g.setColor(Color.black);
                                            g.drawLine(xStart + 1 + cornerWidth, y, xEnd - cornerWidth - 1, y);
                                            g.drawLine(xStart + 1, y + cornerHeight, xStart + 1, y + h);
                                            g.drawLine(xEnd - 1, y + cornerHeight, xEnd - 1, y + h);

                                            drawCorner(g, xStart + 1 + cornerWidth, y + cornerHeight, 1);
                                            drawCorner(g, xEnd - 1 - cornerWidth, y + cornerHeight, 0);
                                        } else {
                                            if (((Treenotation) c.trn).isLocked()) {
                                                g.drawImage(GuiResources.imageLock,
                                                        xEnd - GuiResources.imageLock.getWidth(null),
                                                        y + 1, null);
                                            } else if (((Treenotation) c.trn).getContext() == null) {
                                                g.drawImage(GuiResources.imageSocket,
                                                        xEnd - 2 - GuiResources.imageSocket.getWidth(null),
                                                        y + 3, null);
                                            } else if (((Treenotation) c.trn).getContext() instanceof TopologyManager
                                                /*((Treenotation)c.trn).getContext() instanceof SyntaxStructureEx*/
                                                    ) {
                                                g.drawImage(GuiResources.imagePlug,
                                                        xEnd - 2 - GuiResources.imagePlug.getWidth(null),
                                                        y + 3, null);
                                            }

                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1, y, xEnd - 1, y);
                                            g.drawLine(xStart + 1, y + 1, xEnd - 1, y + 1);
                                            g.drawLine(xStart + 1, y + 2, xStart + 1, y + 4);
                                            g.drawLine(xStart + 2, y + 2, xStart + 2, y + 4);
                                            g.drawLine(xEnd - 1, y + 2, xEnd - 1, y + 4);
                                            g.drawLine(xEnd - 2, y + 2, xEnd - 2, y + 4);
                                        }
                                    }
                                } else if (c.beginning) {
                                    if (c.bottom) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart + 1 + cornerWidth, y - 1, xEnd - xStart - cornerWidth, h + 1);
                                            g.fillRect(xStart + 1, y - 1, cornerWidth, h + 1 - cornerHeight);
                                            fillCorner(g, xStart + 1 + cornerWidth, y + h - cornerHeight - 1, 2);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1 + cornerWidth, y + h - 1, xEnd, y + h - 1);
                                            g.drawLine(xStart + 1, y - 1, xStart + 1, y + h - cornerHeight - 1);

                                            drawCorner(g, xStart + 1 + cornerWidth, y + h - cornerHeight - 1, 2);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1, y + h - 1, xEnd, y + h - 1);
                                            g.drawLine(xStart + 1, y + h - 2, xEnd, y + h - 2);
                                            g.drawLine(xStart + 1, y + h - 3, xStart + 1, y + h - 5);
                                            g.drawLine(xStart + 2, y + h - 3, xStart + 2, y + h - 5);
                                        }
                                    } else if (c.top) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart + 1 + cornerWidth, y, xEnd - xStart - cornerWidth, h + 1);
                                            g.fillRect(xStart + 1, y + cornerHeight, cornerWidth, h + 1 - cornerHeight);

                                            fillCorner(g, xStart + 1 + cornerWidth, y + cornerHeight, 1);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1 + cornerWidth, y, xEnd, y);
                                            g.drawLine(xStart + 1, y + cornerHeight, xStart + 1, y + h);

                                            drawCorner(g, xStart + 1 + cornerWidth, y + cornerHeight, 1);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart + 1, y, xEnd, y);
                                            g.drawLine(xStart + 1, y + 1, xEnd, y + 1);
                                            g.drawLine(xStart + 1, y + 2, xStart + 1, y + 4);
                                            g.drawLine(xStart + 2, y + 2, xStart + 2, y + 4);
                                        }
                                    }
                                } else if (c.ending) {
                                    if (c.bottom) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart, y - 1, xEnd - xStart - cornerWidth, h + 1);
                                            g.fillRect(xEnd - cornerWidth, y - 1, cornerWidth, h + 1 - cornerHeight);
                                            fillCorner(g, xEnd - 1 - cornerWidth, y + h - cornerHeight - 1, 3);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y + h - 1, xEnd - cornerWidth - 1, y + h - 1);
                                            g.drawLine(xEnd - 1, y - 1, xEnd - 1, y + h - cornerHeight - 1);

                                            drawCorner(g, xEnd - 1 - cornerWidth, y + h - cornerHeight - 1, 3);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y + h - 1, xEnd - 1, y + h - 1);
                                            g.drawLine(xStart, y + h - 2, xEnd - 1, y + h - 2);
                                            g.drawLine(xEnd - 1, y + h - 3, xEnd - 1, y + h - 5);
                                            g.drawLine(xEnd - 2, y + h - 3, xEnd - 2, y + h - 5);
                                        }
                                    } else if (c.top) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart, y, xEnd - xStart - cornerWidth, h + 1);
                                            g.fillRect(xEnd - cornerWidth, y + cornerHeight, cornerWidth, h + 1 - cornerHeight);
                                            fillCorner(g, xEnd - 1 - cornerWidth, y + cornerHeight, 0);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y, xEnd - cornerWidth - 1, y);
                                            g.drawLine(xEnd - 1, y + cornerHeight, xEnd - 1, y + h);

                                            drawCorner(g, xEnd - 1 - cornerWidth, y + cornerHeight, 0);
                                        } else {
                                            if (((Treenotation) c.trn).isLocked()) {
                                                g.drawImage(GuiResources.imageLock,
                                                        xEnd - GuiResources.imageLock.getWidth(null),
                                                        y + 1, null);
                                            } else if (((Treenotation) c.trn).getContext() == null) {
                                                g.drawImage(GuiResources.imageSocket,
                                                        xEnd - 2 - GuiResources.imageSocket.getWidth(null),
                                                        y + 3, null);
                                            } else if (((Treenotation) c.trn).getContext() instanceof TopologyManager
                                                /*((Treenotation)c.trn).getContext() instanceof SyntaxStructureEx*/
                                                    ) {
                                                g.drawImage(GuiResources.imagePlug,
                                                        xEnd - 2 - GuiResources.imagePlug.getWidth(null),
                                                        y + 3, null);
                                            }

                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y, xEnd - 1, y);
                                            g.drawLine(xStart, y + 1, xEnd - 1, y + 1);
                                            g.drawLine(xEnd - 1, y + 2, xEnd - 1, y + 4);
                                            g.drawLine(xEnd - 2, y + 2, xEnd - 2, y + 4);
                                        }
                                    }
                                } else {
                                    if (c.bottom) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart, y - 1, xEnd - xStart + 1, h + 1);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y + h - 1, xEnd, y + h - 1);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y + h - 1, xEnd, y + h - 1);
                                            g.drawLine(xStart, y + h - 2, xEnd, y + h - 2);
                                        }
                                    } else if (c.top) {
                                        if (trnIsSolid((Treenotation) c.trn)) {
                                            g.fillRect(xStart, y, xEnd - xStart + 1, h + 1);

                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y, xEnd, y);
                                        } else {
                                            g.setColor(Color.black);

                                            g.drawLine(xStart, y, xEnd, y);
                                            g.drawLine(xStart, y + 1, xEnd, y + 1);
                                        }
                                    }
                                }
                                if (isEnabled() && selected.containsKey(c.trn)) {
                                    Treenotation selectionParent = selected.get(c.trn);
                                    if (c.top) {
                                        g.setColor(Color.white);
                                        g.drawLine(xStart, y - 1, xEnd, y - 1);
                                        g.setColor(Color.black);
                                        g.drawLine(xStart, y - 2, xEnd, y - 2);

                                        if (c.beginning)
                                            g.drawLine(xStart - 1, y - 2, xStart - 1, y + h + 1);
                                        if (c.ending)
                                            g.drawLine(xEnd + 1, y - 2, xEnd + 1, y + h + 1);
                                    } else if (c.bottom) {
                                        g.setColor(Color.white);
                                        g.drawLine(xStart, y + h, xEnd, y + h);
                                        g.setColor(Color.black);
                                        g.drawLine(xStart, y + h + 1, xEnd, y + h + 1);

                                        if (c.beginning)
                                            g.drawLine(xStart - 1, y - 2, xStart - 1, y + h + 1);
                                        if (c.ending)
                                            g.drawLine(xEnd + 1, y - 2, xEnd + 1, y + h + 1);
                                    }
                                    if (selectionParent == c.parent) {
                                        g.setColor(trnSelectionColorTransparent);
                                    } else {
                                        g.setColor(trnSelectionColorTransparentWeak);
                                    }
                                    if (c.bgr_beg && c.bgr_end) {
                                        g.fillRect(xStart, y - 1, xEnd - xStart + 1, h + 2);
                                    } else if (c.bgr_beg) {
                                        g.fillRect(xStart, y - 1, xEnd - xStart, h + 2);
                                    } else if (c.bgr_end) {
                                        g.fillRect(xStart + 1, y - 1, xEnd - xStart, h + 2);
                                    } else {
                                        g.fillRect(xStart + 1, y - 1, xEnd - xStart - 1, h + 2);
                                    }
                                }
                            }
                        }
                    }
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setFont(fontSmall);

                g.setColor(Color.WHITE);
                for (int i = 0; i < nLabels; i++) {
                    LabelInGrid label = labels.get(i);

                    tc = grid.get(label.startCellX);
                    int xStart = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                    tc = grid.get(label.endCellX);
                    int xEnd = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);

                    int y = dim.height - 1 - additionAreaHeight - cellHeight * label.Y + (int) Math.floor(currentHeight * cellHeight) - smallDescent - 3;
                    if (!label.top)
                        y -= 2;
                    label.label.draw(g, Math.max(xStart + cornerWidth, 0), Math.min(xEnd - cornerWidth, dim.width - 1), y);
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(fontRelations);
                g.setColor(Color.black);
                for (int i = 0; i < nRelations; i++) {
                    RelationInGrid relation = relations[i];

                    if (relation.info.host == ROOT) {
                        if (relation.info.type != null) {
                            int topY = dim.height - additionAreaHeight - cellHeight * relation.topY + (int) Math.floor(currentHeight * cellHeight) - cellHeight;
                            int x;
                            tc = grid.get(relation.rightCellEndX);
                            int rightEndX = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            tc = grid.get(relation.leftCellStartX);
                            int leftStartX = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            x = (int) (leftStartX + (rightEndX - leftStartX) * relation.rightK);
                            int endY = dim.height - additionAreaHeight - cellHeight * relation.rightCellY + (int) Math.floor(currentHeight * cellHeight);
                            Stroke oldStroke = g.getStroke();
                            try {
                                if (relation.info.type.getName().equals(TrnRelationTypeStorage.root_RELATION_name)) {
                                    g.setStroke(dashedStroke);
                                } else if (relation.info.type.getName().equals(TrnRelationTypeStorage.root_path_RELATION_name)) {
                                    g.setStroke(waveStroke);
                                }
                            } catch (TreetonModelException e) {
                                g.setStroke(null);
                            }
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_OFF);
                            g.drawLine(x, topY, x, endY - cellHeight / 3 + 1);
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                            g.setStroke(oldStroke);
                            if (relation.arrow == 1) {
                                xTrPoints[0] = x - 2;
                                yTrPoints[0] = endY - cellHeight / 3;
                                xTrPoints[1] = x + 3;
                                yTrPoints[1] = endY - cellHeight / 3;
                                xTrPoints[2] = x;
                                yTrPoints[2] = endY;
                                g.fillPolygon(xTrPoints, yTrPoints, 3);
                            }
                        }
                    } else {
                        int topY = dim.height - 1 - additionAreaHeight - cellHeight * relation.topY + (int) Math.floor(currentHeight * cellHeight) - cellHeight / 2;
                        int xStart, xEnd;
                        if (relation.leftCellStartX == -1 && relation.rightCellEndX == -1) {
                            tc = grid.get(0);
                            xStart = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            tc = grid.get(gridWidth - 1);
                            xEnd = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                        } else if (relation.leftCellStartX == -1) {
                            tc = grid.get(relation.rightCellStartX);
                            int rightStartX = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            tc = grid.get(relation.rightCellEndX);
                            int rightEndX = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            xEnd = (int) (rightStartX + (rightEndX - rightStartX) * relation.rightK);
                            tc = grid.get(0);
                            xStart = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            int endY = dim.height - 1 - additionAreaHeight - cellHeight * relation.rightCellY + (int) Math.floor(currentHeight * cellHeight);
                            g.drawLine(xEnd, topY, xEnd, endY);
                            if (relation.arrow == 1) {
                                xTrPoints[0] = xEnd - 2;
                                yTrPoints[0] = endY - cellHeight / 3;
                                xTrPoints[1] = xEnd + 3;
                                yTrPoints[1] = endY - cellHeight / 3;
                                xTrPoints[2] = xEnd;
                                yTrPoints[2] = endY;
                                g.fillPolygon(xTrPoints, yTrPoints, 3);
                            }
                        } else if (relation.rightCellEndX == -1) {
                            tc = grid.get(relation.leftCellStartX);
                            int leftStartX = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            tc = grid.get(relation.leftCellEndX);
                            int leftEndX = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            xStart = (int) (leftStartX + (leftEndX - leftStartX) * relation.leftK);
                            tc = grid.get(gridWidth - 1);
                            xEnd = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            int startY = dim.height - 1 - additionAreaHeight - cellHeight * relation.leftCellY + (int) Math.floor(currentHeight * cellHeight);
                            g.drawLine(xStart, topY, xStart, startY);
                            if (relation.arrow == -1) {
                                xTrPoints[0] = xStart - 2;
                                yTrPoints[0] = startY - cellHeight / 3;
                                xTrPoints[1] = xStart + 3;
                                yTrPoints[1] = startY - cellHeight / 3;
                                xTrPoints[2] = xStart;
                                yTrPoints[2] = startY;
                                g.fillPolygon(xTrPoints, yTrPoints, 3);
                            }
                        } else {
                            tc = grid.get(relation.rightCellStartX);
                            int rightStartX = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            tc = grid.get(relation.rightCellEndX);
                            int rightEndX = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            xEnd = (int) (rightStartX + (rightEndX - rightStartX) * relation.rightK);
                            tc = grid.get(relation.leftCellStartX);
                            int leftStartX = (int) Math.floor((((Treenotation) tc.trn).toDouble() - cur) * cellWidth);
                            tc = grid.get(relation.leftCellEndX);
                            int leftEndX = (int) Math.floor((((Treenotation) tc.trn).endToDouble() - cur) * cellWidth);
                            xStart = (int) (leftStartX + (leftEndX - leftStartX) * relation.leftK);
                            int endY = dim.height - 1 - additionAreaHeight - cellHeight * relation.rightCellY + (int) Math.floor(currentHeight * cellHeight);
                            g.drawLine(xEnd, topY, xEnd, endY);
                            int startY = dim.height - 1 - additionAreaHeight - cellHeight * relation.leftCellY + (int) Math.floor(currentHeight * cellHeight);
                            g.drawLine(xStart, topY, xStart, startY);
                            if (relation.arrow == 1) {
                                xTrPoints[0] = xEnd - 2;
                                yTrPoints[0] = endY - cellHeight / 3;
                                xTrPoints[1] = xEnd + 3;
                                yTrPoints[1] = endY - cellHeight / 3;
                                xTrPoints[2] = xEnd;
                                yTrPoints[2] = endY;
                                g.fillPolygon(xTrPoints, yTrPoints, 3);
                            } else if (relation.arrow == -1) {
                                xTrPoints[0] = xStart - 2;
                                yTrPoints[0] = startY - cellHeight / 3;
                                xTrPoints[1] = xStart + 3;
                                yTrPoints[1] = startY - cellHeight / 3;
                                xTrPoints[2] = xStart;
                                yTrPoints[2] = startY;
                                g.fillPolygon(xTrPoints, yTrPoints, 3);
                            }
                        }
                        g.drawLine(xStart, topY, xEnd, topY);
                        int y = dim.height - 1 - additionAreaHeight - cellHeight * relation.topY + (int) Math.floor(currentHeight * cellHeight) - relationsDescent - 4;
                        relation.label.drawBackGround(g, Math.max(xStart + 4, 0), Math.min(xEnd - 4, dim.width - 1), y, relationLabelBackGroundColor, true);
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);
                        relation.label.draw(g, Math.max(xStart + 4, 0), Math.min(xEnd - 4, dim.width - 1), y);
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);


                g.setColor(textBackGroundColor); //отрисовка полосы текста
                g.fillRect(0, dim.height - 1 - additionAreaHeight, dim.width, additionAreaHeight - (gridIsOn ? gridNumbersHeight : 0) - 1);
                if (gridIsOn) {
                    g.setColor(gridNumbersBackGroundColor);
                    g.fillRect(0, dim.height - 1 - gridNumbersHeight - 2, dim.width, gridNumbersHeight + 3);
                }

                g.setFont(fontBig);

                fake = (start - cur) * cellWidth;

                h = dim.height - 2 - bigDescent - (gridIsOn ? gridNumbersHeight : 0);
                xl = (int) Math.floor(fake);
                int x = xl + (cellWidth - bigFontWidth) / 2;
                for (int i = start; i < end; i++) {
                    boolean textSelection = textSelectionIntervalStart >= 0 && (i >= textSelectionIntervalStart && i <= textSelectionIntervalEnd);
                    boolean cursor = i == curPos && showCursor;
                    if (cursor || textSelection) {
                        g.setColor(textSelection ? (cursor ? cursorWithTextSelectionColor : textSelectionColor) : textCursorColor);
                        g.fillRect(xl, dim.height - 1 - additionAreaHeight, cellWidth - 1, dim.height - 1);
                    }
                    if (gridIsOn) {
                        g.setColor(Color.WHITE);
                        g.drawLine(xl, dim.height - 1 - additionAreaHeight, xl, dim.height - 1);
                    }
                    g.setColor(Color.BLACK);
                    g.drawChars(document, i, 1, x, h);
                    x += cellWidth;
                    xl += cellWidth;
                }
                if (gridIsOn) {
                    g.setColor(Color.WHITE);
                    g.drawLine(xl, dim.height - 1 - additionAreaHeight, xl, dim.height - 1);
                }


                if (gridIsOn) { //отрисовка номеров символов
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);

                    fake = (start - cur) * cellWidth;

                    standartTransform = g.getTransform();
                    rotatedTransform.setTransform(standartTransform);
                    rotatedTransform.rotate(-3.1415926 / 2);

                    g.setTransform(rotatedTransform);
                    x = (int) Math.floor(fake) + (cellWidth - (numbersFontHeight - numbersDescent)) / 2 + numbersFontHeight - numbersDescent;
                    g.setFont(fontNumbers);
                    for (int i = start; i < end; i++) {
                        g.setColor(gridNumbersColor);
                        String s = Integer.toString(i);
                        s.getChars(0, s.length(), spaces, maxOffsetLen - s.length());
                        g.drawChars(spaces, 0, maxOffsetLen, -dim.height + 2, x);
                        Arrays.fill(spaces, maxOffsetLen - s.length(), maxOffsetLen - 1, ' ');
                        x += cellWidth;
                    }
                    g.setTransform(standartTransform);
                }
            }
        }

        public void paint(Graphics _g) {
            Graphics2D g = (Graphics2D) _g;
            if (!initialized) {
                fontSmall = new Font("Palatino LinoStar", Font.BOLD, 12);
                fontNumbers = new Font("Courier New", 0, 12);
                fontRelations = new Font("Palatino LinoStar", 0, 11);
                fontBig = new Font("Palatino LinoStar", 0, 14);
                FontMetrics fm = g.getFontMetrics(fontNumbers);
                numbersDescent = fm.getDescent();
                Rectangle2D rect = fm.getStringBounds(Integer.toString(maxOffset), g);
                gridNumbersHeight = (int) rect.getWidth() + 2;
                numbersFontHeight = (int) fm.getHeight();

                fm = g.getFontMetrics(fontRelations);
                relationsDescent = fm.getDescent();

                fm = g.getFontMetrics(fontSmall);
                smallDescent = fm.getDescent();
                cellHeight = (int) fm.getHeight() + 4;
                cornerHeight = (int) (cellHeight * 0.35);

                fm = g.getFontMetrics(fontBig);
                bigDescent = fm.getDescent();
                Rectangle2D r = fm.getStringBounds("M", g);
                bigFontWidth = (int) r.getWidth();
                if (cellWidth != 0) {
                    cellWidth = bigFontWidth + bigFontWidth / 2;
                }
                additionAreaHeight = (int) r.getHeight() + 2;

                standartTransform = g.getTransform();
                rotatedTransform = new AffineTransform(standartTransform);
                rotatedTransform.rotate(-3.1415926 / 2);

                initialized = true;
                if (cellWidth == 0) {
                    fitToView();
                }
                cornerWidth = (int) (cellWidth * 0.15);
                componentResized(null);
                verScroll.setValue(verScroll.getMaximum() - 1);
                up2date = true;
                repaint();
            } else {
                if (!up2date) {
                    try {
                        fillGrid();
                    } catch (TreetonModelException e) {
                        e.printStackTrace();
                    }
                    up2date = true;
                }
                paintGrid(g);
            }
        }

        private class LabelInGrid {
            TreenotationLabel label;
            int startCellX;
            int endCellX;
            int Y;
            boolean top;

            LabelInGrid() {
                label = new TreenotationLabel(64);
                startCellX = -1;
                endCellX = -1;
                Y = -1;
                top = true;
            }

            void reset() {
                startCellX = -1;
                endCellX = -1;
                Y = -1;
                top = true;
            }
        }

        private class RelationInfoComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                RelationInfo r1 = (RelationInfo) o1;
                RelationInfo r2 = (RelationInfo) o2;
                if (r1.host == ROOT) {
                    if (r2.host == ROOT) {
                        return 0;
                    } else {
                        return 1;
                    }
                } else if (r2.host == ROOT) {
                    return -1;
                }
                return r1.getTokenLength() - r2.getTokenLength();
            }
        }

        private class RelComparator implements Comparator {
            int startCellX;
            int endCellX;

            public int compare(Object o1, Object o2) {
                RelationInGrid r1 = (RelationInGrid) o1;
                RelationInGrid r2 = (RelationInGrid) o2;

                boolean firstIsLeft;
                boolean secondIsLeft;

                if (r1.info.host == ROOT) {
                    if (r2.leftCellStartX == startCellX && r2.leftCellEndX == endCellX) {
                        return -1;
                    } else {
                        return 1;
                    }

                }
                if (r2.info.host == ROOT) {
                    if (r1.leftCellStartX == startCellX && r1.leftCellEndX == endCellX) {
                        return 1;
                    } else {
                        return -1;
                    }
                }

                if (r1.leftCellStartX == startCellX && r1.leftCellEndX == endCellX) {
                    firstIsLeft = true;
                } else {
                    firstIsLeft = false;
                }

                if (r2.leftCellStartX == startCellX && r2.leftCellEndX == endCellX) {
                    secondIsLeft = true;
                } else {
                    secondIsLeft = false;
                }

                if (firstIsLeft && !secondIsLeft) {
                    return 1;
                }

                if (!firstIsLeft && secondIsLeft) {
                    return -1;
                }


                if (firstIsLeft) { //both are left
                    return r2.topY - r1.topY;
                } else { //both are right
                    return r1.topY - r2.topY;
                }
            }

        }

        private class RelationInfo {
            Treenotation host;
            Treenotation slave;
            TrnRelationType type;

            void reset() {
                host = null;
                slave = null;
                type = null;
            }

            int getTokenLength() {
                Token min = host.getStartToken().compareTo(slave.getStartToken()) < 0 ? host.getStartToken() : slave.getStartToken();
                Token max = host.getEndToken().compareTo(slave.getEndToken()) < 0 ? slave.getEndToken() : host.getEndToken();
                int i = 1;
                while (min != max) {
                    min = min.getNextToken();
                    i++;
                }
                return i;
            }

            public void copyFrom(RelationInfo info) {
                this.host = info.host;
                this.slave = info.slave;
                this.type = info.type;
            }

        }

        private class RelationToSort {
            int startCellX;
            int endCellX;
            RelationInGrid[] relations = new RelationInGrid[10];
            int nRelations = 0;

            void addRelation(RelationInGrid rel) {
                if (nRelations == relations.length) {
                    RelationInGrid[] tarr = new RelationInGrid[relations.length * 3 / 2];
                    System.arraycopy(relations, 0, tarr, 0, relations.length);
                    relations = tarr;
                }
                relations[nRelations++] = rel;
            }


            void sortRelations() {
                relComparator.startCellX = startCellX;
                relComparator.endCellX = endCellX;
                Arrays.sort(relations, 0, nRelations, relComparator);
                double delta = (double) 1 / (double) (nRelations + 1);
                double k = delta;
                for (int i = 0; i < nRelations; i++, k += delta) {
                    RelationInGrid r = relations[i];
                    if (r.leftCellStartX == startCellX && r.leftCellEndX == endCellX) {
                        r.leftK = k;
                    } else {
                        r.rightK = k;
                    }
                }
            }
        }

        private class RelationInGrid {
            RelationInfo info;
            TreenotationLabel label;

            int leftCellStartX;
            int leftCellEndX;
            int rightCellStartX;
            int rightCellEndX;
            int leftCellY;
            int rightCellY;
            int topY;

            int arrow; //-1 - left, 0 - disable, 1 - right

            double leftK;
            double rightK;

            public RelationInGrid() {
                info = new RelationInfo();
                label = new TreenotationLabel(64);
                leftCellStartX = -1;
                leftCellEndX = -1;
                rightCellStartX = -1;
                rightCellEndX = -1;
                leftCellY = -1;
                rightCellY = -1;
                topY = -1;
                leftK = 0.5;
                rightK = 0.5;
            }

            void reset() {
                info.reset();
                leftCellStartX = -1;
                leftCellEndX = -1;
                rightCellStartX = -1;
                rightCellEndX = -1;
                leftCellY = -1;
                rightCellY = -1;
                topY = -1;
                leftK = 0.5;
                rightK = 0.5;
            }
        }

        public class Cell {
            Object trn;
            Treenotation parent;

            int background_r;
            int background_g;
            int background_b;

            boolean bgr_beg;
            boolean bgr_end;

            boolean ending;
            boolean beginning;
            boolean top;
            boolean bottom;

            Cell() {
                trn = null;
                parent = null;
                ending = false;
                beginning = false;
                top = false;
                bottom = false;
                background_r = -1;
                background_g = -1;
                background_b = -1;
                bgr_beg = false;
                bgr_end = false;
            }

            void reset() {
                trn = null;
                parent = null;
                ending = false;
                beginning = false;
                top = false;
                bottom = false;
                background_r = -1;
                background_g = -1;
                background_b = -1;
                bgr_beg = false;
                bgr_end = false;
            }
        }

        private class TokenCell extends Cell {
            ArrayList<Cell> cells; //{Cell}*
            int firstFree;

            TokenCell(int nCells) {
                super();
                cells = new ArrayList<Cell>(128);
                firstFree = -1;

                for (int i = 0; i < nCells; i++) {
                    cells.add(new Cell());
                }
            }

            void expand(int nCells) {
                if (nCells <= cells.size()) {
                    return;
                }
                int sz = cells.size();
                for (; nCells > sz; nCells--) {
                    cells.add(new Cell());
                }
            }
        }
    }

    public class ZoomButton extends JButton implements MouseListener {
        Point pnt;

        public void paint(Graphics g) {
            Dimension dim = this.getSize();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, dim.width, dim.height);
            g.setColor(Color.BLACK);
            g.drawLine(dim.width / 8, dim.height / 2, dim.width / 8 + dim.width / 4 - 1, dim.height / 2);
            g.drawLine(dim.width / 2 + dim.width / 8 - 1, dim.height / 2, dim.width / 2 + dim.width / 8 + dim.width / 4 - 1, dim.height / 2);
            g.drawLine(dim.width / 2 + dim.width / 4 - 1, dim.height / 2 - dim.width / 8, dim.width / 2 + dim.width / 4 - 1, dim.height / 2 + dim.width / 8);
            if (pnt != null) {
                g.setColor(Color.DARK_GRAY);
                if (pnt.x < dim.width / 2) { //Левая половина
                    g.fillRect(0, 0, dim.width / 2, dim.height);
                    g.setColor(Color.WHITE);
                    g.drawLine(dim.width / 8, dim.height / 2, dim.width / 8 + dim.width / 4 - 1, dim.height / 2);
                } else if (pnt.x > dim.width / 2) {
                    g.fillRect(dim.width / 2, 0, dim.width / 2, dim.height);
                    g.setColor(Color.WHITE);
                    g.drawLine(dim.width / 2 + dim.width / 8 - 1, dim.height / 2, dim.width / 2 + dim.width / 8 + dim.width / 4 - 1, dim.height / 2);
                    g.drawLine(dim.width / 2 + dim.width / 4 - 1, dim.height / 2 - dim.width / 8, dim.width / 2 + dim.width / 4 - 1, dim.height / 2 + dim.width / 8);
                }
            }
            g.setColor(Color.WHITE);
            g.drawRect(-1, -1, dim.width, dim.height);
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!isEnabled())
                return;
            pnt = e.getPoint();
            Dimension dim = this.getSize();
//      int r = (int)(Math.random()*document.length*2);

            if (pnt.x < dim.width / 2) { //Левая половина
/*        System.out.println("Cursor on " + r);
        setCursorPosition(r);*/
                if (dp.cellWidth > dp.bigFontWidth) {
                    dp.cellWidth -= dp.bigFontWidth / 2;
                    dp.cornerWidth = (int) (dp.cellWidth * 0.15);
                }
            } else if (pnt.x > dim.width / 2) {
/*        Fraction.assign(fakeFraction,(int)(Math.random()*document.length*2),(int)(Math.random()*3+1));
        System.out.println("Scroll to " + fakeFraction + " (about "+ fakeFraction.toInt()+")");
        scrollToPosition(fakeFraction);*/
                if (dp.cellWidth < dp.bigFontWidth * 50) {
                    dp.cellWidth += dp.bigFontWidth / 2;
                    dp.cornerWidth = (int) (dp.cellWidth * 0.15);
                }
            }
            componentResized(null);
            dp.repaint();
        }

        public void mouseReleased(MouseEvent e) {
            if (!isEnabled())
                return;
            pnt = null;
        }
    }

    public class GridButton extends JButton implements MouseListener {
        Point pnt;

        public void paint(Graphics g) {
            Dimension dim = this.getSize();

            if (pnt != null) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, dim.width, dim.height);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, dim.width, dim.height);
                g.setColor(Color.BLACK);
            }
            g.drawLine((int) Math.round((dim.getWidth() - 2) / 4 - 1) + 1, 1, (int) Math.round((dim.getWidth() - 2) / 4 - 1) + 1, dim.height - 2);
            g.drawLine((int) Math.round((dim.getWidth() - 2) / 2 - 1) + 1, 1, (int) Math.round((dim.getWidth() - 2) / 2 - 1) + 1, dim.height - 2);
            g.drawLine((int) Math.ceil((dim.getWidth() - 2) * 0.75 - 1) + 1, 1, (int) Math.ceil((dim.getWidth() - 2) * 0.75 - 1) + 1, dim.height - 2);

            g.drawLine(1, dim.height / 3, dim.width - 2, dim.height / 3);
            g.drawLine(1, (dim.height / 3) * 2, dim.width - 2, (dim.height / 3) * 2);

            /*g.setColor(Color.WHITE);
            g.drawRect(-1,-1,dim.width,dim.height);*/
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!isEnabled())
                return;
            pnt = e.getPoint();
            if (gridIsOn) {
                dp.additionAreaHeight -= dp.gridNumbersHeight;
                gridIsOn = false;
            } else {
                dp.additionAreaHeight += dp.gridNumbersHeight;
                gridIsOn = true;
            }

            componentResized(null);
            dp.repaint();
        }

        public void mouseReleased(MouseEvent e) {
            if (!isEnabled())
                return;
            pnt = null;
        }
    }
}

