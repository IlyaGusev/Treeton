/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceChain;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Vector;

public class InstantiatedResourcesModel implements TreeModel {
    Vector<Object> instances = new Vector<Object>();
    ArrayList<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    public Object getRoot() {
        return this;
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof ResourceChain) {
            ResourceChain ch = (ResourceChain) parent;
            return ch.getResource(index);
        } else if (parent == this) {
            return instances.get(index);
        }
        return null;
    }

    public int getChildCount(Object parent) {
        if (parent instanceof ResourceChain) {
            ResourceChain ch = (ResourceChain) parent;
            return ch.getNumberOfResources();
        } else if (parent == this) {
            return instances.size();
        }
        return 0;
    }

    public boolean isLeaf(Object node) {
        return node instanceof Resource && !(node instanceof ResourceChain);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        Object o = path.getLastPathComponent();
        if (o instanceof ResourceChain) {
            ((ResourceChain) o).setName(newValue.toString());
        }
        notifyListenersChanged(new TreeModelEvent(this, path));
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof ResourceChain) {
            ResourceChain ch = (ResourceChain) parent;
            return ch.getNumberOfResource((Resource) child);
        } else if (parent == this) {
            return instances.indexOf(child);
        }
        return 0;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void addResource(Resource res, int before) {
        if (before < 0) {
            before = 0;
        } else if (before > instances.size()) {
            before = instances.size();
        }
        instances.insertElementAt(res, before);
        notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, res}));
    }

    public void addResourceChain(ResourceChain ch, int before) {
        if (before < 0) {
            before = 0;
        } else if (before > instances.size()) {
            before = instances.size();
        }
        instances.insertElementAt(ch, before);
        notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, ch}));
    }

    public void removeChain(ResourceChain ch) {
        instances.remove(ch);
        notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, ch}));
    }

    public void removeResource(Resource res) {
        if (instances.contains(res)) {
            instances.remove(res);
            notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, res}));
        } else {
            for (Object o : instances) {
                if (o instanceof ResourceChain) {
                    ResourceChain ch = (ResourceChain) o;
                    if (ch.containsResource(res)) {
                        ch.removeResource(res);
                        notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, ch, res}));
                        return;
                    }
                }
            }
        }
    }

    public boolean mayBeShiftedUp(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof ResourceChain) {
            int i = instances.indexOf(o);
            if (i > 0) {
                return true;
            }
        } else if (o instanceof Resource) {
            if (path.getPath().length == 2) {
                int i = instances.indexOf(o);
                if (i > 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public void shiftObjectUp(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof ResourceChain) {
            int i = instances.indexOf(o);
            if (i > 0) {
                Object o1 = instances.get(i - 1);
                instances.set(i - 1, o);
                instances.set(i, o1);
                notifyListenersChanged(new TreeModelEvent(this, new Object[]{this}, new int[]{i, i - 1}, new Object[]{o, o1}));
            }
        } else if (o instanceof Resource) {
            if (path.getPath().length == 2) {
                int i = instances.indexOf(o);
                if (i > 0) {
                    Object o1 = instances.get(i - 1);
                    if (o1 instanceof ResourceChain) {
                        ResourceChain ch = (ResourceChain) o1;
                        Resource res = (Resource) o;
                        instances.remove(o);
                        notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, res}));
                        ch.addResource(res);
                        notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, ch, res}));
                    } else if (o1 instanceof Resource) {
                        instances.set(i - 1, o);
                        instances.set(i, o1);
                        notifyListenersChanged(new TreeModelEvent(this, new Object[]{this}, new int[]{i, i - 1}, new Object[]{o, o1}));
                    }
                }
            } else {
                Resource res = (Resource) o;
                ResourceChain ch = (ResourceChain) path.getPath()[1];
                int k = ch.indexOfResource(res);
                if (k == 0) {
                    ch.removeResource(res);
                    notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, ch, res}));
                    int j = instances.indexOf(ch);
                    instances.insertElementAt(res, j);
                    notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, res}));
                } else if (k > 0) {
                    Resource res1 = ch.getResource(k - 1);
                    ch.swapResources(res, res1);
                    notifyListenersChanged(new TreeModelEvent(this, new Object[]{this, ch}, new int[]{k, k - 1}, new Object[]{res, res1}));
                }
            }
        }
    }

    public boolean mayBeShiftedDown(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof ResourceChain) {
            int i = instances.indexOf(o);
            if (i < instances.size() - 1) {
                return true;
            }
        } else if (o instanceof Resource) {
            if (path.getPath().length == 2) {
                int i = instances.indexOf(o);
                if (i < instances.size() - 1) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public void shiftObjectDown(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof ResourceChain) {
            int i = instances.indexOf(o);
            if (i < instances.size() - 1) {
                Object o1 = instances.get(i + 1);
                instances.set(i + 1, o);
                instances.set(i, o1);
                notifyListenersChanged(new TreeModelEvent(this, new Object[]{this}, new int[]{i, i + 1}, new Object[]{o, o1}));
            }
        } else if (o instanceof Resource) {
            if (path.getPath().length == 2) {
                int i = instances.indexOf(o);
                if (i < instances.size() - 1) {
                    Object o1 = instances.get(i + 1);
                    if (o1 instanceof ResourceChain) {
                        ResourceChain ch = (ResourceChain) o1;
                        Resource res = (Resource) o;
                        instances.remove(o);
                        notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, res}));
                        ch.addResourceFirst(res);
                        notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, ch, res}));
                    } else if (o1 instanceof Resource) {
                        instances.set(i + 1, o);
                        instances.set(i, o1);
                        notifyListenersChanged(new TreeModelEvent(this, new Object[]{this}, new int[]{i, i + 1}, new Object[]{o, o1}));
                    }
                }
            } else {
                Resource res = (Resource) o;
                ResourceChain ch = (ResourceChain) path.getPath()[1];
                int k = ch.indexOfResource(res);
                if (k == ch.getNumberOfResources() - 1) {
                    ch.removeResource(res);
                    notifyListenersRemoved(new TreeModelEvent(this, new Object[]{this, ch, res}));
                    int j = instances.indexOf(ch);
                    instances.insertElementAt(res, j + 1);
                    notifyListenersInserted(new TreeModelEvent(this, new Object[]{this, res}));
                } else if (k < ch.getNumberOfResources() - 1) {
                    Resource res1 = ch.getResource(k + 1);
                    ch.swapResources(res, res1);
                    notifyListenersChanged(new TreeModelEvent(this, new Object[]{this, ch}, new int[]{k, k + 1}, new Object[]{res, res1}));
                }
            }
        }
    }


    private void notifyListenersInserted(TreeModelEvent event) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listeners.size(); i++) {
            TreeModelListener treeModelListener = listeners.get(i);
            treeModelListener.treeNodesInserted(event);
        }
    }

    private void notifyListenersRemoved(TreeModelEvent event) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listeners.size(); i++) {
            TreeModelListener treeModelListener = listeners.get(i);
            treeModelListener.treeNodesRemoved(event);
        }
    }

    private void notifyListenersChanged(TreeModelEvent event) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listeners.size(); i++) {
            TreeModelListener treeModelListener = listeners.get(i);
            treeModelListener.treeNodesChanged(event);
        }
    }


    public Object[] pathForObject(Object o) {
        if (o instanceof ResourceChain) {
            return new Object[]{this, o};
        }
        if (o instanceof Resource) {
            if (instances.contains(o)) {
                return new Object[]{this, o};
            } else {
                for (Object o1 : instances) {
                    if (o1 instanceof ResourceChain && ((ResourceChain) o1).containsResource((Resource) o)) {
                        return new Object[]{this, o1, o};
                    }
                }
            }
        } else if (o == this) {
            return new Object[]{this};
        }
        return null;
    }
}
