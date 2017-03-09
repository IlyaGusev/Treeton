/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;

public class CheckBoxItem {
    protected String name;
    protected boolean checked;
    protected boolean enabled;
    protected String toolTip;
    protected Color frColor;
    protected Icon icon;

    public CheckBoxItem() {
        this(null, false, true, null);
    }

    public CheckBoxItem(String name) {
        this(name, false, true, null);
    }

    public CheckBoxItem(String name, boolean checked) {
        this(name, checked, true, null);
    }

    public CheckBoxItem(String name, boolean checked,
                        String toolTip) {
        this(name, checked, true, toolTip);
    }

    public CheckBoxItem(String name, boolean checked,
                        boolean enabled, String toolTip) {
        this.name = name;
        this.checked = checked;
        this.enabled = enabled;
        this.toolTip = toolTip;
        frColor = null;
        icon = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public Color getFrColor() {
        return frColor;
    }

    public void setFrColor(Color frColor) {
        this.frColor = frColor;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String toString() {
        return name;
    }

    public boolean equals(CheckBoxItem c) {
        return false;
    }
}
