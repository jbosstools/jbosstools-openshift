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
package org.jboss.tools.openshift.io.internal.ui;

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;

public class OpenShiftIOUIActivator extends BaseCorePlugin {

	private static OpenShiftIOUIActivator INSTANCE = null;
	
	public static final String PLUGIN_ID = "org.jboss.tools.openshift.io.ui";
	
	public OpenShiftIOUIActivator() {
		super();
		INSTANCE = this;
	}

	public static OpenShiftIOUIActivator getDefault() {
		return INSTANCE;
	}
	
	public static void logError(String message, Throwable t) {
		getDefault().pluginLogInternal().logError(message, t);
	}

}
