/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.api.ResourceModel;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.util.ProgressListener;

import java.text.MessageFormat;
import java.util.Map;

//todo Продумать ситуацию с эксепшенами во время исполнения и инициализации

/**
 * Этот класс описывает абстрактный ресурс обработки текстов (РОТ) с точки зрения его работы в Treeton.
 * Основное назначение РОТ -- обрабатывать текст, преобразуя ассоциированное с ним хранилище метаинформации.
 * <p>
 * Классы, реализующие все РОТ в системе, должны быть наследниками этого класса. В Treeton предусмотрены
 * средства, обеспечивающие корректную работу с такими ресурсами, а именно инициализацию, запуск (однопоточный),
 * остановку и деинициализацию. Создание ресурсов происходит с помощью объекта класса
 * {@link treeton.core.config.context.resources.ResourceUtils}.
 * <p>
 * Любой потомок этого класса обязан иметь публичный пустой конструктор.
 * Любой листовой (не промежуточный) потомок этого класса обязан реализовывать методы:
 * - инициализации
 * - деинициализации
 * - обработки текста
 * - остановки
 * - восстановления после аварийной остановки
 * <p>
 * Cамим классом и классом ResourceUtils обеспечивается поддержка метода getStatus(). В любой момент у ресурса можно
 * спросить его статус (см. {@link treeton.core.config.context.resources.ResourceStatus}).
 * <p>
 * Для реализации абстрактных методов потомкам доступны:
 * <p>
 * 1. Модель создаваемого ресурса
 * 2. Список провалидированных параметров инициализации
 * 3. Контекст модели предметной области, в котором был инициализирован ресурс
 * 4. Контекст ресурсов, которому принадлежит модель ресурса
 * 5. Текущий слушатель прогресса выполнения
 */

public abstract class Resource {
    ResourceModel model;
    TreenotationsContext trnContext;
    Map<String, Object> initialParams;
    ProgressListener plistener;
    ResourceStatus status;

    protected Resource() {
    }

    /**
     * Получить модель, по которой был создан экземпляр ресурса
     *
     * @return модель ресурса
     */

    public ResourceModel getResourceModel() {
        return model;
    }

    /**
     * Установить модель ресурса (вызывается только изнутри Treeton) Не следует использовать
     * этот метод.
     *
     * @param model модель ресурса
     */

    public void setResourceModel(ResourceModel model) {
        this.model = model;
    }

    /**
     * Получить имя экземпляра ресурса. По умолчанию оно совпадает с именем модели ресурса.
     *
     * @return имя ресурса
     */

    public String getName() throws ContextException {
        return model.getName();
    }

    /**
     * Получить контекст ресурсов, к которому относится модель этого ресурса
     *
     * @return контекст ресурсов
     */

    public ResourcesContext getResContext() throws ContextException {
        return model.getInitialContext();
    }

    /**
     * Получить описание типа ресурса, к которому относится модель данного ресурса.
     *
     * @return
     * @throws ContextException
     */

    public ResourceType getType() throws ContextException {
        return model.getType();
    }

    /**
     * Получить текущий слушатель прогресса выполнения.
     *
     * @return слушатель
     */

    public final ProgressListener getProgressListener() {
        return plistener;
    }

    /**
     * Установить слушатель прогресса выполнения
     *
     * @param plistener слушатель
     */

    public final void setProgressListener(ProgressListener plistener) {
        this.plistener = plistener;
    }

    /**
     * Получить контекст предметной области, в котором был инициализирован ресурс
     *
     * @return контекст предметной области
     */

    public TreenotationsContext getTrnContext() {
        return trnContext;
    }

    /**
     * Получить параметры ресурса. Эти параметры совпадают с параметрами, возвращаемыми моделью данного ресурса
     * {@link treeton.core.config.context.resources.api.ResourceModel#getInitialParameters()}, с точностью до изменений,
     * которые могут быть внесены процессом валидации параметров по сигнатуре, заданной для типа данного ресурса
     * (приведение типов, для множественных атрибутов оборочивание единичных объектов в списки).
     *
     * @return отображение с названий параметров на их значения
     */

    public final Map<String, Object> getInitialParameters() {
        return initialParams;
    }

    /**
     * Установить параметры ресурса (вызывается только изнутри Treeton) Не следует использовать
     * этот метод.
     *
     * @param params множество параметров
     */

    public void setInitialParameters(Map<String, Object> params) {
        initialParams = params;
    }

    /**
     * Инициализировать ресурс в некотором контексте предметной области.
     *
     * @param trnContext контекст предметной области
     */

    public final void initialize(TreenotationsContext trnContext) throws ResourceInstantiationException {
        this.trnContext = trnContext;
        status = ResourceStatus.INITIALIZING;
        try {
            String oper = "initialize " + getFullName();
            init();
        } catch (ResourceInstantiationException e) {
            try {
                e.setResourceName(getName());
            } catch (ContextException e1) {
                e.setResourceName("Exception during getting name");
            }
            try {
                deInitialize();
            } catch (Error e1) {
                status = ResourceStatus.NOT_INITIALIZED;
                throw e;
            }
            throw e;
        } catch (Error e) {
            try {
                deInitialize();
            } catch (Error e1) {
                status = ResourceStatus.NOT_INITIALIZED;
                throw e;
            }
            throw e;
        } catch (ContextException e) {
            ResourceInstantiationException ee = new ResourceInstantiationException("Unable to get full name", e);
            try {
                deInitialize();
            } catch (Error e1) {
                status = ResourceStatus.NOT_INITIALIZED;
                throw ee;
            }
            throw ee;
        }
        status = ResourceStatus.READY_TO_WORK;
    }

