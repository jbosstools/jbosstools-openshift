/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.springboot;

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;

public class OpenShiftSpringBootActivator extends BaseCorePlugin {
	
	private static OpenShiftSpringBootActivator instance = null;
	
	public static final String PLUGIN_ID = "org.jboss.tools.openshift.springboot";
	
	public OpenShiftSpringBootActivator() {
		super();
		setInstance(this);
	}

	private static void setInstance(OpenShiftSpringBootActivator instance) {
		OpenShiftSpringBootActivator.instance = instance;
	}
	
	public static OpenShiftSpringBootActivator getDefault() {
		return instance;
	}
	
	public static void logError(Throwable t) {
		getDefault().pluginLogInternal().logError(t);
	}

}
