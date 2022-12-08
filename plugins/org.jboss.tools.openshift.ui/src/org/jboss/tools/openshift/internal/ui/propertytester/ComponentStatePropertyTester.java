/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.jboss.tools.openshift.core.odo.ComponentFeatures;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

public class ComponentStatePropertyTester extends PropertyTester {

	private static final String VALUE_IS_DEBUG = "isDebug";
	private static final String VALUE_IS_DEPLOY = "isDeploy";
	private static final String VALUE_IS_DEV = "isDev";

	private static final String VALUE_IS_DEBUG_RUNNING = "isDebugRunning";
	private static final String VALUE_IS_DEPLOY_RUNNING = "isDeployRunning";
	private static final String VALUE_IS_DEV_RUNNING = "isDevRunning";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		ComponentElement component = getComponentElement(receiver);
		if (!(component != null) || !(expectedValue instanceof Boolean) || args == null || args.length != 1
				|| !(args[0] instanceof String)) {
			return false;
		}
		String arg = (String) args[0];
		ComponentFeatures features = component.getWrapped().getInfo().getFeatures();
		ComponentFeatures liveFeatures = component.getWrapped().getLiveFeatures();

		switch (arg) {
		case VALUE_IS_DEBUG:
			if (!liveFeatures.isDebug())
				return expectedValue.equals(Boolean.valueOf(features.isDebug()));
			break;
		case VALUE_IS_DEPLOY:
			if (!liveFeatures.isDeploy())
				return expectedValue.equals(Boolean.valueOf(features.isDeploy()));
			break;
		case VALUE_IS_DEV:
			if (!liveFeatures.isDev())
				return expectedValue.equals(Boolean.valueOf(features.isDev()));
			break;

		case VALUE_IS_DEBUG_RUNNING:
			return expectedValue.equals(Boolean.valueOf(liveFeatures.isDebug()));
		case VALUE_IS_DEPLOY_RUNNING:
			return expectedValue.equals(Boolean.valueOf(liveFeatures.isDeploy()));
		case VALUE_IS_DEV_RUNNING:
			return expectedValue.equals(Boolean.valueOf(liveFeatures.isDev()));
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
