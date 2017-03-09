/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.core.Fraction;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.model.TrnType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class UserDrawFrame extends JFrame implements ActionListener {
    public String parentMenuName;
    public String menuTitle;
    public String windowTitle;

    private UserDrawFrame instance = null;
    private TreenotationStorage treenotations;
    private String document;
    private TrnType[] type;
    private Fraction from, to;

    protected UserDrawFrame(TrnType[] tp, Fraction _from, Fraction _to, TreenotationStorage trns, String doc) {
        super();
        parentMenuName = "Tools";
        menuTitle = "Viewer";
        windowTitle = "View";
        treenotations = trns;
        instance = this;
        document = doc.toString().replace('\r', ' ').replace('\n', ' ');
        type = tp;
        from = _from;
        to = _to;
        init();
    }

    public static UserDrawFrame getInstance(TrnType[] type, Fraction from, Fraction to, TreenotationStorage trns, String doc) {
        return new UserDrawFrame(type, from, to, trns, doc);
    }

    public static void showUserDrawFrame(TrnType[] type, Fraction from, Fraction to, TreenotationStorage trns, String doc) {
        UserDrawFrame userFrame = getInstance(type, from, to, trns, doc);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();

    }

    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        JScrollPane jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        DrawingPane dp = new DrawingPane();
        jsc.getViewport().add(dp, null);
        instance.setBounds(0, 0, 100, 100);

        setContentPane(jsc);

        this.validate();
    }

    protected void addListeners() {

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                getContentPane().repaint();
            }
        });
    }

    public void actionPerformed(ActionEvent e) {

    }

    class DrawingPane extends JComponent {

        LinkedList lineList;
        LinkedList connectionsList;
        HashMap types;
        Font font;
        int maxx = 0, maxy = 0, charWidth = 8, charHeight = 16, maxOffset;

        DrawingPane() {
            super();
            lineList = null;
            types = new HashMap();
            font = null;
        }

        public void paint(Graphics _g) {
            Graphics2D g = (Graphics2D) _g;
            if (lineList == null) {
                lineList = new LinkedList();
                connectionsList = new LinkedList();

                if (treenotations == null)
                    return;
                if (treenotations.isEmpty())
                    return;


                int bitField[][] = new int[100][((treenotations.lastToken().getEndNumerator() + 1) / 32) + 1];

                for (int i = 0; i < 100; i++) {
                    Arrays.fill(bitField[i], 0);
                }

                int x1 = 0, x2 = 0, y = 0, n = 0;

                Iterator itr = treenotations.typeIterator(type, from, to);
                Treenotation cur = null;
                n = 0;
                while (itr.hasNext()) {
                    cur = (Treenotation) itr.next();
//          System.out.print("..."+n);

                    x1 = cur.getStartNumerator();
                    x2 = cur.getEndNumerator();

                    if (x1 > maxx)
                        maxx = x1;
                    if (x2 > maxx)
                        maxx = x2;

                    int j;
                    for (y = 0; y < 100; y++) {
                        for (j = x1; j < x2; j++) {
                            if ((bitField[y][j / 32] & (1 << (j % 32))) != 0) {
                                break;
                            }
                        }
                        if (j == x2) {
                            break;
                        }
                    }

                    for (j = x1; j < x2; j++) {
                        bitField[y][j / 32] |= (1 << (j % 32));
                    }


                    Iterator itr1 = lineList.iterator();
                    MyLine p;
                    while (itr1.hasNext()) {
                        p = (MyLine) itr1.next();
                        if (p.trn.isChildOf(cur)) {
                            connectionsList.add(new MyLine(((double) (p.x1 + p.x2)) / 2, p.y1, ((double) (p.x1 + p.x2)) / 2, y * 10, null, 0));
                        }
                        if (cur.isChildOf(p.trn)) {
                            connectionsList.add(new MyLine(((double) (x1 + x2)) / 2, y * 10, ((double) (x1 + x2)) / 2, p.y1, null, 0));
                        }
                    }

                    lineList.add(new MyLine(x1, y * 10, x2, y * 10, cur, n++));


                    if (y * 10 > maxy) {
                        maxy = y * 10;
                    }
                }

                maxOffset = maxx;
                font = new Font("Courier New", 0, 20);
                FontMetrics fm = g.getFontMetrics(font);
                charWidth = fm.charWidth('a');
                charHeight = fm.getHeight();
                maxx *= charWidth;
                maxy *= 2;
                maxx += 10;
                maxy += charHeight * 2 + 8;

                itr = lineList.iterator();
                Color fake = new Color(0, 0, 0);
                while (itr.hasNext()) {
                    MyLine cr = (MyLine) itr.next();
                    cr.x1 *= charWidth;
                    cr.x1 += 5;
                    cr.x2 *= charWidth;
                    cr.x2 += 5;
                    cr.y1 *= 2;
                    cr.y1 += charHeight + 4;
                    cr.y2 *= 2;
                    cr.y2 += charHeight + 4;
                    types.put(cr.trnType, fake);
                    cr.trn = null;
                }

                itr = connectionsList.iterator();
                while (itr.hasNext()) {
                    MyLine cr = (MyLine) itr.next();
                    cr.x1 *= charWidth;
                    cr.x1 += 5;
                    cr.x2 *= charWidth;
                    cr.x2 += 5;
                    cr.y1 *= 2;
                    cr.y1 += charHeight + 4;
                    cr.y2 *= 2;
                    cr.y2 += charHeight + 4;
                }


                itr = types.keySet().iterator();
                while (itr.hasNext()) {
                    float[] hsb = new float[3];
                    hsb[0] = (float) Math.random() * 0.8f + 0.1f;
                    hsb[1] = (float) Math.random() / 2f + 0.4f;
                    hsb[2] = (float) Math.random() / 2f + 0.4f;
                    types.put((TrnType) itr.next(), new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])));
                }

                document = document.substring(0, maxOffset);

                setPreferredSize(new Dimension(maxx, maxy));

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                font = new Font("Courier New", 0, 10);
                fm = g.getFontMetrics(font);
                Rectangle2D rect = fm.getStringBounds(Integer.toString(maxOffset + 1), g);
                instance.setBounds(60, 0, Math.max(Math.min(screenSize.width - 60, maxx + 20), 50), Math.max(Math.min(screenSize.height, maxy + (int) rect.getWidth() + 48), 50));
                instance.validate();
                repaint();

            } else {
                g.setFont(font);
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, maxx, maxy);
                int n = 0;
                font = new Font("Courier New", 0, 10);
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics(font);
                Rectangle2D rect = fm.getStringBounds(Integer.toString(maxOffset + 1), g);
                int len = maxy + (int) rect.getWidth();
                for (int i = 4; i < maxx; i += charWidth, n++) {
                    g.setColor(Color.WHITE);
                    g.drawLine(i, 0, i, len);
                    AffineTransform b;
                    AffineTransform a = new AffineTransform((b = g.getTransform()));
                    a.rotate(3.1415926 / 2);
                    g.setTransform(a);
                    g.setColor(new Color(0, 0, 190));
                    g.drawString(Integer.toString(n), maxy, -i - 2);
                    g.setTransform(b);
                }
                font = new Font("Courier New", 0, 20);
                g.setFont(font);
                g.setColor(Color.BLACK);
                g.drawString(document, 5, maxy - 2);
                font = new Font("Courier New", 0, 10);
                g.setFont(font);
                Iterator itr = lineList.iterator();
                while (itr.hasNext()) {
                    MyLine cur = (MyLine) itr.next();
                    Color bg = (Color) types.get(cur.trnType);
                    g.setColor(bg);
                    g.fillRect((int) cur.x1, maxy - (int) cur.y1 - 10, (int) cur.x2 - (int) cur.x1 - 1, 8);
                    if (cur.corrupted) {
                        g.setColor(Color.RED);
                        g.drawLine((int) (cur.x1 + cur.x2) / 2 - 3, maxy - (int) cur.y1 - 6 - 3, (int) (cur.x1 + cur.x2) / 2 + 3, maxy - (int) cur.y1 - 6 + 3);
                        g.drawLine((int) (cur.x1 + cur.x2) / 2 + 3, maxy - (int) cur.y1 - 6 - 3, (int) (cur.x1 + cur.x2) / 2 - 3, maxy - (int) cur.y1 - 6 + 3);
                    }
/*          float[] hsb=Color.RGBtoHSB(bg.getRed(),bg.getGreen(),bg.getBlue(),null);
          hsb[0]=hsb[0] > 0.5f ? (float)(hsb[0] - 0.5f) : (float)(0.5f + hsb[0]);
          /*hsb[2]=hsb[2] < 0.5f ? (float) Math.min(hsb[2]*1.5f,1f) : hsb[2]*0.75f;*/
                    g.setColor(Color.WHITE);
                    fm = g.getFontMetrics(font);
                    rect = fm.getStringBounds(Integer.toString(cur.number), g);
                    if (rect.getWidth() < cur.x2 - cur.x1 + 1) {
                        g.drawString(Integer.toString(cur.number), (int) cur.x1, maxy - (int) cur.y1 - 3);
                    } else {
                        g.drawString("**", (int) cur.x1, maxy - (int) cur.y1 - 3);
                    }
                }
                g.setColor(new Color(60, 60, 60));
                itr = connectionsList.iterator();
                while (itr.hasNext()) {
                    MyLine cur = (MyLine) itr.next();
                    if (cur.y1 < cur.y2) {
                        g.drawLine((int) cur.x1 - 2, maxy - (int) cur.y1 - 11, (int) cur.x1 + 2, maxy - (int) cur.y1 - 11);
                        g.drawLine((int) cur.x1, maxy - (int) cur.y1 - 11, (int) cur.x2, maxy - (int) cur.y2 - 2);
                        g.drawLine((int) cur.x2 - 3, maxy - (int) cur.y2 - 2 + 3, (int) cur.x2, maxy - (int) cur.y2 - 2);
                        g.drawLine((int) cur.x2 + 3, maxy - (int) cur.y2 - 2 + 3, (int) cur.x2, maxy - (int) cur.y2 - 2);
                    } else {
                        g.drawLine((int) cur.x1 - 2, maxy - (int) cur.y1 - 2, (int) cur.x1 + 2, maxy - (int) cur.y1 - 2);
                        g.drawLine((int) cur.x1, maxy - (int) cur.y1 - 2, (int) cur.x2, maxy - (int) cur.y2 - 11);
                        g.drawLine((int) cur.x2 - 3, maxy - (int) cur.y2 - 11 - 3, (int) cur.x2, maxy - (int) cur.y2 - 11);
                        g.drawLine((int) cur.x2 + 3, maxy - (int) cur.y2 - 11 - 3, (int) cur.x2, maxy - (int) cur.y2 - 11);
                    }
                }
            }
        }

        class MyLine {
            double x1, y1, x2, y2;
            int number;
            Treenotation trn;
            boolean corrupted;
            TrnType trnType;

            MyLine(double _x1, double _y1, double _x2, double _y2, Treenotation _trn, int _number) {
                x1 = _x1;
                y1 = _y1;
                x2 = _x2;
                y2 = _y2;
                trn = _trn;
                number = _number;
                if (trn != null) {
                    if (trn instanceof Token) {
                        corrupted = false;
                        for (int i = 0; i < type.length; i++)
                            corrupted = corrupted || ((Token) trn).corrupted(type[i]);
                    } else {
                        corrupted = trn.corrupted();
                    }
                    trnType = trn.getType();
                }
            }
        }

        class HeightComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                MyLine a = (MyLine) o1;
                MyLine b = (MyLine) o2;

                if (a.y1 < b.y1) {
                    return -1;
                } else if (a.y1 == b.y1) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
    }
}
