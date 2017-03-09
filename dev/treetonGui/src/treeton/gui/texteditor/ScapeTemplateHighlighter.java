/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.texteditor;

import treeton.core.Token;
import treeton.core.TreenotationStorage;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.gui.util.DialogHandler;
import treeton.scape.ScapeOutputItem;
import treeton.scape.ScapePhase;
import treeton.scape.ScapeResult;
import treeton.scape.ScapeRule;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class ScapeTemplateHighlighter {
    private static final Highlighter.HighlightPainter painter1 = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private final JTextPane textPane;
    private final TreenotationStorage storage;

    public ScapeTemplateHighlighter(final JTextPane textPane, final TreenotationStorage storage) {
        this.textPane = textPane;
        this.storage = storage;
    }

    public static ScapePhase buildScapePhase(String template, TrnTypeStorage types) throws ResourceInstantiationException, TreetonModelException, IOException, ParseException {
        ScapePhase phase = new ScapePhase(types);

        ScapeRule rule = new ScapeRule();
        String s = "rule predMorph {" + template + "->}";
        char[] f = s.toCharArray();
        rule.readIn(f, 0, f.length - 1, phase);

        phase.build(Arrays.asList(rule).iterator(), "PredMorphByTemplate",
                ScapePhase.CONTROL_TYPE_APPELT,
                types.getAllTypes(),
                new ScapeOutputItem[]{}
        );
        phase.initialize();
        return phase;
    }

    public static void highlight(String sourceText, TreenotationStorage storage, JTextPane textpane) {
        String template = JOptionPane.showInputDialog("Scape template: ");
        if (template == null)
            return;
        ScapePhase phase = null;
        try {
            phase = buildScapePhase(template, storage.getTypes());
        } catch (Exception e) {
            e.printStackTrace();
            DialogHandler.getInstance().error("Couldn't parse scape template: " + template, e);
            return;
        }
        phase.reset(storage);

        Highlighter highlighter = textpane.getHighlighter();
        if (!(highlighter instanceof UnderlineHighlighter))
            textpane.setHighlighter(highlighter = new UnderlineHighlighter(Color.RED));

        boolean highlighted = highlighter.getHighlights().length > 0;

        while (phase.nextStartPoint((Token) null) != null) {
            ScapeResult r;
            while (null != (r = phase.nextResult())) {
                int start = r.getStartToken().getStartNumerator();
//                start = recountPos(start, sourceText); // TODO: do we need recountPos and when?
                int end = r.getEndToken().getEndNumerator();
//                end = recountPos(end,sourceText);

                try {
                    if (highlighted)
                        highlighter.addHighlight(start, end, painter1);
                    else
                        ((UnderlineHighlighter) highlighter).addHighlight(start, end);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                    DialogHandler.getInstance().error(e);
                }
            }
        }
    }

    public static int recountPos(int sourcePos, String source) {
        if (sourcePos >= source.length() + 1)
            throw new ArrayIndexOutOfBoundsException("No character " + sourcePos + " in string of length " + source.length());

        int res = sourcePos;
        for (int i = 0; i < sourcePos - 1; i++) {
            if (source.charAt(i) == '\r')
                res--;
        }
        return res;
    }

    public static void removeHighlight(JTextPane textPane) {
        textPane.getHighlighter().removeAllHighlights();
    }
}
