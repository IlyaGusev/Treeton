/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class Refresher {
    Refreshable target;
    Thread refresher;
    int refreshRate = 1000;

    public Refresher(Refreshable target) {
        this.target = target;
    }

    public void startRefreshing() {
        if (refresher == null) {
            refresher = new Thread(
                    new Runnable() {
                        public void run() {
                            while (refresher != null) {
                                target.refresh();
                                try {
                                    Thread.sleep(refreshRate);
                                } catch (InterruptedException e) {
                                    //do nothing
                                }
                            }
                        }
                    }
            );
            refresher.start();
        }
    }

    public void stopRefreshing() {
        refresher = null;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    public void setRefreshRate(int refreshRate) {
        this.refreshRate = refreshRate;
    }

}
