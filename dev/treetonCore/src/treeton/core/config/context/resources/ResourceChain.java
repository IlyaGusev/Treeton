/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Класс реализует понятие цепочки ресурсов обработки текстов. В нем реализованы все абстрактные методы класса
 * {@link Resource}, поэтому с цепочкой ресурсов можно работать как с любым другим ресурсом обработки текстов (она
 * инициализируется, применяется к текстам, деинициализируется, может быть остановлена). Инициализируется цепочка
 * в соответствии с моделью цепочки ({@link treeton.core.config.context.resources.api.ResourceChainModel}).
 * <p>
 * На уровне моделей система построена таким образом, что цепочки ресурсов могут ссылаться как на конкретные
 * ресурсы, так и на другие цепочки ресурсов. При инициализации конкретного экземпляра цепочки ресурсов производится
 * последовательность подстановок (на место ссылки на каждую цепочку подставляется содержимое цепочки). Таким образом,
 * инициализированная цепочка гарантировано имеет плоскую структуру (просто последовательность ресурсов). Если в
 * процессе подстановок обнаруживается цикл, бросается исключение
 * {@link treeton.core.config.context.resources.ResourceInstantiationException}.
 * <p>
 * Дополнительно этот класс предоставляет возможности по манипулированию ресурсами внутри цепочки уже после
 * инициализации (добавление/удаление ресурса, возможность поменять ресурсы местами). Кроме того, есть возможность
 * узнавать, какой процент ресурсов уже был завершен в процессе работы цепочки. Еще одной возможностью является
 * передача в этот класс объекта, реализующего интерфейс {@link treeton.core.config.context.resources.IgnoredDetector},
 * что позволяет исключать некоторые ресурсы из цепочки, не удаляя их на совсем.
 * <p>
 * Класс позволяет добавлять специальных слушателей для логирования ({@link LogListener}). При инициализации
 * (выполнении) цепочки они оповещаются о том, какие конкретно ресурсы инициализируются (выполняются).
 */

public class ResourceChain extends Resource {
    protected ArrayList<Resource> resources = new ArrayList<Resource>();
    protected IgnoredDetector ignoredDetector;
    protected Resource current;
    protected double progress = 0.0;
    protected boolean stopping = false;
    ArrayList<LogListener> listeners = new ArrayList<LogListener>();
    boolean ready = false;
    String name;
    private ResourceFactory factory;

    public ResourceChain(ResourceFactory factory) {
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
        init(new HashSet<ResourceChainModel>());
    }

    private void init(HashSet<ResourceChainModel> passedAlready) throws ResourceInstantiationException {
        passedAlready.add((ResourceChainModel) model);
        addResources((ResourceChainModel) model, passedAlready, initialParams);
        synchronized (this) {
            if (stopping) {
                deInit();
            }
        }
        //noinspection SuspiciousMethodCalls
        passedAlready.remove(model);
    }

    private void addResources(ResourceChainModel model, HashSet<ResourceChainModel> passedAlready, Map<String, Object> params) throws ResourceInstantiationException {
        try {
            for (int i = 0; i < model.size(); i++) {
                ResourceChainNode nd = model.get(i);
                ResourceModel resourceModel = nd.getResourceModel();
                if (resourceModel instanceof ResourceChainModel) {
                    readChain((ResourceChainModel) resourceModel, params, passedAlready);
                } else {
                    long time = System.currentTimeMillis();
                    notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.AddingResource") + " " + safeGetName(resourceModel) + "...");
                    readResource(resourceModel, nd.passParameters() ? params : null);
                    LogMemoryStat();
                    notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.Finished") + ". " + Double.toString((System.currentTimeMillis() - time) / 1000.0) + "s");
                }

                synchronized (this) {
                    if (stopping) {
                        current = null;
                        return;
                    }
                    current = null;
                }
            }
        } catch (ResourceInstantiationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResourceInstantiationException(getName(), "Exception during resourceChain instantiation: " + ex.getMessage(), ex);
        }
    }

