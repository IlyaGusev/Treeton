/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusException;
import treeton.prosody.metricindex.MetricIndex;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;

public class CorpusTweakerPanel extends JPanel {
    private final Corpus corpus;
    private JTextField grammarPathField;

    public CorpusTweakerPanel(Corpus corpus) {
        this.corpus = corpus;
        init();
    }

    private void init(){
        Box toolbar = Box.createHorizontalBox();

        JLabel label = new JLabel("Mdl meter analyzer grammar path: ");
        toolbar.add(label);

        String currentPath = corpus.getGlobalProperty("mdlMeterAnalyzerGrammarPath");
        if( currentPath == null ) {
            grammarPathField = new JTextField();
        } else {
            grammarPathField = new JTextField(currentPath);
        }
        grammarPathField.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
            try {
                corpus.setGlobalProperty( MetricIndex.MDL_ANLYZER_GRAMMAR_PATH_PROPERTY_NAME, grammarPathField.getText() );
            } catch (CorpusException e1) {
                e1.printStackTrace();
            }
            }
        });
        grammarPathField.setPreferredSize(new Dimension(150, grammarPathField.getPreferredSize().height));

        toolbar.add(grammarPathField);
        this.add(toolbar);
    }
}
