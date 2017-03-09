/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.WrongParametersException;
import treeton.core.config.context.resources.api.ParamDescription;
import treeton.core.config.context.resources.api.ResourceSignature;
import treeton.core.util.xml.XMLParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ResourceSignatureXMLImpl implements ResourceSignature {
    HashMap<String, ParamDescription> params = new HashMap<String, ParamDescription>();

    public ResourceSignatureXMLImpl(Element xmlElement) throws WrongParametersException {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "PARAMDESCRIPTION");
        for (Element e : l) {
            String name = e.getAttribute("NAME");

            if (name == null || name.length() == 0 || !ContextUtil.isWellFormedName(name)) {
                throw new WrongParametersException("Wrong value of the NAME parameter: " + name);
            }
            String tp = e.getAttribute("TYPE");
            Class cls = null;
            if ("Integer".equals(tp)) {
                cls = Integer.class;
            } else if ("Long".equals(tp)) {
                cls = Long.class;
            } else if ("Boolean".equals(tp)) {
                cls = Boolean.class;
            } else if ("String".equals(tp)) {
                cls = String.class;
            }

            if (cls == null) {
                throw new WrongParametersException("Wrong value of the TYPE parameter: " + tp);
            }

            boolean manyValued = "true".equals(e.getAttribute("MANYVALUED"));
            boolean optional = "true".equals(e.getAttribute("OPTIONAL"));

            params.put(name, new ParamDescription(name, cls, manyValued, optional));
        }
    }


    public ParamDescription getParamDescription(String name) {
        return params.get(name);
    }

    public Iterator<ParamDescription> iterator() {
        return params.values().iterator();
    }

}
