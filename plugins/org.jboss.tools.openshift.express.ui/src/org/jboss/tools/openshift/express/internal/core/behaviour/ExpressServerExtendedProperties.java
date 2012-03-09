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
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

public class ExpressServerExtendedProperties extends ServerExtendedProperties {

	public ExpressServerExtendedProperties(IAdaptable adaptable) {
		super(adaptable);
	}
	public boolean hasWelcomePage() {
		return true;
	}
	
	public String getWelcomePageUrl() {
		if (!ExpressServerUtils.isOpenShiftRuntime(server)) {
			return null;
		}
		IApplication application = ExpressServerUtils.findApplicationForServer(server);
		if (application != null) {
			try {
				return application.getApplicationUrl();
			} catch (OpenShiftException e) {
				IStatus status = OpenShiftUIActivator.createErrorStatus(
						NLS.bind("Could not get application url for server {0}", server.getName()), e);
				OpenShiftUIActivator.log(status);
			}
		}
		return null;
	}
}
