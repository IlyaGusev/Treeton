/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.context.resources.api.ParamDescription;
import treeton.core.config.context.resources.api.ResourceSignature;

import java.util.*;

public class AttrStorage extends HashMap<String, ParamDescr> implements Comparator {
    ResourceSignature res;

    public LinkedList<TableElement> getList() {
        LinkedList<TableElement> list = new LinkedList<TableElement>();
        Set<Map.Entry<String, ParamDescr>> entries = this.entrySet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, ParamDescr> entry = (Map.Entry<String, ParamDescr>) it.next();
            LinkedList<Object> val;
            val = entry.getValue().pValue;
            for (int i = 0; i < val.size(); i++) {
                Object v = val.get(i);
                TableElement el = new TableElement();
                el.pName = entry.getKey();
                if (i == 0) {
                    el.isParent = true;
                } else el.isParent = false;
                el.pType = entry.getValue().pType;
                el.isEditable = entry.getValue().pEditable;
                Iterator<ParamDescription> ik = res.iterator();
                while (ik.hasNext()) {
                    ParamDescription pd = ik.next();
                    if (pd.getName().equals(el.pName)) {
                        el.isEditable = false;
                    }
                }
                el.pMultiple = entry.getValue().pMultiple;
                el.isOptional = entry.getValue().pOptional;
                el.pValue = v;
                list.addLast(el);
            }
        }
        Collections.sort(list, new AttrStorage());
        return list;
    }

    public int compare(Object o1, Object o2) {
        TableElement l1, l2;
        l1 = (TableElement) o1;
        l2 = (TableElement) o2;
        if ((l1.isEditable && l2.isEditable) || (!l1.isEditable && !l2.isEditable)) {
            if (l1.isOptional && l2.isOptional || l1.isOptional && l2.isOptional) {
                return (l1.pName.compareTo(l2.pName));
            } else if (l1.isOptional && !l2.isOptional) return 1;
            else return -1;
        } else if (l1.isEditable && !l2.isEditable) {
            return 1;
        } else return -1;
    }
//        if (l1.pName.equals(l2.pName)){
//            if(!l1.isParent)return 1;
//            else return -1;
//        } else if (l1.isEditable && !l2.isEditable) {
//            return 1;
//        } else return -1;
//    }

}

