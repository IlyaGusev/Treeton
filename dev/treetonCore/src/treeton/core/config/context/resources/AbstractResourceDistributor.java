/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceDistributorModel;
import treeton.core.config.context.resources.api.ResourceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Этот класс реализует базовые функции ресурса-распределителя: инициализация (построение по модели) и деинициализация.
 * Функция применения к тексту оставлена для наследников класса в виде абстрактного метода. Класс может быть использован
 * в качестве основы при реализации полноценного ресурса-распределителя. Для реализации функции применения к тексту
 * наследнику доступно отображение resources (со строки-условия на ресурс).
 * <p>
 * Класс позволяет работать с объектом, реализующим интерфейс {@link treeton.core.config.context.resources.IgnoredDetector}.
 * что позволяет исключать некоторые ресурсы из цепочки, не удаляя их на совсем.
 * <p>
 * Класс позволяет добавлять специальных слушателей для логирования ({@link LogListener}). При инициализации
 * распределителя они оповещаются о том, какие конкретно ресурсы инициализируются.
 */

public abstract class AbstractResourceDistributor extends Resource {
    protected Map<String, Resource> resources = new HashMap<String, Resource>();
    protected IgnoredDetector ignoredDetector;
    protected Resource current;
    protected boolean stopping = false;
    ArrayList<LogListener> listeners = new ArrayList<LogListener>();
    private ResourceFactory factory;

    public AbstractResourceDistributor(ResourceFactory factory) {
        this.factory = factory;
        initialParams = new HashMap<String, Object>();
    }

    protected final void LogMemoryStat() {
        long total = Runtime.getRuntime().totalMemory();
        long used = total - Runtime.getRuntime().freeMemory();

        notifyLogListeners(Long.toString(used >> 20) + "Mb / " + Long.toString(total >> 20) + "Mb");
    }

    public boolean isIgnored(Resource r) {
        return ignoredDetector != null && ignoredDetector.isIgnored(r);
    }

    protected void init() throws ResourceInstantiationException {
        ResourceDistributorModel model = (ResourceDistributorModel) this.model;

        try {
            for (String s : model.listConditions()) {
                ResourceChainNode nd = model.get(s);
                ResourceModel resourceModel = nd.getResourceModel();

                long time = System.currentTimeMillis();
                notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.AbstractResourceDistributor.AddingResource") + " " + safeGetName(resourceModel) + "...");
                readResource(resourceModel, nd.passParameters() ? initialParams : null);
                LogMemoryStat();
                notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.AbstractResourceDistributor.Finished") + ". " + Double.toString((System.currentTimeMillis() - time) / 1000.0) + "s");
                resources.put(s, current);

                synchronized (this) {
                    if (stopping) {
                        current = null;
                        return;
                    }
                    current = null;
                }
            }
        } catch (ContextException e) {
            throw new ResourceInstantiationException("Context problem", e);
        }

        synchronized (this) {
            if (stopping) {
                deInit();
            }
        }
    }

    private void readResource(ResourceModel model, Map<String, Object> params) throws ContextException, ResourceInstantiationException {
        synchronized (this) {
            current = model instanceof ResourceChainModel ? factory.createResourceChain((ResourceChainModel) model) : factory.createResource(model);
        }
        if (params != null) {
            current.getInitialParameters().putAll(params);
        }
        current.setProgressListener(getProgressListener());
        current.initialize(getTrnContext());
        current.setProgressListener(null);
    }

    public final void deInit() {
        for (Resource r : resources.values()) {
            notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.DeinitializingResource") + " " + safeGetName(r) + "...");
            r.deInitialize();
            LogMemoryStat();
            notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.Finished"));
        }
        resources.clear();
        current = null;
    }

    protected final synchronized void stop() {
        stopping = true;
        if (current != null) {
            notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.TryingToStopResource") + " " + safeGetName(current) + "...");
            current.startStopping();
        }
    }

    private String safeGetName(ResourceModel model) {
        try {
            return model.getName();
        } catch (ContextException e) {
            return "Exception during getting name!";
        }
    }

    private String safeGetName(Resource res) {
        try {
            return res.getName();
        } catch (ContextException e) {
            return "Exception during getting name";
        }
    }

    protected final synchronized void processTerminated() {
        if (current != null) {
            notifyLogListeners(
                    BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.CallingProcessTerminatedInsideThe") + " " + safeGetName(current) + " ...");
            current.recoverAfterProcessTermination();
        }
        if (getStatus() == ResourceStatus.INITIALIZING) {
            for (Resource r : resources.values()) {
                r.deInitialize();
            }
            resources.clear();
        }
    }

    public final int getNumberOfResources() {
        return resources.size();
    }

    public final boolean containsResource(Resource res) {
        return resources.containsValue(res);
    }

    public final void addLogListener(LogListener l) {
        listeners.add(l);
    }

    public final void removeLogListener(LogListener l) {
        listeners.remove(l);
    }

    protected void notifyLogListeners(String message) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listeners.size(); i++) {
            LogListener logListener = listeners.get(i);
            logListener.info(message);
        }
    }

    public final void setIgnoredDetector(IgnoredDetector detector) {
        ignoredDetector = detector;
    }

    public Resource get(String condition) {
        return resources.get(condition);
    }

    public abstract String process(String text, TextMarkingStorage storage, Map<String, Object> runtimeParameters) throws ExecutionException;
}
