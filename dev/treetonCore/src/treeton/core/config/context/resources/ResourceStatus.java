/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

/**
 * Перечислимый тип, описывающий возможные состояния ресурсов
 */

public enum ResourceStatus {

    /**
     * Не инициализирован
     */

    NOT_INITIALIZED,

    /**
     * В данный момент инициализируется
     */
    INITIALIZING,

    /**
     * Готов к обработке текста
     */
    READY_TO_WORK,

    /**
     * В данный момент обрабатывает текст
     */
    WORKING,

    /**
     * В данный момент останавливается
     */
    STOPPING,

    /**
     * В данный момент деинициализируется
     */
    DEINITIALIZING
}
