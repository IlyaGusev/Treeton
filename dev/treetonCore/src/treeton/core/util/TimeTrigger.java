/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class TimeTrigger extends Trigger {
    long startTime;
    long delta;

    public TimeTrigger(long delta) {
        this.delta = delta;
        setCondition(new Condition() {
            public boolean isTrue() {
                return (System.currentTimeMillis() - startTime) < TimeTrigger.this.delta;
            }
        });
    }


    public void activate() {
        startTime = System.currentTimeMillis();
        super.activate();
    }
}