    /**
     * Деинициализация ресурса. После вызова этого метода не может быть вызвано метод
     * {@link Resource#execute(String, TextMarkingStorage, java.util.Map)} (до тех пор пока не будет повторно вызван
     * метод {@link Resource#initialize(treeton.core.config.context.treenotations.TreenotationsContext)})
     */

    public final void deInitialize() {
        status = ResourceStatus.DEINITIALIZING;
        deInit();
        status = ResourceStatus.NOT_INITIALIZED;
    }

    /**
     * Получить статус ресурса. Список возможных статусов см. здесь {@link treeton.core.config.context.resources.ResourceStatus}.
     *
     * @return статус ресурса
     */

    public ResourceStatus getStatus() {
        return status;
    }

    /**
     * Инициировать процесс остановки. В случае, если ресурс слишком долго работает или инициализируется) можно
     * "попросить" его остановиться.
     */

    public final void startStopping() {
        status = ResourceStatus.STOPPING;
        stop();
    }

    /**
     * Этот меотд следует вызывать в случае, когда ресурс был остановлен некорректно (была убита нить), чтобы дать ему
     * возможность "восстановиться" (очистить временные буферы и т.п.)
     */

    public void recoverAfterProcessTermination() {
        processTerminated();
        status = status == ResourceStatus.INITIALIZING ? ResourceStatus.NOT_INITIALIZED : ResourceStatus.READY_TO_WORK;
    }

    /**
     * Запустить ресурс, т.е. применить его к некоторому тексту и хранилищу метаинформации, ассоциированному с этим
     * текстом. Результатом работы ресурса является строка. Назначение этой строки может быть различным. Многие ресурсы
     * возращают в качестве результата null. Возвращать в качестве результат не null имеет смысл только для ресурсов,
     * про которые известно, что они будут стоять в цепочке ресурсов последними. Такие ресурсы могут вернуть строку,
     * которая будет считаться результатом обработки текста всей цепочкой ранее запущенных ресурсов (например, ресурс
     * сериализующий все хранилище метаинформации или определенный его фрагмент).
     *
     * @param text              обрабатываемый текст
     * @param storage           хранилище метаинформации
     * @param runtimeParameters параметры
     * @return Результат в виде строки
     * @throws ExecutionException все ошибки в процессе запуска оборачиваются этим исключением
     */

    public final String execute(String text, TextMarkingStorage storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        status = ResourceStatus.WORKING;
        try {
            String oper = "execute " + getFullName();
            String res = process(text, storage, runtimeParameters);
            status = ResourceStatus.READY_TO_WORK;
            return res;
        } catch (ExecutionException e) {
            status = ResourceStatus.READY_TO_WORK;
            throw e;
        } catch (Error e) {
            status = ResourceStatus.READY_TO_WORK;
            throw e;
        } catch (ContextException e) {
            status = ResourceStatus.READY_TO_WORK;
            throw new ExecutionException("Unable to get full name", e);
        }
    }

    /**
     * Получить полное имя ресурса в формате "<полное имя контекста предметной области>/
     * <полное имя контекста ресурсов которому принадлежит модель ресурса>/<имя экземпляра ресурса>"
     *
     * @return полное имя ресурса
     */

    public String getFullName() throws ContextException {
        return MessageFormat.format("{0}/{1}/{2}", ContextUtil.getFullName(trnContext), ContextUtil.getFullName(model.getInitialContext()), getName());
    }

    /**
     * Этот метод должен реализовывать логику обработки текста (преобразования хранилища метаинформации). Он вызывается
     * из {@link Resource#execute(String, TextMarkingStorage, java.util.Map)}
     *
     * @param text              обрабатываемый текст
     * @param storage           хранилище метаинформации
     * @param runtimeParameters параметры
     * @return Результат в виде строки
     * @throws ExecutionException все ошибки в процессе должны быть обернуты этим исключением
     */

    protected abstract String process(String text, TextMarkingStorage storage, Map<String, Object> runtimeParameters) throws ExecutionException;

    /**
     * Этот метод должен приводить к остановке ресурса в ближайшее время (в пределах нескольких секунд) после его
     * вызова. Он вызывается из {@link treeton.core.config.context.resources.Resource#startStopping()}. Этот метод
     * может быть нереализован (оставлен пустым). Тогда единственным способом останавливать такой ресурс будет аварийная
     * остановка (убиение нити).
     */

    protected abstract void stop();

    /**
     * Этот метод должен обеспечивать восстановление ресурса после аварийной остановки. Он вызывается из
     * {@link treeton.core.config.context.resources.Resource#recoverAfterProcessTermination()}
     */

    protected abstract void processTerminated();

    /**
     * Этот метод должен рпоизводить инициализацию ресурса, т.е. приведение его в состояние, при котором он может
     * корректно выполнять метод {@link Resource#execute(String, TextMarkingStorage, java.util.Map)}. Он вызывается из
     * {@link Resource#initialize(treeton.core.config.context.treenotations.TreenotationsContext)}
     *
     * @throws ResourceInstantiationException этот эксепшн должен бросаться при возникновении любых проблем во время
     *                                        инициализации
     */

    protected abstract void init() throws ResourceInstantiationException;

    /**
     * Этот метод должен обеспечивать деинициализацию всех структур, необходимых для работы ресурса. Вызывается из
     * {@link treeton.core.config.context.resources.Resource#deInitialize()}
     */

    protected abstract void deInit();
}
