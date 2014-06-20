/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.server;

import org.eclipse.core.runtime.IAdaptable;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;

import com.openshift.client.IApplication;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerExtendedProperties extends ServerExtendedProperties {

	public OpenShiftServerExtendedProperties(IAdaptable adaptable) {
		super(adaptable);
	}
	
	public boolean hasWelcomePage() {
		return true;
	}
	
	public String getWelcomePageUrl() {
		if (!OpenShiftServerUtils.isOpenShiftRuntime(server)) {
			return null;
		}
		final IApplication application = OpenShiftServerUtils.getApplication(server);
		if (application != null) {
			return application.getApplicationUrl();
		}
		return null;
	}
}
