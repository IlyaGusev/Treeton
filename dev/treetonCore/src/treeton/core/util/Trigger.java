/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class Trigger {
    boolean dismissed;
    private Condition condition;
    private long refreshRate = 500;
    private TriggerListener listener;

    public long getRefreshRate() {
        return refreshRate;
    }

    public void setRefreshRate(long refreshRate) {
        this.refreshRate = refreshRate;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public TriggerListener getListener() {
        return listener;
    }

    public void setListener(TriggerListener listener) {
        this.listener = listener;
    }

    public void activate() {
        dismissed = false;
        (new Thread(new Runnable() {
            public void run() {
                while (condition.isTrue() && !dismissed) {
                    try {
                        Thread.sleep(refreshRate);
                    } catch (InterruptedException e) {
                        //do nothing
                    }
                }
                if (!dismissed)
                    listener.triggerWorked();
            }
        })).start();
    }

    public void dismiss() {
        dismissed = true;
    }
}
