/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.gui.trnedit.IntFeatureMapEditorDialog;
import treeton.gui.trnedit.IntFeatureMapEditorListener;
import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.corpus.CorpusException;
import treeton.prosody.corpus.CorpusFolder;
import treeton.prosody.corpus.CorpusListener;
import treeton.prosody.metricindex.MetricIndex;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

public class ProsodyCorpusEntryEditor extends JPanel implements CorpusListener, ActionListener {
    private DefaultHighlighter highlighter;
    private JButton saveButton;
    private JButton revertButton;
    private JButton editMetainfoButton;
    private JButton makeAmbigiousUnstressedButton;
    private JButton makeAmbigiousStressedButton;
    private JComboBox<String> metersCombo;
    private JComboBox<String> footnessCombo;

    private CorpusEntry corpusEntry;

    public void setMetricIndex(MetricIndex metricIndex) {
        this.metricIndex = metricIndex;
    }

    private MetricIndex metricIndex;
    private TrnType ignoredTextType;
    private TrnType verseType;
    private TrnType phonWordType;
    private TrnType syllableType;
    private TrnType accVariantType;
    private int userVariantFeatureId;
    private TrnType corpusElementType;
    private AttributeSet boldGreen;
    private AttributeSet simpleGreen;
    private AttributeSet boldRed;
    private AttributeSet boldBlack;
    private Treenotation currentSyllable;
    private int currentCaretPosition;

    private ArrayList<Object> currentMetricHighlights = new ArrayList<Object>();
    private Object currentSyllableHighlight;
    private Object currentPhonWordHighlight;
    private Object currentVerseHighlight;
    private JPanel foldersInfoPanel;
    private JLabel label;
    private Icon folderIcon = new DefaultTreeCellRenderer().getDefaultClosedIcon();

    private String SAVE_COMMAND = "Save changes (Ctrl-S)";
    private String REVERT_COMMAND = "Revert changes";
    private String EDIT_METAINFO_COMMAND = "Edit meta information";
    private String MAKE_ALL_AMBIGIOUS_UNSTRESSED = "Make all ambigious syllables unstressed";
    private String MAKE_ALL_AMBIGIOUS_STRESSED = "Make all ambigious syllables stressed";

    private JTextPane textPane = new JTextPane() {
        @Override
        public String getToolTipText(MouseEvent event) {
            if( corpusEntry == null ) {
                return null;
            }

            int dot = textPane.viewToModel(event.getPoint());
            if( dot == -1 ) {
                return null;
            }

            TreenotationStorageImpl metadata = corpusEntry.getMetadata();

            TypeIteratorInterface iterator = metadata.sortedTypeIterator(new TrnType[] {verseType}, metadata.firstToken(), metadata.lastToken() );

            Integer verseNumber = null;
            int i = 0;
            while( iterator.hasNext() ) {
                Treenotation trn = (Treenotation) iterator.next();

                if( trn.getStartNumerator() <= dot && dot < trn.getEndNumerator() ) {
                    iterator.close();
                    verseNumber = i;
                    break;
                }

                i++;
            }

            if( verseNumber == null ) {
                return null;
            }

            return getMetricInfo(verseNumber, corpusEntry);
        }
    };

