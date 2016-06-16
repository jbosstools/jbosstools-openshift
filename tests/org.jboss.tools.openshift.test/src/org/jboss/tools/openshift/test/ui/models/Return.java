package org.jboss.tools.openshift.test.ui.models;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class Return<T> implements InvocationHandler {
	private T value;

	public Return(T value) {
		this.value= value;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return value;
	}

}
