/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.Context;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceModel;
import treeton.core.config.context.resources.api.ResourcesContext;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class ResourcesContextModel implements TreeModel {
    ResourcesContext rootContext;
    boolean ancestorsOnly;
    ResourcesContext targetContext;
    Stack<Object> stack = new Stack<Object>();

    public ResourcesContextModel(ResourcesContext rootContext) {
        this.rootContext = rootContext;
    }

    public Object getRoot() {
        return rootContext;
    }

    public Object getChild(Object parent, int index) {
        try {
            if (parent instanceof ResourcesContext) {
                ResourcesContext c = (ResourcesContext) parent;

                int nr = c.getResourcesCount();
                if (index >= 0 && index < nr) {
                    Iterator<String> it = c.resourcesIterator();
                    for (int i = 0; i < nr; i++) {
                        String s = it.next();
                        if (i == index) {
                            return new Object[]{c, s};
                        }
                    }
                } else if (index >= nr) {
                    int nc = c.getResourceChainsCount();
                    if (index - nr < nc) {
                        Iterator<String> it = c.resourceChainsIterator();
                        for (int i = 0; i < nc; i++) {
                            String s = it.next();
                            if (i == index - nr) {
                                return new Object[]{c, s};
                            }
                        }
                    } else if (index - nr >= nc) {
                        int nd = c.getChildCount();
                        if (ancestorsOnly) {
                            if (index - nr - nc == 0) {
                                Iterator<Context> it = c.childIterator();
                                for (int i = 0; i < nd; i++) {
                                    Context dm = it.next();
                                    if (ContextUtil.inherits(targetContext, dm))
                                        return dm;
                                }
                            }
                        } else {
                            if (index - nr - nc < nd) {
                                Iterator<Context> it = c.childIterator();
                                for (int i = 0; i < nd; i++) {
                                    Context dm = it.next();
                                    if (i == index - nr - nc) {
                                        return dm;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (parent instanceof Object[]) {
                Object[] arr = (Object[]) parent;
                ResourcesContext c = (ResourcesContext) arr[0];

                if (c.getResourceChainModel((String) arr[1], false) != null) {
                    List<String> l = getChainNodes(c, (String) arr[1]);
                    return new Object[]{(String) arr[1], l.get(index), c};
                }
            }

            return null;
        } catch (ContextException e) {
            throw new RuntimeException("ContextException occured", e);
        }
    }

    List<String> getChainNodes(ResourcesContext ctx, String name) throws ContextException {
        List<String> res = new ArrayList<String>();
        ResourceChainModel resourceChainModel = ctx.getResourceChainModel(name, true);
        for (int i = 0; i < resourceChainModel.size(); i++) {
            ResourceChainNode nd = resourceChainModel.get(i);
            ResourceModel mdl = nd.getResourceModel();
            res.add(ContextUtil.fullName(mdl.getInitialContext(), mdl.getName()));
        }
        return res;
    }

    public int getChildCount(Object parent) {
        try {
            if (parent instanceof ResourcesContext) {
                ResourcesContext c = (ResourcesContext) parent;
                if (ancestorsOnly) {
                    if (parent.equals(targetContext))
                        return c.getResourcesCount() + c.getResourceChainsCount();
                    return c.getResourcesCount() + c.getResourceChainsCount() + (c.getChildCount() == 0 ? 0 : 1);
                } else {
                    return c.getChildCount() + c.getResourcesCount() + c.getResourceChainsCount();
                }
            } else if (parent instanceof Object[]) {
                Object[] arr = (Object[]) parent;
                if (arr.length == 2) {
                    ResourcesContext c = (ResourcesContext) arr[0];

                    if (c.getResourceChainModel((String) arr[1], false) != null) {
                        return getChainNodes(c, (String) arr[1]).size();
                    }
                }
            }
            return 0;
        } catch (ContextException e) {
            throw new RuntimeException("ContextException occured", e);
        }
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        try {
            if (parent instanceof ResourcesContext) {
                ResourcesContext c = (ResourcesContext) parent;
                int i = 0;
                if (child instanceof Object[]) {
                    Iterator<String> it = c.resourcesIterator();
                    while (it.hasNext()) {
                        String s = it.next();
                        if (s.equals(((Object[]) child)[1]))
                            return i;
                        i++;
                    }
                    it = c.resourceChainsIterator();
                    while (it.hasNext()) {
                        String s = it.next();
                        if (s.equals(((Object[]) child)[1]))
                            return i;
                        i++;
                    }
                } else {
                    i = c.getResourcesCount() + c.getResourceChainsCount();
                    if (ancestorsOnly) {
                        return i;
                    } else {
                        Iterator<Context> it = c.childIterator();
                        while (it.hasNext()) {
                            Context ctx = it.next();
                            if (ctx.equals(child))
                                return i;
                            i++;
                        }
                    }
                }
            } else if (parent instanceof Object[]) {
                Object[] arr = (Object[]) parent;
                ResourcesContext c = (ResourcesContext) arr[0];

                if (c.getResourceChainModel((String) arr[1], false) != null) {
                    List<String> l = getChainNodes(c, (String) arr[1]);
                    //noinspection RedundantCast
                    return l.indexOf((String) arr[1]);
                }
            }
            return -1;
        } catch (ContextException e) {
            throw new RuntimeException("ContextException occured", e);
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }

    public boolean isAncestorsOnly() {
        return ancestorsOnly;
    }

    public void setAncestorsOnly(boolean ancestorsOnly) {
        this.ancestorsOnly = ancestorsOnly;
    }

    public ResourcesContext getTargetContext() {
        return targetContext;
    }

    public void setTargetContext(ResourcesContext targetContext) {
        this.targetContext = targetContext;
    }

    public Object[] pathForObject(Object o) throws ContextException {
        while (o != null) {
            stack.push(o);
            if (o instanceof ResourcesContext) {
                o = ((ResourcesContext) o).getParent();
            } else {
                o = ((Object[]) o)[0];
            }
        }
        Object[] arr = new Object[stack.size()];
        int i = 0;
        while (!stack.isEmpty()) {
            arr[i++] = stack.pop();
        }
        return arr;
    }
}