    public String getMetricInfo(Integer verseNumber, CorpusEntry entry ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if( verseNumber != null ) {
            sb.append("<b>Метрическая информация для строки</b>:<br><br>");
        } else {
            sb.append("<b>Метрическая информация для всего текста</b>:<br><br>");
        }

        Collection<String> meters = metricIndex.getMeters();
        for (String meter : meters) {
            sb.append("<b>").append(meter).append("</b>");
            int maxFootCount = metricIndex.getMaxFootCount(entry, meter);

            if( verseNumber != null ) {
                sb.append("<b>:</b> ");

                int startLength = sb.length();
                for (int j = 1; j <= maxFootCount; j++) {
                    double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, j);

                    if (sb.length() > startLength) {
                        sb.append(", ");
                    }

                    if (probabilities == null || probabilities.length == 0 ) {
                        sb.append("-");
                    } else {
                        assert verseNumber < probabilities.length;

                        sb.append(String.format("%.2f", probabilities[verseNumber]));
                    }
                }
            } else {
                sb.append(" <b>(max):</b> ");

                int startLength = sb.length();
                for (int j = 1; j <= maxFootCount; j++) {
                    double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, j);

                    if (sb.length() > startLength) {
                        sb.append(", ");
                    }

                    if (probabilities == null || probabilities.length == 0 ) {
                        sb.append("-");
                    } else {
                        double max = 0;
                        for (double probability : probabilities) {
                            max = Math.max(max, probability);
                        }
                        sb.append(String.format("%.2f", max ));
                    }
                }
                sb.append("<br><b>").append(meter).append(" (avg):</b> ");

                startLength = sb.length();
                for (int j = 1; j <= maxFootCount; j++) {
                    double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, j);

                    if (sb.length() > startLength) {
                        sb.append(", ");
                    }

                    if (probabilities == null || probabilities.length == 0) {
                        sb.append("-");
                    } else {
                        double avg = 0;
                        for (double probability : probabilities) {
                            avg += probability;
                        }

                        avg /= probabilities.length;
                        sb.append(String.format("%.2f", avg ));
                    }
                }

            }

            sb.append("<br>");
        }
        sb.append("</html>");

        return sb.toString();
    }


    public void openEntry(CorpusEntry entry ) {
        corpusEntry = entry;

        if( corpusEntry == null ) {
            highlighter = null;
            textPane.setText("");
            textPane.setEditable(false);
            textPane.setEnabled(false);
            textPane.setHighlighter(null);
            currentSyllable = null;
            currentCaretPosition = -1;
            currentSyllableHighlight = null;
            currentPhonWordHighlight = null;
            currentVerseHighlight = null;
            currentMetricHighlights.clear();
            label.setText("Элемент корпуса не выбран");
            updateFoldersInfo();
            editMetainfoButton.setEnabled(false);
            makeAmbigiousUnstressedButton.setEnabled(false);
            makeAmbigiousStressedButton.setEnabled(false);
            saveButton.setEnabled(false);
            revertButton.setEnabled(false);
            return;
        }

        label.setText(entry.getLabel());
        textPane.setEnabled(true);
        textPane.setText(entry.getText().replaceAll("\r\n", " \n"));
        textPane.setEditable(false);
        highlighter = new DefaultHighlighter();
        currentMetricHighlights.clear();
        currentSyllableHighlight = null;
        currentPhonWordHighlight = null;
        currentVerseHighlight = null;

        metersCombo.removeAllItems();
        metersCombo.addItem("");
        for (String meter : metricIndex.getMeters()) {
            metersCombo.addItem(meter);
        }
        metersCombo.setSelectedItem("");
        footnessCombo.removeAllItems();
        footnessCombo.addItem("");

        textPane.setHighlighter(highlighter);
        createHighlights();
        updateFoldersInfo();
        showAccents();
        editMetainfoButton.setEnabled(true);
        makeAmbigiousUnstressedButton.setEnabled(true);
        makeAmbigiousStressedButton.setEnabled(true);
        saveButton.setEnabled(false);
        revertButton.setEnabled(false);
    }

    private void changeUserStress( Treenotation syllable, boolean stressed ) {
        assert syllable != null;
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(accVariantType, syllable.getStartToken(), syllable.getEndToken());

        while( iterator.hasNext() ) {
            TreenotationImpl accVariant = (TreenotationImpl) iterator.next();
            if (Boolean.TRUE.equals(accVariant.get(userVariantFeatureId))) {
                iterator.close();
                metadata.remove(accVariant);
                break;
            }
        }

        TreenotationImpl accVariant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(metadata, syllable.getStartToken(),
                syllable.getEndToken(),accVariantType);
        accVariant.put(userVariantFeatureId,Boolean.TRUE);
        accVariant.addTree( new TreenotationImpl.Node(
                stressed ? TreenotationImpl.PARENT_CONNECTION_STRONG : TreenotationImpl.PARENT_CONNECTION_WEAK,
                (TreenotationImpl) syllable) );
        metadata.add(accVariant);

        saveButton.setEnabled(true);
        revertButton.setEnabled(true);
    }

    private Treenotation findCorpusElementTrn() {
        if( corpusEntry == null ) {
            return null;
        }

        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(corpusElementType);
        assert iterator.hasNext();

        Treenotation corpusElementTrn = (Treenotation) iterator.next();

        if( iterator.hasNext() ) {
            iterator.close();
            assert false;
        }

        return corpusElementTrn;
    }

    private Treenotation getVerse( Treenotation syllable ) {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();

        TypeIteratorInterface iterator = metadata.typeIterator(verseType, syllable.getStartToken(), syllable.getEndToken() );

        if( iterator.hasNext() ) {
            Treenotation trn = (Treenotation) iterator.next();
            assert !iterator.hasNext();

            return trn;
        }

        return null;
    }

    private Treenotation getSyllable(int dot) {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        Token tok = metadata.getTokenByOffset(dot, 1, true);
        if( tok == null ) {
            return null;
        }

        TypeIteratorInterface iterator = metadata.typeIterator(syllableType, tok, tok);

        if( iterator.hasNext() ) {
            Treenotation trn = (Treenotation) iterator.next();
            assert !iterator.hasNext();

            return trn;
        }

        return null;
    }

    private Treenotation getPhonWordBySyllable(Treenotation syllable) {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();

        TypeIteratorInterface iterator = metadata.typeIterator(phonWordType, syllable.getStartToken(), syllable.getEndToken());

        if( iterator.hasNext() ) {
            Treenotation trn = (Treenotation) iterator.next();
            assert !iterator.hasNext();

            return trn;
        }

        return null;
    }

    private void showAccents() {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        textPane.getStyledDocument().setCharacterAttributes(0,textPane.getStyledDocument().getLength(),
                SimpleAttributeSet.EMPTY,true);

        TypeIteratorInterface iterator = metadata.typeIterator(syllableType);

        while (iterator.hasNext()) {
            Treenotation syll = (Treenotation) iterator.next();

            if( mustBeIgnored( syll ) ) {
                continue;
            }

            int accentStatus = getSyllableStatus( syll );

            AttributeSet style;

            if( ( accentStatus & USER_STRESS ) > 0 ) {
                if( ( accentStatus & STRESSED ) > 0 ) {
                    style = simpleGreen;
                } else {
                    continue;
                }
            } else {
                if( ( accentStatus & STRESSED ) > 0 ) {
                    style = boldGreen;
                } else if( ( accentStatus & AMBIGIOUS ) > 0 ) {
                    style = boldRed;
                } else {
                    style = boldBlack;
                }
            }

            assert syll.getStartDenominator() == 1 && syll.getEndDenominator() == 1;

            textPane.getStyledDocument().setCharacterAttributes(syll.getStartNumerator(),
                    syll.getEndNumerator() - syll.getStartNumerator(), style, true);
        }
    }

    private boolean mustBeIgnored(Treenotation syllable) {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(ignoredTextType, syllable.getStartToken(), syllable.getEndToken());

        if( iterator.hasNext() ) {
            iterator.close();
            return true;
        }

        return false;
    }

    private static int STRESSED = 1;
    private static int AMBIGIOUS = 2;
    private static int USER_STRESS = 4;

    private int getSyllableStatus(Treenotation syllable) {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(accVariantType, syllable.getStartToken(), syllable.getEndToken());

        int status = -2;

        while( iterator.hasNext() ) {
            TreenotationImpl accVariant = (TreenotationImpl) iterator.next();
            boolean isUser = Boolean.TRUE.equals(accVariant.get(userVariantFeatureId));

            TreenotationImpl.Node[] syllables = accVariant.getTrees();

            for (TreenotationImpl.Node syllableNode : syllables) {
                if( syllableNode.getTrn() != syllable ) {
                    continue;
                }

                if( isUser ) {
                    iterator.close();
                    if( syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG ) {
                        return STRESSED | USER_STRESS;
                    } else {
                        return USER_STRESS;
                    }

                }

                if( syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG ) {
                    if( status == -2 ) {
                        status = 1;
                    } else if( status == 0 ) {
                        status = -1;
                    }
                } else {
                    if( status == -2 ) {
                        status = 0;
                    } else if( status == 1 ) {
                        status = -1;
                    }
                }
            }
        }

        return status == -2 ? 0 : ( status == 1 ? STRESSED : ( status == -1 ? AMBIGIOUS : 0 ) );
    }

    private void createHighlights() {
        TreenotationStorageImpl metadata = corpusEntry.getMetadata();

        TypeIteratorInterface iterator = metadata.typeIterator(ignoredTextType);

        while (iterator.hasNext()) {
            Treenotation ignoredText = (Treenotation) iterator.next();

            assert ignoredText.getStartDenominator() == 1 && ignoredText.getEndDenominator() == 1;

            try {
                highlighter.addHighlight(ignoredText.getStartNumerator(), ignoredText.getEndNumerator(),
                        new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY));
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateMetricHighlights() {
        for (Object metricHighlight : currentMetricHighlights) {
            highlighter.removeHighlight(metricHighlight);
        }

        currentMetricHighlights.clear();

        String meter = (String) metersCombo.getSelectedItem();
        if( meter == null || meter.isEmpty() ) {
            return;
        }

        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(verseType);

        int maxFootCount = metricIndex.getMaxFootCount(corpusEntry,meter);

        double[] sumProbByFootCount = new double[maxFootCount+1];
        double maxProb = 0.0;
        int maxProbFootness = -1;

        for( int i = 0; i <= maxFootCount; i++ ) {
            double[] probsForFootCount = metricIndex.getMeterProbabilities(corpusEntry,meter,i);
            sumProbByFootCount[i] = 0.0;
            if( probsForFootCount == null ) {
                continue;
            }

            for (double v : probsForFootCount) {
                sumProbByFootCount[i] += v;
            }

            if( sumProbByFootCount[i] > maxProb ) {
                maxProb = sumProbByFootCount[i];
                maxProbFootness = i;
            }
        }

        if( maxProbFootness == -1 ) {
            return;
        }

        double[] probs = metricIndex.getMeterProbabilities(corpusEntry,meter,maxProbFootness);

        int verseIndex = -1;
        while (iterator.hasNext()) {
            Treenotation verse = (Treenotation) iterator.next();
            verseIndex++;

            assert verseIndex >= 0 && verseIndex < probs.length;

            double prob = probs[verseIndex];
            Color color = new Color( 1, (float) prob, (float) prob, (float) 0.3);

            assert verse.getStartDenominator() == 1 && verse.getEndDenominator() == 1;

            try {
                Object highlight = highlighter.addHighlight(verse.getStartNumerator(), verse.getEndNumerator(),
                        new DefaultHighlighter.DefaultHighlightPainter(color));
                currentMetricHighlights.add(highlight);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }


    public ProsodyCorpusEntryEditor(TreenotationsContext trnContext ) throws TreetonModelException, MalformedURLException, ContextException {
        ignoredTextType = trnContext.getType("IgnoredText");
        verseType = trnContext.getType("Verse");
        phonWordType = trnContext.getType("PhonWord");
        syllableType = trnContext.getType("Syllable");
        accVariantType = trnContext.getType("AccVariant");
        userVariantFeatureId = accVariantType.getFeatureIndex("userVariant");
        corpusElementType = trnContext.getType("CorpusElement");
        ToolTipManager.sharedInstance().registerComponent(textPane);
        initGui();
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentCaretPosition = -1;
            }
        });
        textPane.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (highlighter == null) {
                    return;
                }

                if (currentCaretPosition == e.getDot()) {
                    return;
                }

                if( currentSyllable == null ) {
                    currentCaretPosition = -1;
                }

                Treenotation syllable = null;

                if( currentCaretPosition == -1 ) {
                    currentCaretPosition = e.getDot();
                    syllable = getSyllable(currentCaretPosition);
                } else if (currentCaretPosition < e.getDot()) {
                    int i = e.getDot();
                    for (; i < textPane.getText().length(); i++) {
                        Treenotation syl = getSyllable(i);

                        if (syl != null && syl != currentSyllable) {
                            syllable = syl;
                            break;
                        }
                    }

                    currentCaretPosition = i;
                    textPane.setCaretPosition(currentCaretPosition);
                } else {
                    int i = e.getDot();
                    for (; i > 0; i--) {
                        Treenotation syl = getSyllable(i);

                        if (syl != null && !mustBeIgnored(syl) && syl != currentSyllable) {
                            syllable = syl;
                            break;
                        }
                    }
                    currentCaretPosition = i;
                    textPane.setCaretPosition(currentCaretPosition);
                }

                if (currentSyllable == null) {
                    if (syllable != null && !mustBeIgnored(syllable)) {
                        assert syllable.getStartDenominator() == 1 && syllable.getEndDenominator() == 1;

                        try {
                            currentSyllableHighlight = highlighter.addHighlight(syllable.getStartNumerator(), syllable.getEndNumerator(),
                                    new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE));
                            Treenotation phonWord = getPhonWordBySyllable( syllable );
                            if( phonWord != null ) {
                                currentPhonWordHighlight = highlighter.addHighlight(phonWord.getStartNumerator(), phonWord.getEndNumerator(),
                                        new DefaultHighlighter.DefaultHighlightPainter(new Color((float) 1, (float) 0.8, (float) 0, (float) 0.5)));
                            }
                            Treenotation verse = getVerse( syllable );
                            if( verse != null ) {
                                currentVerseHighlight = highlighter.addHighlight(verse.getStartNumerator(), verse.getEndNumerator(),
                                        new DefaultHighlighter.DefaultHighlightPainter(new Color((float) 0.5, (float) 0.5, (float) 0.5, (float) 0.3)));
                            }
                            currentSyllable = syllable;
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else if (syllable == null || mustBeIgnored(syllable)) {
                    highlighter.removeHighlight(currentSyllableHighlight);
                    if( currentPhonWordHighlight != null ) {
                        highlighter.removeHighlight(currentPhonWordHighlight);
                    }
                    if( currentVerseHighlight != null ) {
                        highlighter.removeHighlight(currentVerseHighlight);
                    }
                    currentSyllableHighlight = null;
                    currentPhonWordHighlight = null;
                    currentVerseHighlight = null;
                    currentSyllable = null;
                } else if (currentSyllable != syllable) {
                    assert syllable.getStartDenominator() == 1 && syllable.getEndDenominator() == 1;

                    try {
                        highlighter.removeHighlight(currentSyllableHighlight);
                        if( currentPhonWordHighlight != null ) {
                            highlighter.removeHighlight(currentPhonWordHighlight);
                        }
                        if( currentVerseHighlight != null ) {
                            highlighter.removeHighlight(currentVerseHighlight);
                        }
                        currentSyllableHighlight = highlighter.addHighlight(syllable.getStartNumerator(), syllable.getEndNumerator(),
                                new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE));
                        Treenotation phonWord = getPhonWordBySyllable( syllable );
                        if( phonWord != null ) {
                            currentPhonWordHighlight = highlighter.addHighlight(phonWord.getStartNumerator(), phonWord.getEndNumerator(),
                                    new DefaultHighlighter.DefaultHighlightPainter(new Color((float) 1, (float) 0.8, (float) 0, (float) 0.5)));
                        }
                        Treenotation verse = getVerse( syllable );
                        if( verse != null ) {
                            currentVerseHighlight = highlighter.addHighlight(verse.getStartNumerator(), verse.getEndNumerator(),
                                    new DefaultHighlighter.DefaultHighlightPainter(new Color((float) 0.5, (float) 0.5, (float) 0.5, (float) 0.3)));
                        }
                        currentSyllable = syllable;
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    if (currentSyllable != null) {
                        int currentStatus = getSyllableStatus(currentSyllable);
                        boolean stressed = (currentStatus & STRESSED) > 0;
                        changeUserStress(currentSyllable, !stressed);
                        showAccents();
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if( e.getKeyCode() == KeyEvent.VK_S && ( e.getModifiers() & KeyEvent.CTRL_MASK ) > 0 ) {
                    doSave();
                }
            }
        });

        openEntry(null);
    }


    private void initGui() throws MalformedURLException {
        Font f = textPane.getFont();
        Font newFont = new Font( f.getName(), f.getStyle(), f.getSize() + 4 );
        textPane.setFont( newFont);
        StyleContext sc = StyleContext.getDefaultStyleContext();
        boldRed = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.RED);
        boldRed = sc.addAttribute(boldRed,StyleConstants.Bold,Boolean.TRUE);
        boldGreen = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.GREEN.darker());
        boldGreen = sc.addAttribute(boldGreen,StyleConstants.Bold,Boolean.TRUE);
        boldBlack = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
        boldBlack = sc.addAttribute(boldBlack,StyleConstants.Bold,Boolean.TRUE);
        simpleGreen = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.GREEN.darker());

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        /* Кнопки */

        JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setRollover(true);
        saveButton = GuiResources.tbf.addToolBarButton(toolbar, "save.gif", SAVE_COMMAND, SAVE_COMMAND, this);
        saveButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), SAVE_COMMAND);
        toolbar.add(saveButton);
        revertButton = GuiResources.tbf.addToolBarButton(toolbar, "undo.gif", REVERT_COMMAND, REVERT_COMMAND, this);
        toolbar.add(revertButton);
        editMetainfoButton = GuiResources.tbf.addToolBarButton(toolbar, "info12.gif", EDIT_METAINFO_COMMAND, EDIT_METAINFO_COMMAND, this);
        toolbar.add(editMetainfoButton);
        makeAmbigiousUnstressedButton = GuiResources.tbf.addToolBarButton(toolbar, "down.gif", MAKE_ALL_AMBIGIOUS_UNSTRESSED, MAKE_ALL_AMBIGIOUS_UNSTRESSED, this);
        toolbar.add(makeAmbigiousUnstressedButton);
        makeAmbigiousStressedButton = GuiResources.tbf.addToolBarButton(toolbar, "up.gif", MAKE_ALL_AMBIGIOUS_STRESSED, MAKE_ALL_AMBIGIOUS_STRESSED, this);
        toolbar.add(makeAmbigiousStressedButton);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Metric template:"));
        metersCombo = new JComboBox<String>();
        metersCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateMetricHighlights();
            }
        });
        toolbar.add(metersCombo);
        toolbar.add(new JLabel("Foot count:"));
        footnessCombo = new JComboBox<String>();
        footnessCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateMetricHighlights();
            }
        });
        toolbar.add(footnessCombo);
        toolbar.setBorder(null);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth  = 1;
        c.weightx = 1;
        c.weighty = 0;

        layout.setConstraints(toolbar, c);
        this.add(toolbar);

        /* Заголовок */
        label = new JLabel();
        label.setBackground(Color.WHITE);
        label.setFont(new Font("Tahoma", Font.PLAIN, 16));
        label.setText("Элемент корпуса не выбран");




        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill   = GridBagConstraints.NONE;
        c.gridheight = 1;
        c.gridwidth  = 1;
        c.weightx = 0;
        c.weighty = 0;

        layout.setConstraints(label, c);
        this.add(label);

        /* Область с папками тэгами */

        foldersInfoPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        foldersInfoPanel.setLayout(flowLayout);
        updateFoldersInfo();


        c.gridheight = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        c.weightx = 1;
        layout.setConstraints(foldersInfoPanel, c);
        this.add(foldersInfoPanel);

        /* Область текста */
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setMinimumSize(new Dimension(100, 50));
        scrollPane.setPreferredSize(new Dimension(200, 300));

        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.BOTH;
        layout.setConstraints(scrollPane, c);
        this.add(scrollPane);
    }

    private void updateFoldersInfo() {
        foldersInfoPanel.removeAll();

        if( corpusEntry != null ) {
            for (CorpusFolder folder : corpusEntry.getParentFolders()) {
                JLabel jLabel = new JLabel(folder.getLabel());
                if( folderIcon != null ) {
                    jLabel.setIcon(folderIcon);
                }
                jLabel.setOpaque(true);
                LineBorder border = new LineBorder(Color.LIGHT_GRAY,2,true);
                jLabel.setBorder(border);
                jLabel.setBackground(Color.LIGHT_GRAY.brighter());
                foldersInfoPanel.add(jLabel);
                jLabel.revalidate();
            }
        }
    }

    @Override
    public void entryCreated(CorpusEntry entry) {
    }

    @Override
    public void entryDeleted(CorpusEntry entry, Collection<CorpusFolder> parentFolders ) {
        if( entry == corpusEntry ) {
            openEntry( null );
        }
    }

    @Override
    public void entryNameChanged(CorpusEntry entry) {
        if( entry == corpusEntry ) {
            label.setText(corpusEntry.getLabel());
        }
    }

    @Override
    public void entryTextChanged(CorpusEntry entry) {
        // Никогда не должен вызываться в обозримом будущем, но на всякий случай полная перезагрузка

        if( entry == corpusEntry ) {
            openEntry(null);
            openEntry(entry);
        }
    }

    @Override
    public void entryMetadataManuallyEdited(CorpusEntry entry) {
        // Это может инциироваться только самим редактором. Пока считаем, что в один момент времени функционирует не
        // более одного редактора и ничего не делаем.
    }

    @Override
    public void entryMetadataReloaded(CorpusEntry entry) {
        // Будет вывзываться когда сделаю лингвоакцентный анализ из gui. Ситуация редкая - делаем полную
        // перезагрузку

        if( entry == corpusEntry ) {
            openEntry(entry);
        }

    }

    @Override
    public void folderCreated(CorpusFolder folder) {
    }

    @Override
    public void folderNameChanged(CorpusFolder folder) {
        if( corpusEntry != null && corpusEntry.getParentFolders().contains(folder)) {
            updateFoldersInfo();
        }
    }

    @Override
    public void folderParentChanged(CorpusFolder folder, CorpusFolder oldParent) {
    }

    @Override
    public void entryWasPlacedIntoFolder(CorpusEntry entry, CorpusFolder folder) {
        if( entry == corpusEntry ) {
            updateFoldersInfo();
        }
    }

    @Override
    public void entryWasRemovedFromFolder(CorpusEntry entry, CorpusFolder folder) {
        if( entry == corpusEntry ) {
            updateFoldersInfo();
        }
    }

    @Override
    public void folderDeleted(CorpusFolder folder) {
        // ничего не делаем, т.к. нас сначала оповестят об удалении элемента из папки
    }

    @Override
    public void corpusLabelChanged() {
    }

    @Override
    public void globalCorpusPropertyChanged(String propertyName) {
        // TODO Если свойство MetricIndex.MDL_ANLYZER_GRAMMAR_PATH_PROPERTY_NAME поменялось
        // TODO запускать метрический анализ, обновлять подсветку, если выбранный метр еще присутствует.
        // TODO если выбранный метр пропал, опустошать комбобокс и гасить подсветку
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if(EDIT_METAINFO_COMMAND.equals(cmd)) {
            Treenotation trn = findCorpusElementTrn();
            if( trn != null ) {
                IntFeatureMapEditorDialog dialog = new IntFeatureMapEditorDialog("Метаинформация элемента корпуса",trn,new IntFeatureMapEditorListener() {
                    private BlackBoard board = TreetonFactory.newBlackBoard(100,false);
                    @Override
                    public void imapEdited(IntFeatureMap source, IntFeatureMap attrs, IntFeatureMap inducedAttrs) {
                        source.removeAll();
                        attrs.fillBlackBoard(board);
                        source.put(board);
                    }
                });

                dialog.showDialog(JOptionPane.getFrameForComponent(ProsodyCorpusEntryEditor.this));
                saveButton.setEnabled(true);
                revertButton.setEnabled(true);
            }
        } else if(MAKE_ALL_AMBIGIOUS_UNSTRESSED.equals(cmd)) {
            doUnambiguate(false);
        } else if(MAKE_ALL_AMBIGIOUS_STRESSED.equals(cmd)) {
            doUnambiguate(true);
        } else if(SAVE_COMMAND.equals(cmd)) {
            doSave();
        } else if(REVERT_COMMAND.equals(cmd)) {
            doRevert();
        }
    }

    private void doRevert() {
        try {
            corpusEntry.getCorpus().reloadEntry(corpusEntry);
        } catch (CorpusException e) {
            e.printStackTrace();
        }
    }

    public void doSave() {
        if( !saveButton.isEnabled() ) {
            return;
        }

        if( corpusEntry == null ) {
            return;
        }

        try {
            corpusEntry.getCorpus().metadataWasManuallyEdited(corpusEntry);
        } catch (CorpusException e1) {
            e1.printStackTrace();
        }

        saveButton.setEnabled(false);
        revertButton.setEnabled(false);

        updateMetricHighlights();
    }

    private void doUnambiguate( boolean stressed ) {
        if( corpusEntry == null ) {
            return;
        }

        TreenotationStorageImpl metadata = corpusEntry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(syllableType);

        while (iterator.hasNext()) {
            Treenotation syll = (Treenotation) iterator.next();

            if (mustBeIgnored(syll)) {
                continue;
            }

            int acccentStatus = getSyllableStatus( syll );

            if( ( acccentStatus & AMBIGIOUS ) > 0 ) {
                changeUserStress( syll, stressed );
            }
        }

        showAccents();
    }
}
