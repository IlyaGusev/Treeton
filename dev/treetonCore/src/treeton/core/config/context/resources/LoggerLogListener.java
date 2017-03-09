/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import org.apache.log4j.Logger;
import treeton.core.util.Utils;

public class LoggerLogListener implements LogListener {
    Logger logger;

    public LoggerLogListener(Logger logger) {
        this.logger = logger;
    }

    public void info(String s) {
        logger.info(s + " " + Utils.memoryState());
    }

    public void error(String s, Throwable e) {
        logger.error(s, e);
    }
}
