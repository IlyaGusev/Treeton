/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.config.GuiConfiguration;
import treeton.gui.texteditor.TextEditorPanel;

public class DefaultLabelGeneratorConfiguration {
    private static DefaultLabelGeneratorConfiguration instance;

    public static DefaultLabelGeneratorConfiguration getInstance() {
        if (instance == null) {
            instance = new DefaultLabelGeneratorConfiguration();
        }
        return instance;
    }

    public void configure() {
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "System", SystemLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Sem", AbbyyLabelGenerator.class);
        GuiConfiguration.getInstance().registerLabelGenerator(TextEditorPanel.class, "Lexeme", AbbyyLabelGenerator.class);
    }
}