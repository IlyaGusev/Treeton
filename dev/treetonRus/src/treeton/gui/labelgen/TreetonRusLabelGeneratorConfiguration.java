/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.config.GuiConfiguration;
import treeton.gui.texteditor.TextEditorPanel;

public class TreetonRusLabelGeneratorConfiguration extends DefaultLabelGeneratorConfiguration {
    private static TreetonRusLabelGeneratorConfiguration instance;

    public static TreetonRusLabelGeneratorConfiguration getInstance() {
        if (instance == null) {
            instance = new TreetonRusLabelGeneratorConfiguration();
        }
        return instance;
    }

    public void configure() {
        super.configure();
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Token", TokenLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "SpaceToken", TokenLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Gramm", GrammLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "System", SystemLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Verse", VerseLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Syllable", SyllableLabelGenerator.class);
    }

}
