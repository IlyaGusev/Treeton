/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.queueview;

public interface QueueViewerListener {
    public void queueViewChanged(QueueViewerEventType type, Object info, QueueViewerPanel source);
}
