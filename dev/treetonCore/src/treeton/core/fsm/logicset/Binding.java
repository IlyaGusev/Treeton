/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

public class Binding {

    Integer id;
    Integer start = null;
    Integer finish = null;

    public Binding(Integer id) {
        this.id = id;
    }

    public boolean isFinished() {
        return finish != null;
    }

    public void Finish(int i) {
        this.finish = i;
    }

    public boolean isStarted() {
        return start != null;
    }

    public void Start(int i) {
        this.start = i;
    }

    public Integer getId() {
        return id;
    }
}
