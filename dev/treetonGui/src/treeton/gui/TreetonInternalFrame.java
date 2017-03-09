/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import javax.swing.*;

public abstract class TreetonInternalFrame extends JInternalFrame {
    protected TreetonInternalFrame() {
    }

    protected TreetonInternalFrame(String title) {
        super(title);
    }

    protected TreetonInternalFrame(String title, boolean resizable) {
        super(title, resizable);
    }

    protected TreetonInternalFrame(String title, boolean resizable, boolean closable) {
        super(title, resizable, closable);
    }

    protected TreetonInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable) {
        super(title, resizable, closable, maximizable);
    }

    protected TreetonInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);
    }

    public abstract void deinit();

    public abstract void init();

    public abstract void activate();

    public abstract void deactivate();
}
