/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import javax.swing.*;

public class CombinatorVisualizerFrame extends JFrame {
    public String windowTitle;
    private CombinatorVisualizer2D panel;

    public CombinatorVisualizerFrame(Combinator combinator, double normScale) {
        super();
        panel = new CombinatorVisualizer2D(combinator, normScale);
        init();
    }


    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        setContentPane(panel);
        this.setBounds(100, 100, 500, 500);

        this.validate();
    }

    public void validate() {
        panel.componentResized(null);
        super.validate();
    }

}
