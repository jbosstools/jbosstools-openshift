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
package org.jboss.tools.openshift.io.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.jboss.tools.openshift.io.internal.core.OpenShiftIOCoreActivator;

/**
 * A login provider responsible for performing login to OpenShift.io cluster.
 * This involves UI.
 */
public interface LoginProvider {
	LoginResponse login(ICluster cluster, IAccount account);
	
	static LoginProvider get() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(OSIOCoreConstants.LOGIN_PROVIDER_EXTENSION_POINT);
		for(IConfigurationElement element : elements) {
			try {
				return (LoginProvider) element.createExecutableExtension("class");
			} catch (CoreException e) {
				OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
			}
		}
		return null;
	}
}
