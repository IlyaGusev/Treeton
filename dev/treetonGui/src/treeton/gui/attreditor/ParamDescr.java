/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.context.resources.api.ParamDescription;

import java.net.URI;
import java.util.LinkedList;

public class ParamDescr {
    LinkedList<Object> pValue;
    Boolean pMultiple;
    Boolean pEditable;
    Boolean pOptional;
    String pType;


    public ParamDescr(Object pValue, Boolean pMultiple, String pType, Boolean pOptional) {
        super();
        this.pValue = new LinkedList<Object>();
        this.pEditable = true;
        this.pOptional = pOptional;
        if (pValue instanceof java.util.LinkedList) {
            for (Object o : (LinkedList) pValue) {
                this.pValue.add(o);
            }
        } else this.pValue.add(pValue);
        this.pMultiple = pMultiple;
        this.pType = pType;
    }

    public ParamDescr() {
        super();
        this.pEditable = true;
        this.pValue = new LinkedList<Object>();
    }

    public ParamDescr(ParamDescription pd) {
        super();
        this.pMultiple = pd.isManyValued();
        this.pValue = new LinkedList<Object>();
        if (pd.getType() == String.class) {
            this.pType = "String";
            this.pValue.add("");
        } else if (pd.getType() == Integer.class) {
            this.pType = "Integer";
            this.pValue.add(0);
        } else if (pd.getType() == Long.class) {
            this.pType = "Long";
            this.pValue.add(0);
        } else if (pd.getType() == Boolean.class) {
            this.pType = "Boolean";
            this.pValue.add(false);
        } else if (pd.getType() == URI.class) {
            this.pType = "URI";
            this.pValue.add(false);
        }
    }

    public ParamDescr(ParamDescription pd, Object val) {
        super();
        this.pMultiple = pd.isManyValued();
        this.pValue = new LinkedList<Object>();
        if (pd.getType() == String.class) {
            this.pType = "String";
            if (val != null) {
                if (pd.isManyValued()) {
                    this.pValue = (LinkedList<Object>) val;
                } else this.pValue.add(val);
            } else this.pValue.add("");
        } else if (pd.getType() == Integer.class) {
            this.pType = "Integer";
            if (val != null) {
                if (pd.isManyValued()) {
                    this.pValue = (LinkedList<Object>) val;
                } else this.pValue.add(val);
            } else this.pValue.add(0);
        } else if (pd.getType() == Long.class) {
            this.pType = "Long";
            if (val != null) {
                if (pd.isManyValued()) {
                    this.pValue = (LinkedList<Object>) val;
                } else this.pValue.add(val);
            } else this.pValue.add(0);
        } else if (pd.getType() == Boolean.class) {
            if (val != null) {
                if (pd.isManyValued()) {
                    this.pValue = (LinkedList<Object>) val;
                } else this.pValue.add(val);
            } else this.pValue.add(false);
            this.pType = "Boolean";
        }
    }
}