    private void readResource(ResourceModel model, Map<String, Object> params) throws ContextException, ResourceInstantiationException {
        synchronized (this) {
            current = factory.createResource(model);
        }
        if (params != null) {
            current.getInitialParameters().putAll(params);
        }
        current.setProgressListener(getProgressListener());
        current.initialize(getTrnContext());
        current.setProgressListener(null);
        resources.add(current);
    }

    private void readChain(ResourceChainModel model, Map<String, Object> params, HashSet<ResourceChainModel> passedAlready) throws ResourceInstantiationException, ContextException {
        if (passedAlready.contains(model)) {
            throw new ResourceInstantiationException(null, "Loop located during ResourceChain instantiation (chain " + safeGetName(model) + ")");
        }
        passedAlready.add(model);
        addResources(model, passedAlready, params);
        passedAlready.remove(model);
    }

    private String safeGetName(Resource res) {
        try {
            return res.getName();
        } catch (ContextException e) {
            return "Exception during getting name";
        }
    }

    public final void deInit() {
        for (Resource r : resources) {
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

    protected final synchronized void processTerminated() {
        if (current != null) {
            notifyLogListeners(
                    BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.CallingProcessTerminatedInsideThe") + " " + safeGetName(current) + " ...");
            current.recoverAfterProcessTermination();
        }
        if (getStatus() == ResourceStatus.INITIALIZING) {
            for (Resource r : resources) {
                r.deInitialize();
            }
            resources.clear();
        }
    }

    public final double getProgress() {
        return progress;
    }

    public final Resource getResource(int index) {
        return resources.get(index);
    }

    public final int getNumberOfResources() {
        return resources.size();
    }

    public final int getNumberOfResource(Resource res) {
        return resources.indexOf(res);
    }

    public final boolean containsResource(Resource res) {
        return resources.contains(res);
    }

    public final void removeResource(Resource res) {
        resources.remove(res);
    }

    public final void addResource(Resource res) {
        resources.add(res);
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

    public final int indexOfResource(Resource res) {
        return resources.indexOf(res);
    }

    public final void setIgnoredDetector(IgnoredDetector detector) {
        ignoredDetector = detector;
    }

    public final void clearProgress() {
        progress = 0;
    }

    protected final String process(String text, TextMarkingStorage storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        try {
            String result = null;
            int i = 0;
            stopping = false;
            for (Resource r : resources) {
                i++;
                if (isIgnored(r)) {
                    continue;
                }
                progress = (i * 100.0) / (double) (resources.size() + 1);
                synchronized (this) {
                    current = r;
                }
                long time = System.currentTimeMillis();
                notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.StartedResource") + " " + safeGetName(r) + "...");
                r.setProgressListener(plistener);
                result = r.execute(text, storage, runtimeParameters);
                r.setProgressListener(null);
                LogMemoryStat();
                notifyLogListeners(BasicConfiguration.localize("BasicBundle", "treeton.core.config.context.resources.ResourceChain.FinishedResource") + " " + safeGetName(r) + ". " + Double.toString((System.currentTimeMillis() - time) / 1000.0) + "s");
                if (Thread.currentThread().isInterrupted())
                    throw new ExecutionException("Chain thread was interrupted");
                synchronized (this) {
                    if (stopping) {
                        current = null;
                        stopping = false;
                        return null;
                    }
                }
            }
            progress = 100.0;
            synchronized (this) {
                current = null;
            }
            return result;
        } catch (ExecutionException e) {
            synchronized (this) {
                current = null;
            }
            throw e;
        } finally {
            synchronized (this) {
                current = null;
            }
        }
    }

    public void swapResources(Resource res1, Resource res2) {
        int k = resources.indexOf(res1);
        int l = resources.indexOf(res2);
        resources.set(k, res2);
        resources.set(l, res1);
    }

    public void addResourceFirst(Resource res) {
        if (resources.size() == 0) {
            resources.add(res);
            return;
        }
        resources.add(resources.get(resources.size() - 1));
        for (int i = resources.size() - 3; i >= 0; i--) {
            resources.set(i + 1, resources.get(i));
        }
        resources.set(0, res);

    }

    public String getName() {
        if (name == null) {
            return safeGetName(model);
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String safeGetName(ResourceModel model) {
        try {
            return model.getName();
        } catch (ContextException e) {
            return "Exception during getting name!";
        }
    }
}
