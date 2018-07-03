/*******************************************************************************
 * Copyright (c) 2017-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public class RegistryProviderModel {

	private static RegistryProviderModel model = new RegistryProviderModel();

	public static RegistryProviderModel getDefault() {
		return model;
	}

	private ProviderPair[] registryProviders = null;

	private RegistryProviderModel() {
		// Nothing needs to be done to initialize
	}

	/**
	 * Get a registry url for this connection, or null if unavailable
	 * 
	 * @param c
	 * @return
	 */
	public IStatus getRegistryURL(IConnection c) {
		if (c == null) {
			return new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, 
					"Unable to discover a registry URL for connection 'null'", null);
		}
		
		MultiStatus ms = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0,
				"Unable to discover a registry URL for connection " + c.toString(), null);
		IConnectionRegistryProvider[] sorted = getProviders();
		for (int i = 0; i < sorted.length; i++) {
			IStatus ret = sorted[i].getRegistryURL(c);
			if (ret != null) {
				if (ret.isOK()) {
					return ret;
				}
				ms.add(ret);
			}
		}
		return ms;
	}

	private synchronized IConnectionRegistryProvider[] getProviders() {
		if (registryProviders == null) {
			loadProviders();
		}
		List<ProviderPair> l = new ArrayList<>(Arrays.asList(registryProviders));
		Comparator<ProviderPair> c = (e1, e2) -> e2.getWeight() - e1.getWeight();
		List<IConnectionRegistryProvider> ret = l.stream().sorted(c).map(ProviderPair::getProvider)
				.collect(Collectors.toList());
		return ret.toArray(new IConnectionRegistryProvider[ret.size()]);
	}

	private void loadProviders() {
		ArrayList<ProviderPair> ret = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor("org.jboss.tools.openshift.core.connection",
				"registryprovider");
		for (int i = 0; i < cf.length; i++) {
			try {
				IConnectionRegistryProvider p = (IConnectionRegistryProvider) cf[i].createExecutableExtension("class");
				String weight = cf[i].getAttribute("weight");
				Integer in = Integer.parseInt(weight);
				if (p != null && in != null) {
					ret.add(new ProviderPair(p, in.intValue()));
				}
			} catch (CoreException e) {
				OpenShiftCoreActivator.pluginLog().logError("Failure loading registry provider extension", e);
			}
		}
		registryProviders = ret.toArray(new ProviderPair[ret.size()]);
	}

	private static class ProviderPair {
		IConnectionRegistryProvider element;
		int weight;

		public ProviderPair(IConnectionRegistryProvider el, int weight) {
			this.element = el;
			this.weight = weight;
		}

		public int getWeight() {
			return weight;
		}

		public IConnectionRegistryProvider getProvider() {
			return element;
		}
	}
}
