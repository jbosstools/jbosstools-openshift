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

import java.util.function.Function;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.io.internal.core.OpenShiftIOCoreActivator;

/**
 * Interface to be implemented for the tokenProvider extension point. This is
 * just an empty extension to Function<IResource, String> in order to keep
 * loose coupling for extension point users.
 */
public interface TokenProvider extends Function<IResource, String> {
	default String getToken(IResource resource) {
		return apply(resource);
	}
	
	static TokenProvider get() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(OSIOCoreConstants.TOKEN_PROVIDER_EXTENSION_POINT);
		for(IConfigurationElement element : elements) {
			try {
				return (TokenProvider) element.createExecutableExtension("class");
			} catch (CoreException e) {
				OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
			}
		}
		return null;
	}

}
