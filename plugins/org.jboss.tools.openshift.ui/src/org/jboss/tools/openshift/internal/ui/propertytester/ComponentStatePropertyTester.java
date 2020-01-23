package org.jboss.tools.openshift.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

public class ComponentStatePropertyTester extends PropertyTester {

	private final static String PROPERTY_COMPONENT_ALLOWED_STATE = "componentAllowedState";
	
	private static final String VALUE_PUSHED = "pushed";
	
	private static final String VALUE_NOT_PUSHED = "not pushed";
	
	private static final String VALUE_NO_CONTEXT = "no context";
	
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof ComponentElement) || !(expectedValue instanceof Boolean) || args == null
		        || args.length != 1 || !(args[0] instanceof String)) {
			return false;
		}
		ComponentElement component = (ComponentElement) receiver;
		switch (component.getWrapped().getState()) {
		case PUSHED:
			return expectedValue.equals(args[0].equals(VALUE_PUSHED));
		case NOT_PUSHED:
			return expectedValue.equals(args[0].equals(VALUE_NOT_PUSHED));
		case NO_CONTEXT:
			return expectedValue.equals(args[0].equals(VALUE_NO_CONTEXT));
		}
		return false;
	}

}
