/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.stack;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Red Hat Developers
 *
 */
public class RemoteStackProviderRegistry implements IRegistryChangeListener {
	private static final String REMOTE_STACK_DEBUGGER = "remoteStackDebugger";

	private static final String CLASS_ATTR = "class";

	private static RemoteStackProviderRegistry INSTANCE = new RemoteStackProviderRegistry();

	public static RemoteStackProviderRegistry getInstance() {
		return INSTANCE;
	}

	private final List<RemoteStackDebugger> remoteStackDebuggers = new ArrayList<>();

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas(OpenShiftCoreActivator.PLUGIN_ID, REMOTE_STACK_DEBUGGER);
		if (deltas != null) {
			synchronized (this) {
				for (IExtensionDelta delta : deltas) {
					IConfigurationElement[] cf = delta.getExtension().getConfigurationElements();
					if (delta.getKind() == IExtensionDelta.ADDED) {
						addRemoteStackDebuggers(cf);
					} else {
						removeRemoteStackDebuggers(cf);
					}
				}
			}
		}
	}

	public void initialize() {
		remoteStackDebuggers.clear();
		loadRemoteStackDebuggers();
		Platform.getExtensionRegistry().addRegistryChangeListener(this, OpenShiftCoreActivator.PLUGIN_ID);
	}

	private void loadRemoteStackDebuggers() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(OpenShiftCoreActivator.PLUGIN_ID,
		        REMOTE_STACK_DEBUGGER);
		addRemoteStackDebuggers(cf);
	}

	private void addRemoteStackDebuggers(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				RemoteStackDebugger provider = (RemoteStackDebugger) ce.createExecutableExtension(CLASS_ATTR);
				synchronized (remoteStackDebuggers) {
					remoteStackDebuggers.add(provider);
				}
			} catch (Throwable t) {
				OpenShiftCoreActivator.logError(t.getLocalizedMessage(), t);
			}
		}
	}

	private void removeRemoteStackDebuggers(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				RemoteStackDebugger provider = (RemoteStackDebugger) ce.createExecutableExtension(CLASS_ATTR);
				synchronized (remoteStackDebuggers) {
					remoteStackDebuggers.remove(provider);
				}
			} catch (Throwable t) {
				OpenShiftCoreActivator.logError(t.getLocalizedMessage(), t);
			}
		}
	}

	public void destroy() {
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
	}

	/**
	 * Return the first avaliable implementation for a specific stack type or null if none match.
	 * 
	 * @param stackType the stack type to match
	 * @param stackVersion the stack version to match
	 * @return the first that matched or null if none matches
	 */
	public RemoteStackDebugger findBytype(String stackType, String stackVersion) {
		return remoteStackDebuggers.stream().filter(item -> item.isValid(stackType, stackVersion)).findFirst().orElse(null);
	}
}
