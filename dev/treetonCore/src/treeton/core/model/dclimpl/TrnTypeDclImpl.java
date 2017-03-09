/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.TString;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TrnTypeDclImpl extends TrnType {
    TrnTypeStorageDclImpl storage;
    boolean autoFill;
    Class<TString> defaultAutoFillType;
    int index;
    HashMap<String, ValidationTree> hierarchies; //{name}->{ValidationTree}+
    ValidationTree mainHierarchy;
    IntroductionBlock introduction;
    TrnTypeDclImpl basisType;
    private String name;
    private boolean tokenType;
    private HashMap<String, Integer> features;
    private ArrayList<String> featureNames;
    private ArrayList<Class> featureTypes;
    private Color clr;

    TrnTypeDclImpl(TrnTypeStorageDclImpl storage, TrnTypeDclImpl proto, TrnTypeDclImpl basisType) throws TreetonModelException {
        this.basisType = basisType;

        this.storage = storage;
        name = proto.name;
        autoFill = proto.autoFill;
        defaultAutoFillType = proto.defaultAutoFillType;
        tokenType = proto.tokenType;
        index = proto.index;
        if (proto.features != null) {
            features = (HashMap<String, Integer>) proto.features.clone();
            featureNames = (ArrayList<String>) proto.featureNames.clone();
            featureTypes = (ArrayList<Class>) proto.featureTypes.clone();
        } else {
            featureNames = new ArrayList<String>();
            featureTypes = new ArrayList<Class>();
        }
        clr = proto.clr;
        if (proto.introduction != null) {
            introduction = new IntroductionBlock(this);
            IntroductionBlock.copyIntroductionBlock(introduction, proto.introduction);
        }
        hierarchies = new HashMap<String, ValidationTree>();
        for (Map.Entry<String, ValidationTree> e : proto.hierarchies.entrySet()) {
            ValidationTree ntree = new ValidationTree(this, basisType);
            ntree.name = e.getValue().name;
            ntree.root = TrnValue.copyValidationTree(e.getValue().root, this, -1, true);
            hierarchies.put(e.getKey(), ntree);

            if (e.getValue() == proto.mainHierarchy) {
                mainHierarchy = ntree;
            }
        }
    }

    public TrnTypeDclImpl(TrnTypeStorageDclImpl storage, String s, int i, String[] fNames, Class[] fTypes, boolean auto, boolean token, Color c) {
        this.storage = storage;
        name = s;
        index = i;
        autoFill = auto;
        defaultAutoFillType = TString.class;
        tokenType = token;
        featureNames = new ArrayList<String>();
        featureTypes = new ArrayList<Class>();
        if (fNames != null) {
            features = new HashMap<String, Integer>();
            for (int j = 0; j < fNames.length; j++) {
                features.put(fNames[j], featureNames.size() + TrnType.getNumberOfSystemFeatures());
                featureNames.add(fNames[j]);
                featureTypes.add(fTypes[j]);
            }
        } else {
            features = null;
        }
        clr = c;
        hierarchies = new HashMap<String, ValidationTree>();
        mainHierarchy = null;
    }

    public TrnTypeStorageDclImpl getStorage() {
        return storage;
    }

    void setFeatureType(int index, Class type) {
        if (index < TrnType.getNumberOfSystemFeatures()) {
            return;
        }
        featureTypes.set(index - TrnType.getNumberOfSystemFeatures(), type);
    }

    protected int _getFeatureIndex(String t) {
        if (features == null) {
            if (!autoFill) {
                return -1;
            } else {
                features = new HashMap<String, Integer>();
                features.put(t, featureNames.size() + TrnType.getNumberOfSystemFeatures());
                featureNames.add(t);
                featureTypes.add(defaultAutoFillType);
                return TrnType.getNumberOfSystemFeatures();
            }
        } else {
            Integer i = features.get(t);
            if (!autoFill) {
                return i == null ? -1 : i;
            } else {
                if (i == null) {
                    features.put(t, i = (featureNames.size() + TrnType.getNumberOfSystemFeatures()));
                    featureNames.add(t);
                    featureTypes.add(defaultAutoFillType);
                }
                return i;
            }
        }
    }

    public int registerFeature(String nm, Class featureType) {
        if (!autoFill)
            return -1;
        if (features == null) {
            features = new HashMap<String, Integer>();
            features.put(nm, featureNames.size() + TrnType.getNumberOfSystemFeatures());
            featureNames.add(nm);
            featureTypes.add(featureType);
            return TrnType.getNumberOfSystemFeatures();
        } else {
            Integer i = features.get(nm);
            if (i == null) {
                features.put(nm, i = (featureNames.size() + TrnType.getNumberOfSystemFeatures()));
                featureNames.add(nm);
                featureTypes.add(featureType);
            }
            return i;
        }
    }

    protected Class _getFeatureType(String t) {
        int i = _getFeatureIndex(t);
        if (i == -1) {
            return null;
        }
        return featureTypes.get(i - TrnType.getNumberOfSystemFeatures());
    }

    protected String _getFeatureNameByIndex(int i) {
        if (i < TrnType.getNumberOfSystemFeatures() || i >= featureNames.size() + TrnType.getNumberOfSystemFeatures())
            return null;
        return featureNames.get(i - TrnType.getNumberOfSystemFeatures());
    }

    protected Class _getFeatureTypeByIndex(int i) {
        if (i < TrnType.getNumberOfSystemFeatures() || i >= featureTypes.size() + TrnType.getNumberOfSystemFeatures())
            return null;
        return featureTypes.get(i - TrnType.getNumberOfSystemFeatures());
    }

    protected int _getFeaturesSize() {
        return features == null ? 0 : features.size();
    }

    public String getName() {
        return name;
    }

    public boolean isTokenType() {
        return tokenType;
    }

    public boolean isAutofillable() {
        return autoFill;
    }

    public int getIndex() {
        return index;
    }

    public Color getColor() {
        return clr;
    }

    protected void fillFeatureNames(String[] arr, int offset) {
        for (int i = offset, k = 0; i < arr.length && k < featureNames.size(); i++, k++) {
            arr[i] = featureNames.get(k);
        }
    }

    protected void fillFeatureTypes(Class[] arr, int offset) {
        for (int i = offset, k = 0; i < arr.length && k < featureTypes.size(); i++, k++) {
            arr[i] = featureTypes.get(k);
        }
    }

    public Object getRoot() {
        return (hierarchies.values().iterator().next()).root;
    }

    public int getChildCount(Object parent) {
        if (parent instanceof TrnValue) {
            return ((TrnValue) parent).getChildCount();
        } else {
            return ((TrnFeature) parent).getChildCount();
        }
    }

    public boolean isLeaf(Object node) {
        if (node instanceof TrnValue) {
            return ((TrnValue) node).isLeaf();
        } else {
            return ((TrnFeature) node).isLeaf();
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeTreeModelListener(TreeModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof TrnValue) {
            return ((TrnValue) parent).getChild(index);
        } else {
            return ((TrnFeature) parent).getChild(index);
        }
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof TrnValue) {
            return ((TrnValue) parent).getIndexOfChild(child);
        } else {
            return ((TrnFeature) parent).getIndexOfChild(child);
        }
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ValidationTree getHierarchy(String name) {
        return hierarchies.get(name);
    }

    public ValidationTree getHierarchy() {
        return mainHierarchy;
    }

    public boolean isValueDeclared(int featureIndex, Object value) {
        if (introduction == null) {
            throw new RuntimeException("No introduction block provided");
        }
        TrnFeatureInIntroduction f = introduction.getFeature(featureIndex);
        if (f == null) {
            throw new RuntimeException("featureIndex is invalid");
        }
        return f.values.containsKey(value);
    }

    public MarkInIntroduction getDeclaredMark(String name) {
        if (introduction == null) {
            throw new RuntimeException("No introduction block provided");
        }
        return introduction.marks.get(name);
    }

    public String toString() {
        return name;
    }

    public IntroductionBlock getIntroduction() {  //pjalybin 17.11.05
        return introduction;
    }

    public TrnTypeDclImpl getBasisType() {
        return basisType;
    }

    public ValidationTree[] getAllHierarchies()          //pjalybin 18.11.05
    {
        if (hierarchies != null) {
            ValidationTree[] res = new ValidationTree[hierarchies.values().size()];
            hierarchies.values().toArray(res);
            return res;
        } else return null;
    }

    public void disableAutofill() {
        autoFill = false;
    }
}
