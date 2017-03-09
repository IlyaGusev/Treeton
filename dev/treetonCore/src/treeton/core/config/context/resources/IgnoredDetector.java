/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

/**
 * Служебный интерфейс, используемый классом {@link treeton.core.config.context.resources.ResourceChain}. С его помощью
 * можно блокировать запуск определенных ресурсов в процессе выполнения цепочки ресурсов.
 */

public interface IgnoredDetector {
    public boolean isIgnored(Resource res);
}
