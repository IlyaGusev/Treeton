/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

public interface LogListener {
    public void info(String s);

    public void error(String s, Throwable e);
}
