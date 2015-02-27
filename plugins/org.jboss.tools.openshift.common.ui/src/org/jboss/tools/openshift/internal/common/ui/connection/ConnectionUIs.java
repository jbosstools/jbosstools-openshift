/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * @author Andre Dietisheim
 */
public class ConnectionUIs {

	private static ConnectionUIs INSTANCE = new ConnectionUIs();
	
	private static final String CONNECTION_UI_EXTENSION = "org.jboss.tools.openshift.common.ui.connectionUI";
	private static final String ATTRIBUTE_CLASS = "class";

	private Collection<IConnectionUI> connectionUIs;
	
	public static ConnectionUIs getInstance() {
		return INSTANCE;
	}
	
	private ConnectionUIs() {
		// inhibit instantiation
	}

	public Collection<IConnectionUI> getAll() {
		if (connectionUIs == null) {
			this.connectionUIs = createConnectionUIs();
		}
		return connectionUIs;
	}

	private Collection<IConnectionUI> createConnectionUIs() {
		List<IConnectionUI> connectionUIs = new ArrayList<IConnectionUI>();
		IConfigurationElement[] config = 
				Platform.getExtensionRegistry().getConfigurationElementsFor(CONNECTION_UI_EXTENSION);
		for (IConfigurationElement extension : config) {
			if (extension.getAttribute(ATTRIBUTE_CLASS) != null) {
				String clazz = extension.getAttribute(ATTRIBUTE_CLASS);
				Bundle bundle = Platform.getBundle(extension.getDeclaringExtension().getContributor().getName());
				IConnectionUI connectionUI = createConnectionUI(clazz, bundle);
				if (connectionUI != null) {
					connectionUIs.add(connectionUI);
				}
			}
		}
		return connectionUIs;
	}

	private IConnectionUI createConnectionUI(final String clazzDefinition, final Bundle bundle) {
		try {
			if (bundle == null) {
				return null;
			}
			Class<?> clazz = bundle.loadClass(clazzDefinition);
			Object object = clazz.newInstance();
			if (object == null
					|| !IConnectionUI.class.isAssignableFrom(object.getClass())) {
				return null;
			}
			return (IConnectionUI) object;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(
					NLS.bind("Could not load class {0} in bundle {1}", clazzDefinition, bundle.getSymbolicName())); //$NON-NLS-1$
		} catch(InstantiationException e) {
			throw new IllegalStateException(
					NLS.bind("Could not instantiate class {0} in extension bundle {1}", clazzDefinition, bundle.getSymbolicName())); //$NON-NLS-1$
		} catch(IllegalAccessException e) {
			throw new IllegalStateException(
					NLS.bind("Could not instantiate class {0} in extension bundle {1}", clazzDefinition, bundle.getSymbolicName())); //$NON-NLS-1$
		}
	}
}
