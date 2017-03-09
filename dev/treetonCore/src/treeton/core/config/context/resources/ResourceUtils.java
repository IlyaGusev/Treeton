/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Этот класс реализует все функции фабрики ресурсов. Он работает только с методами, вынесенными в интерфейсы моделей,
 * поэтому пригоден для использования с любой реализацией моделей ресурсов, цепочек ресурсов и т.д.
 * В случае, если понадобится дополнительная кастомизация, он может быть использован в качестве основы.
 */

public class ResourceUtils implements ResourceFactory {
    private static Object safeGet(Map<String, Object> params, String name) {
        if (params == null) {
            return null;
        }
        return params.get(name);
    }

    public Resource createResource(ResourceModel model) throws ContextException {
        try {
            ResourceType tp = model.getType();
            if (tp == null) {
                throw new ResourceInstantiationException("Unable to find resource type for " + model.getName());
            }

            Resource res;
            try {
                res = (Resource) tp.getResourceClass().newInstance();
            } catch (InstantiationException e1) {
                throw new ResourceInstantiationException("Wrong resource class: " + tp.getResourceClass());
            } catch (IllegalAccessException e1) {
                throw new ResourceInstantiationException("Wrong resource class: " + tp.getResourceClass());
            }

            res.setResourceModel(model);
            Map<String, Object> parameters = model.getInitialParameters();
            validateParams(tp.getSignature(), parameters);
            res.setInitialParameters(parameters);

            return res;
        } catch (ResourceInstantiationException e1) {
            throw new ContextException("Problem occured during creating the resource " + model.getName(), e1);
        } catch (WrongParametersException e1) {
            throw new ContextException("Problem occured during creating the resource " + model.getName(), e1);
        }
    }

    public Resource createResource(ResourcesContext context, String name) throws ContextException {
        ResourceModel model = context.getResourceModel(name, true);

        return createResource(model);
    }

    public ResourceChain createResourceChain(ResourceChainModel model) {
        ResourceChain resourceChain = new ResourceChain(this);
        resourceChain.setResourceModel(model);
        return resourceChain;
    }

    public ResourceChain createResourceChain(ResourcesContext context, String name) throws ContextException {
        ResourceChainModel model = context.getResourceChainModel(name, true);

        return createResourceChain(model);
    }

    public void validateParams(ResourceSignature signature, Map<String, Object> params) throws WrongParametersException {
        for (ParamDescription descr : signature) {
            Object val = safeGet(params, descr.getName());
            if (val == null) {
                if (!descr.isOptional()) {
                    throw new WrongParametersException("Missing non-optional parameter " + descr.getName());
                }
            } else {
                if (descr.isManyValued()) {
                    List<Object> valList;
                    if (!(val instanceof List)) {
                        valList = new ArrayList<Object>();
                        valList.add(val);
                        params.put(descr.getName(), valList);
                    } else {
                        //noinspection unchecked
                        valList = (List<Object>) val;
                    }
                    for (int j = 0; j < valList.size(); j++) {
                        Object o = valList.get(j);
                        if (descr.getType() == Integer.class && !(o instanceof Integer)) {
                            try {
                                Integer i = Integer.valueOf((String) o);
                                valList.set(j, i);
                            } catch (Exception e) {
                                throw new WrongParametersException("Value " + o + " of the component " + j + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                            }
                        } else if (descr.getType() == Long.class && !(o instanceof Long)) {
                            try {
                                Long l = Long.valueOf((String) o);
                                valList.set(j, l);
                            } catch (Exception e) {
                                throw new WrongParametersException("Value " + o + " of the component " + j + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                            }
                        } else if (descr.getType() == Boolean.class && !(o instanceof Boolean)) {
                            try {
                                Boolean b = Boolean.valueOf((String) o);
                                valList.set(j, b);
                            } catch (Exception e) {
                                throw new WrongParametersException("Value " + o + " of the component " + j + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                            }
                        } else if (descr.getType() == String.class && !(o instanceof String)) {
                            try {
                                String s = o.toString();
                                valList.set(j, s);
                            } catch (Exception e) {
                                throw new WrongParametersException("Value " + o + " of the component " + j + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                            }
                        } else if (descr.getType() == URI.class && !(o instanceof URI)) {
                            try {
                                URI u = new URI(o.toString());
                                valList.set(j, u);
                            } catch (Exception e) {
                                throw new WrongParametersException("Value " + o + " of the component " + j + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                            }
                        }
                    }
                } else {
                    if (descr.getType() == Integer.class && !(val instanceof Integer)) {
                        try {
                            Integer i = Integer.valueOf((String) val);
                            params.put(descr.getName(), i);
                        } catch (Exception e) {
                            throw new WrongParametersException("Value " + val + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                        }
                    } else if (descr.getType() == Long.class && !(val instanceof Long)) {
                        try {
                            Long l = Long.valueOf((String) val);
                            params.put(descr.getName(), l);
                        } catch (Exception e) {
                            throw new WrongParametersException("Value " + val + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                        }
                    } else if (descr.getType() == Boolean.class && !(val instanceof Boolean)) {
                        try {
                            Boolean b = Boolean.valueOf((String) val);
                            params.put(descr.getName(), b);
                        } catch (Exception e) {
                            throw new WrongParametersException("Value " + val + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                        }
                    } else if (descr.getType() == String.class && !(val instanceof String)) {
                        try {
                            String s = val.toString();
                            params.put(descr.getName(), s);
                        } catch (Exception e) {
                            throw new WrongParametersException("Value " + val + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                        }
                    } else if (descr.getType() == URI.class && !(val instanceof URI)) {
                        try {
                            URI u = new URI(val.toString());
                            params.put(descr.getName(), u);
                        } catch (Exception e) {
                            throw new WrongParametersException("Value " + val + " of the " + descr.getName() + " parameter could not be cast to " + descr.getType());
                        }
                    }
                }
            }
        }
    }

}
