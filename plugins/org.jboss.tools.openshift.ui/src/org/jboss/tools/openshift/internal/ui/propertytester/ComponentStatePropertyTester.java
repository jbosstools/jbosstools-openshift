package org.jboss.tools.openshift.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.jboss.tools.openshift.core.odo.ComponentFeatures;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

public class ComponentStatePropertyTester extends PropertyTester {

	private static final String VALUE_IS_DEBUG = "isDebug";
	private static final String VALUE_IS_DEPLOY = "isDeploy";
	private static final String VALUE_IS_DEV = "isDev";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		ComponentElement component = getComponentElement(receiver);
		if (!(component != null) || !(expectedValue instanceof Boolean) || args == null || args.length != 1
				|| !(args[0] instanceof String)) {
			return false;
		}
		String arg = (String) args[0];
		ComponentFeatures features = component.getWrapped().getLiveFeatures();

		switch (arg) {
		case VALUE_IS_DEBUG:
			return expectedValue.equals(Boolean.valueOf(features.isDebug()));

		case VALUE_IS_DEPLOY:

			return expectedValue.equals(Boolean.valueOf(features.isDeploy()));
		case VALUE_IS_DEV:
			return expectedValue.equals(Boolean.valueOf(features.isDev()));

		default:
			break;
		}

		return false;
	}

	/**
	 * @param receiver the receiver
	 * @return the receiver adapted to ComponentElement or null if not found
	 */
	private ComponentElement getComponentElement(Object receiver) {
		Object result = receiver;
		if (result instanceof AbstractOpenshiftUIElement<?, ?, ?>) {
			while (!(result instanceof ComponentElement) && result instanceof AbstractOpenshiftUIElement<?, ?, ?>) {
				result = ((AbstractOpenshiftUIElement<?, ?, ?>) result).getParent();
			}
		}
		if (result instanceof ComponentElement) {
			return (ComponentElement) result;
		}
		return null;
	}

}
