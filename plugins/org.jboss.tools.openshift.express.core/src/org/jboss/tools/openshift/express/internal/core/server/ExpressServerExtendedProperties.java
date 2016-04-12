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
import org.jboss.tools.openshift.express.internal.core.server.ExpressServerUtils.GetApplicationException;

import com.openshift.client.IApplication;

/**
 * @author Rob Stryker
 */
public class ExpressServerExtendedProperties extends ServerExtendedProperties {

	public ExpressServerExtendedProperties(IAdaptable adaptable) {
		super(adaptable);
	}

	public boolean allowConvenienceEnhancements() {
		return false;
	}
	
	@Override
	public boolean hasWelcomePage() {
		return true;
	}

	@Override
	public String getWelcomePageUrl() throws GetWelcomePageURLException {
		if (!ExpressServerUtils.isExpressRuntime(server)) {
			return null;
		}
		
		try {
			final IApplication application = ExpressServerUtils.getApplication(server);
			if (application != null) {
				return application.getApplicationUrl();
			}
		} catch (GetApplicationException e) {
			throw new GetWelcomePageURLException(e.getMessage(), e.getCause());
		}
		return null;
	}
}
