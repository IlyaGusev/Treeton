/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.FSM;
import treeton.core.fsm.Term;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

//todo: сделать всплывающие подсказки

public class FSMViewPanel extends JPanel implements MouseListener, MouseMotionListener {
    JScrollPane jsc;
    private FSM fsm;
    private FSMDrawer drawer;
    private DrawingPane dp;
    private Point pressed;
    private double pressedX, pressedY;
    private VisibleState curState;
    private JTextPane textArea;
    private JSplitPane split;
    private boolean ready;

    protected FSMViewPanel(FSM _fsm) {
        super();
        fsm = _fsm;
        ready = false;
        init();
    }


    protected void init() {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        dp = new DrawingPane(fsm);
        jsc.getViewport().add(dp, null);

        textArea = new JTextPane();
        textArea.setEditable(false);
        textArea.setContentType("text/html");
        add(textArea, BorderLayout.SOUTH);


        JScrollPane tjsc = new JScrollPane();
        tjsc.getViewport().add(textArea, null);

        split.add(jsc, JSplitPane.TOP);
        split.add(tjsc, JSplitPane.BOTTOM);

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);


        this.validate();
        dp.addMouseListener(this);
        dp.addMouseMotionListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        Term t = drawer.getTerm(dp.getGraphics(), 0, 0, p);
        if (t != null) {
            textArea.setText(t.getString());
        } else {
            VisibleState s = drawer.getState(0, 0, p);
            if (s != null)
                textArea.setText(s.getString());
            else
                textArea.setText("");
        }
        dp.repaint();

    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mousePressed(MouseEvent e) {
        pressed = e.getPoint();
        curState = drawer.getState(0, 0, pressed);
        if (curState != null) {
            textArea.setText(curState.getString());
            pressedX = curState.x;
            pressedY = curState.y;
        } else {
            textArea.setText("");
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseReleased(MouseEvent e) {
        drawer.prefWidth = 0;
        dp.setPreferredSize(drawer.getPrefferedSize());

        jsc.getViewport().remove(dp);
        jsc.getViewport().add(dp);
        curState = null;


        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseDragged(MouseEvent e) {
        int deltax = e.getX() - pressed.x;
        int deltay = e.getY() - pressed.y;
        if (curState != null) {
            curState.x = pressedX + deltax;
            curState.y = pressedY + deltay;
            if (curState.y < 0)
                curState.y = 0;
            if (curState.x < 0)
                curState.x = 0;
            dp.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setStateDrawers(Map<Class, StateDrawer> stateDrawers) {
        if (drawer.stateDrawers == null)
            drawer.stateDrawers = new HashMap<Class, StateDrawer>();
        for (Map.Entry<Class, StateDrawer> entry : stateDrawers.entrySet()) {
            drawer.stateDrawers.put(entry.getKey(), entry.getValue());
        }

    }

    public void paint(Graphics g) {
        if (!ready) {
            ready = true;
            split.setDividerLocation(0.8);
            repaint();
        } else {
            super.paint(g);
        }
    }

    class DrawingPane extends JComponent {
        DrawingPane(FSM fsm) {
            drawer = new FSMDrawer(fsm);
            setPreferredSize(drawer.getPrefferedSize());
        }

        public void paint(Graphics _g) {
            Graphics2D g = (Graphics2D) _g;
            drawer.draw(0, 0, g);
        }

        public Dimension getPreferredSize() {
            if (drawer != null)
                return drawer.getPrefferedSize();
            return new Dimension(100, 100);
        }
    }

}
