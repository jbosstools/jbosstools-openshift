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
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.osgi.framework.Bundle;

/**
 * @author Andre Dietisheim
 */
public class ExtensionUtils {

	private ExtensionUtils() {
	}

	public static Collection<IConfigurationElement> getExtensionConfigurations(String extensionId) {
		return Arrays.asList(Platform.getExtensionRegistry().getConfigurationElementsFor(extensionId));
	}

	public static <T> Collection<T> getExtensions(String extensionId, String classAttribute) {
		List<T> extensions = new ArrayList<T>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(extensionId);
		for (IConfigurationElement configurationElement : config) {
			T extension = safeCreateExtension(extensionId, classAttribute, configurationElement);
			if (extension != null) {
				extensions.add(extension);
			}
		}
		return extensions;
	}

	public static <T> T safeCreateExtension(String extensionId, String classAttribute, IConfigurationElement configurationElement) {
		try {
			return createExtension(classAttribute, configurationElement);
		} catch (IllegalStateException | IllegalArgumentException | ClassNotFoundException e) {
			OpenShiftCommonUIActivator.log(NLS.bind("Could not create extension {0} in bundle {1}", configurationElement.getName(), getBundleNameFor(configurationElement)), e);
			return null;
		}
	}

	public static <T> T createExtension(String classAttribute, IConfigurationElement configurationElement) throws IllegalStateException, IllegalArgumentException, ClassNotFoundException {
		String className = configurationElement.getAttribute(classAttribute);
		if (className  == null) {
			return null;
		}
		return createInstance(className, configurationElement);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T createInstance(final String className, final IConfigurationElement configurationElement) throws IllegalStateException, IllegalArgumentException, ClassNotFoundException {
		Class<T> clazz = getClass(className, configurationElement);
		if (clazz == null) {
			return null;
		}
		try {
			Object object = clazz.newInstance();
			if (object == null) {
				return null;
			}
			return (T) object;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(
					NLS.bind("Could not instantiate class {0}", className)); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClass(final String className, final IConfigurationElement configurationElement) throws IllegalStateException, IllegalArgumentException, ClassNotFoundException {
		if (className == null) {
			throw new IllegalArgumentException("No class name provided");
		}
		String bundleName = getBundleNameFor(configurationElement);
		Bundle bundle = Platform.getBundle(bundleName );
			if (bundle == null) {
				throw new IllegalStateException(NLS.bind("Could not find bundle {0} for extension {1}", bundleName, configurationElement.getName()));
			}
			return (Class<T>) bundle.loadClass(className);
	}

	public static String getBundleNameFor(final IConfigurationElement configurationElement) {
		return configurationElement.getDeclaringExtension().getContributor().getName();
	}

}
