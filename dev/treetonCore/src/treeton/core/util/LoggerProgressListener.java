/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import org.apache.log4j.Logger;

public class LoggerProgressListener implements ProgressListener {
    Logger logger;
    String processName;
    String statusString;

    public LoggerProgressListener(String processName, Logger logger) {
        this.logger = logger;
        this.processName = processName;
    }

    public void progressStarted() {
        logger.info(processName + " started.");
    }

    public void infiniteProgressStarted() {
    }

    public void progressStringChanged(String s) {
        logger.info(s);
    }

    public void statusStringChanged(String s) {
        statusString = s;
        logger.info(statusString);
    }

    public String getStatusString() {
        return statusString;
    }

    public void progressValueChanged(double value) {
    }

    public void progressFinished() {
        logger.info(processName + " finished.");
    }
}
