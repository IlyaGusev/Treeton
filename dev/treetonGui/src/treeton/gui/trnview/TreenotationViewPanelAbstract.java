/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Fraction;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.gui.labelgen.TrnLabelGenerator;

import javax.swing.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

public abstract class TreenotationViewPanelAbstract extends JPanel implements ComponentListener {
    public TreenotationViewPanelAbstract() {
        super();
    }

    // todo: make TreenotationViewPanel() package-local (do something with getToolTipText() overriders)!
    public static TreenotationViewPanelAbstract createTreenotationViewPanel(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        return new ScalableTreenotationViewPanel(tp, _from, _to, _focus, trns, doc, _curPos, labelGenerators);
    }

    public abstract double getFocus();

    public abstract void selectTrn(Treenotation trn);

    public abstract void reset(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos, HashMap<String, TrnLabelGenerator> labelGenerators);

    public abstract void componentHidden(ComponentEvent e);

    public abstract void componentMoved(ComponentEvent e);

    public abstract void componentResized(ComponentEvent e);

    public abstract void recountHorScroll();

    public abstract void recountVerScroll();

    public abstract void componentShown(ComponentEvent e);

    public abstract boolean isCursorVisible();

    public abstract void setCursorVisible(boolean visible);

    public abstract void scrollToPosition(Fraction pos) throws TreetonModelException;

    public abstract void scrollToPosition(double position) throws TreetonModelException;

    public abstract void scrollToPosition(Token token) throws TreetonModelException;

    public abstract void fitToView();

    public abstract int getCursorPosition();

    public abstract void setCursorPosition(int _curPos);

    public abstract void addTrnManipulationListener(TrnManipulationListener l);

    public abstract void removeTrnManipulationListener(TrnManipulationListener l);

    public abstract String getDocument();

    public abstract void hide(Treenotation trn);

    public abstract void hideAll(TrnType type);

    public abstract void showAll(TrnType type);

    public abstract void showAll();

    public abstract void show(Treenotation trn);

    public abstract boolean isTrnShown(Treenotation trn);

    public abstract void selectNone();

    public abstract Object getSelectedObject(MouseEvent e);

    public abstract int getSelectedIntervalStart();

    public abstract int getSelectedIntervalEnd();

    public abstract void addAlwaysShownTrn(Treenotation trn);

    public abstract void removeAlwaysShownTrn(Treenotation trn);

    public abstract void removeAlwaysShownTrns();

    public abstract List<Treenotation> getAlwaysShownTrns();
}
