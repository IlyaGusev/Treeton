/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

import treeton.prosody.mdlcompiler.api.RuntimeContext;
import treeton.prosody.mdlcompiler.api.fsm.BindingUser;

public interface Action<T> extends BindingUser {
	void act(RuntimeContext<T> context,
             ModificationsCollector<T> modificationsCollector,
             InputObjectInfoProvider<T> inputObjectInfoProvider, boolean notransaction);

	String getRuleSignature();

	void setRuleSignature(String rule);

    int getPriority();

    void setPriority(int priority);

    void optimize() throws Exception;
}
