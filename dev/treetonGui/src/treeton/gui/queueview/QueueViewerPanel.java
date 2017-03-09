/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.queueview;

import treeton.core.util.Refreshable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QueueViewerPanel extends JPanel implements TreeSelectionListener, Refreshable {
    List<QueueViewerListener> listeners = new ArrayList<QueueViewerListener>();
    JTree tree;
    Queue queue;
    QueueTreeModel model;
    WeightController weightController;
    LabelGenerator labelGenerator;
    String caption = "";
    QueueViewerCellRenderer renderer;

    public QueueViewerPanel() {
        tree = new JTree() {
            public String getToolTipText(MouseEvent e) {
                Object o;
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    o = path.getLastPathComponent();
                    if (o == model)
                        return model.toString();
                    return labelGenerator.getLabel(((QueueTreeModel.ElementWrapper) o).obj);
                }
                return "";
            }
        };
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.setModel(model = new QueueTreeModel());
        tree.setCellRenderer(renderer = new QueueViewerCellRenderer());
        tree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tree.setScrollsOnExpand(true);
        tree.addTreeSelectionListener(this);
        tree.setBorder(BorderFactory.createEmptyBorder(1, 3, 3, 1));

        JScrollPane jscr = new JScrollPane();
        jscr.getViewport().add(tree);
        setLayout(new BorderLayout(2, 2));
        add(jscr, BorderLayout.CENTER);
    }

    public Queue getQueue() {
        return queue;
    }

    public synchronized void setQueue(Queue queue) {
        this.queue = queue == null ? new LinkedList() : queue;

        refresh();
    }

    public void addListener(QueueViewerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(QueueViewerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(QueueViewerEventType type, Object info) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).queueViewChanged(type, info, this);
        }
    }

    public LabelGenerator getLabelGenerator() {
        return labelGenerator;
    }

    public void setLabelGenerator(LabelGenerator labelGenerator) {
        this.labelGenerator = labelGenerator;
        renderer.setLabelGenerator(labelGenerator);
    }

    public void setWeightController(WeightController controller) {
        this.weightController = controller;
        renderer.setWeightController(controller);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
        model.setName(caption);
    }

    public synchronized void refresh() {
        QueueTreeModel model = new QueueTreeModel();
        model.importQueue(queue);
        model.setName(caption);
        tree.setModel(this.model = model);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUI();
            }
        });
    }

    public void valueChanged(TreeSelectionEvent e) {
        Object o = e.getPath().getLastPathComponent();
        if (o instanceof QueueTreeModel) {
            notifyListeners(QueueViewerEventType.NODE_SELECTED, null);
        } else {
            notifyListeners(QueueViewerEventType.NODE_SELECTED, ((QueueTreeModel.ElementWrapper) o).obj);
        }
    }

    public void selectNone() {
        tree.setSelectionPath(null);
    }


}
