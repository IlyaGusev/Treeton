/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public interface ProgressListener {
    public void progressStarted();

    public void infiniteProgressStarted();

    public void progressStringChanged(String s);

    public void statusStringChanged(String s);

    public String getStatusString();

    public void progressValueChanged(double value);

    public void progressFinished();
}
