package org.jboss.tools.openshift.test.ui.models;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class StubInvocationHandler implements InvocationHandler {
	private Map<Method, InvocationHandler> handlers = new HashMap<Method, InvocationHandler>();
	private InvocationHandler stubbingHandler;
	private Method lastMethod;

	StubInvocationHandler() {
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (stubbingHandler != null) {
			lastMethod = method;
			return stubReturn(method);
		}
		InvocationHandler handler= handlers.get(method);
		if (handler != null) {
			return handler.invoke(proxy, method, args);
		}
		return stubReturn(method);
	}

	private Object stubReturn(Method method) {
		if (!method.getReturnType().isPrimitive()) {
			return null;
		};
		String typeName = method.getReturnType().getTypeName();
		switch (typeName)  {
		case "boolean": return false;
		case "byte": return (byte)0;
		case "char": return (char)0;
		case "short": return (short)0;
		case "int": return 0;
		case "long": return 0;
		case "float": return 0f;
		case "double": return 0.0;
		}
		return null;
	}

	public StubInvocationHandler stub(InvocationHandler handler) {
		this.stubbingHandler = handler;
		return this;
	}

	public <T> void when(T argument) {
		if (stubbingHandler == null) {
			throw new IllegalStateException("no stubbing in progress");
		}
		try {
			handlers.put(lastMethod, stubbingHandler);
		} finally {
			stubbingHandler = null;
			lastMethod = null;
		}
	}

}
