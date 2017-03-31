/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.metricindex.MetricIndex;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetricSearchCriteriaPanel extends JPanel implements ActionListener {
    private MetricSearchCriteria currentSearchCriteria;
    private Map<MetricSearchParameter, JComboBox<String>> currentEnumParameters = new HashMap<>();
    private Map<MetricSearchParameter, JTextField> currentStringParameters = new HashMap<>();
    private Map<MetricSearchParameter, JTextField> currentRealParameters = new HashMap<>();
    private Map<MetricSearchParameter, JComboBox<MetricSearchCriteria.OperatorType>> currentRealOperators = new HashMap<>();

    private Map<JComboBox<String>,MetricSearchParameter> currentEnumParametersReverse = new HashMap<>();
    private Map<Document,MetricSearchParameter> currentStringParametersReverse = new HashMap<>();
    private Map<Document,MetricSearchParameter> currentRealParametersReverse = new HashMap<>();
    private Map<JComboBox<MetricSearchCriteria.OperatorType>,MetricSearchParameter> currentRealOperatorsReverse = new HashMap<>();

    private MetricSearchCriteriaPanelListener listener;

    public MetricSearchCriteriaPanel(  MetricSearchCriteria criteria, MetricSearchCriteriaPanelListener listener ) throws TreetonModelException {
        currentSearchCriteria = criteria;
        this.listener = listener;
        init();
    }

    private void init(){
        setupLayout();
    }

    private void setupLayout() {
        currentEnumParameters.clear();
        currentRealParameters.clear();
        currentRealOperators.clear();
        currentStringParameters.clear();
        currentEnumParametersReverse.clear();
        currentRealParametersReverse.clear();
        currentRealOperatorsReverse.clear();
        currentStringParametersReverse.clear();

        removeAll();

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        if( currentSearchCriteria == null ) {
            return;
        }

        MetricSearchCriteriaSignature signature = currentSearchCriteria.getSignature();

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;

        for (MetricSearchParameter metricSearchParameter : signature.metricSearchParameters) {
            c.gridx = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill   = GridBagConstraints.BOTH;
            c.gridheight = 1;
            c.gridwidth  = 1;
            c.weightx = 1;
            c.weighty = 0;

        /* Заголовок */
            JLabel label = new JLabel();
            label.setBackground(Color.WHITE);
            label.setFont(new Font("Tahoma", Font.PLAIN, 16));
            label.setText(metricSearchParameter.name);

            layout.setConstraints(label, c);
            this.add(label);

            c.gridx = 1;
            c.anchor = GridBagConstraints.NORTHEAST;
            c.fill   = GridBagConstraints.BOTH;
            c.gridheight = 1;
            c.gridwidth  = 1;
            c.weightx = 1;
            c.weighty = 0;

            if( metricSearchParameter.type == MetricSearchParameter.ParameterType.ENUM ) {
                JComboBox<String> valuesCombo = new JComboBox<>();
                currentEnumParameters.put(metricSearchParameter, valuesCombo );
                currentEnumParametersReverse.put( valuesCombo, metricSearchParameter );

                for (String predefinedValue : metricSearchParameter.predefinedValues) {
                    valuesCombo.addItem( predefinedValue );
                }
                valuesCombo.addActionListener(this);
                valuesCombo.setSelectedItem( currentSearchCriteria.getParameterValue(metricSearchParameter.name) );

                layout.setConstraints(valuesCombo, c);
                this.add(valuesCombo);

            } else if( metricSearchParameter.type == MetricSearchParameter.ParameterType.STRING ) {
                JTextField tf = new JTextField();
                currentStringParameters.put(metricSearchParameter,tf);
                currentStringParametersReverse.put(tf.getDocument(),metricSearchParameter);
                tf.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        stringParameterValueChanged( e );
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        stringParameterValueChanged( e );
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        stringParameterValueChanged( e );
                    }
                });
                tf.setText( currentSearchCriteria.getParameterValue(metricSearchParameter.name) );

                layout.setConstraints(tf, c);
                this.add(tf);

            } else {
                assert metricSearchParameter.type == MetricSearchParameter.ParameterType.REAL;
                JComboBox<MetricSearchCriteria.OperatorType> valuesCombo = new JComboBox<>();
                currentRealOperators.put(metricSearchParameter,valuesCombo);
                currentRealOperatorsReverse.put(valuesCombo,metricSearchParameter);
                JTextField tf = new JTextField();
                currentRealParameters.put(metricSearchParameter,tf);
                currentRealParametersReverse.put(tf.getDocument(),metricSearchParameter);

                for (MetricSearchCriteria.OperatorType predefinedValue : MetricSearchCriteria.OperatorType.values()) {
                    valuesCombo.addItem( predefinedValue );
                }
                valuesCombo.addActionListener(this);
                valuesCombo.setSelectedItem( currentSearchCriteria.getOperatorType(metricSearchParameter.name) );


                tf.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        realParameterValueChanged( e );
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        realParameterValueChanged( e );
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        realParameterValueChanged( e );
                    }
                });

                tf.setText( currentSearchCriteria.getParameterValue(metricSearchParameter.name));


                JPanel auxPanel = new JPanel();
                GridBagLayout auxLayout = new GridBagLayout();
                auxPanel.setLayout(auxLayout);
                GridBagConstraints auxc = new GridBagConstraints();
                auxc.gridx = 0;
                auxc.gridy = 0;
                auxc.anchor = GridBagConstraints.NORTHWEST;
                auxc.fill   = GridBagConstraints.HORIZONTAL;
                auxc.gridheight = 1;
                auxc.gridwidth  = 1;
                auxc.weightx = 0;
                auxc.weighty = 0;
                auxLayout.setConstraints(valuesCombo,auxc);
                auxPanel.add(valuesCombo);
                auxc.gridx = 1;
                auxc.weightx = 1;
                auxc.anchor = GridBagConstraints.NORTHEAST;
                auxLayout.setConstraints(tf,auxc);
                auxPanel.add(tf);

                layout.setConstraints(auxPanel, c);
                this.add(auxPanel);
            }
            c.gridy++;
        }

        revalidate();
    }

    private void realParameterValueChanged(DocumentEvent e) {
        MetricSearchParameter parameter = currentRealParametersReverse.get(e.getDocument());
        JComboBox<MetricSearchCriteria.OperatorType> opCombo = currentRealOperators.get( parameter );
        try {
            boolean res = currentSearchCriteria.setParameterInfo(parameter.name,
                    e.getDocument().getText(0,e.getDocument().getLength()), (MetricSearchCriteria.OperatorType) opCombo.getSelectedItem());
            if( !res ) {
                currentRealParameters.get(parameter).setBackground(Color.PINK);
            } else {
                currentRealParameters.get(parameter).setBackground(Color.WHITE);
            }
        } catch (BadLocationException e1) {
            currentRealParameters.get(parameter).setBackground(Color.PINK);
        }
        listener.metricSearchCriteriaChanged( currentSearchCriteria );
    }

    private void stringParameterValueChanged(DocumentEvent e) {
        MetricSearchParameter parameter = currentStringParametersReverse.get(e.getDocument());
        try {
            boolean res = currentSearchCriteria.setParameterInfo(parameter.name,
                    e.getDocument().getText(0,e.getDocument().getLength()), MetricSearchCriteria.OperatorType.EQUAL);
            if( !res ) {
                currentStringParameters.get(parameter).setBackground(Color.PINK);
            } else {
                currentStringParameters.get(parameter).setBackground(Color.WHITE);
            }
        } catch (BadLocationException e1) {
            currentStringParameters.get(parameter).setBackground(Color.PINK);
        }
        listener.metricSearchCriteriaChanged( currentSearchCriteria );
    }

    @SuppressWarnings({"unchecked"})
    private void enumParameterValueChanged(ActionEvent e) {
        JComboBox<String> combo = (JComboBox<String>) e.getSource();
        MetricSearchParameter parameter = currentEnumParametersReverse.get(combo);
        boolean res = currentSearchCriteria.setParameterInfo(parameter.name,
                combo.getSelectedItem().toString(), MetricSearchCriteria.OperatorType.EQUAL);
        if( !res ) {
            combo.setBackground(Color.PINK);
        } else {
            combo.setBackground(Color.WHITE);
        }
        listener.metricSearchCriteriaChanged( currentSearchCriteria );
    }

    @SuppressWarnings("unchecked")
    private void realOperatorParameterValueChanged(ActionEvent e) {
        JComboBox<MetricSearchCriteria.OperatorType> opCombo = (JComboBox<MetricSearchCriteria.OperatorType>) e.getSource();

        MetricSearchParameter parameter = currentRealOperatorsReverse.get(opCombo);
        JTextField tf = currentRealParameters.get(parameter);
        try {
            boolean res = currentSearchCriteria.setParameterInfo(parameter.name,
                    tf.getDocument().getText(0,tf.getDocument().getLength()), (MetricSearchCriteria.OperatorType) opCombo.getSelectedItem());
            if( !res ) {
                tf.setBackground(Color.PINK);
            } else {
                tf.setBackground(Color.WHITE);
            }
        } catch (BadLocationException e1) {
            tf.setBackground(Color.PINK);
        }

        listener.metricSearchCriteriaChanged( currentSearchCriteria );
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void actionPerformed(ActionEvent e) {
         if( currentRealOperatorsReverse.containsKey(e.getSource())) {
            realOperatorParameterValueChanged(e);
        } else if( currentEnumParametersReverse.containsKey(e.getSource())) {
            enumParameterValueChanged(e);
        }
    }
}
