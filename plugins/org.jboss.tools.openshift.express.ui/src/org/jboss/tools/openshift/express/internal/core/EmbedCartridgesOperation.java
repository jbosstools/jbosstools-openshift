/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.openshift.client.IApplication;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.OpenShiftException;

/**
 * An operation that embeds/removes cartridges from a given application.
 * 
 * @author Andre Dietisheim
 */
public class EmbedCartridgesOperation {

	private IApplication application;

	public EmbedCartridgesOperation(IApplication application) {
		this.application = application;
	}

	/**
	 * Embeds and removes cartridges from the given application so that it
	 * matches the given list of enabled cartridges.
	 * 
	 * @param selectedCartridges
	 * @param monitor
	 * @return
	 * @throws SocketTimeoutException
	 * @throws OpenShiftException
	 */
	public List<IEmbeddedCartridge> execute(final List<IEmbeddableCartridge> selectedCartridges,
			final IProgressMonitor monitor)
			throws SocketTimeoutException, OpenShiftException {
		if (selectedCartridges == null) {
			return Collections.emptyList();
		}

		removeEmbeddedCartridges(
				getRemovedCartridges(selectedCartridges, application.getEmbeddedCartridges()), application);
		return addEmbeddedCartridges(
				getAddedCartridges(selectedCartridges, application.getEmbeddedCartridges()), application);
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToRemove, final IApplication application)
			throws OpenShiftException, SocketTimeoutException {
		if (cartridgesToRemove.isEmpty()) {
			return;
		}
		Collections.sort(cartridgesToRemove, new CartridgeAddRemovePriorityComparator());
		for (IEmbeddableCartridge cartridgeToRemove : cartridgesToRemove) {
			final IEmbeddedCartridge embeddedCartridge = application.getEmbeddedCartridge(cartridgeToRemove);
			if (embeddedCartridge != null) {
				embeddedCartridge.destroy();
			}
		}
	}

	private List<IEmbeddedCartridge> addEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToAdd,
			final IApplication application)
			throws OpenShiftException, SocketTimeoutException {
		if (cartridgesToAdd.isEmpty()) {
			return Collections.emptyList();
		}
		Collections.sort(cartridgesToAdd, new CartridgeAddRemovePriorityComparator());
		return application.addEmbeddableCartridges(cartridgesToAdd);
	}

	private List<IEmbeddableCartridge> getAddedCartridges(List<IEmbeddableCartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<IEmbeddableCartridge> cartridgesToAdd = new ArrayList<IEmbeddableCartridge>();
		for (IEmbeddableCartridge cartridge : selectedCartridges) {
			if (!embeddedCartridges.contains(cartridge)) {
				cartridgesToAdd.add(cartridge);
			}
		}
		return cartridgesToAdd;
	}

	private List<IEmbeddableCartridge> getRemovedCartridges(List<IEmbeddableCartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<IEmbeddableCartridge> cartridgesToRemove = new ArrayList<IEmbeddableCartridge>();
		for (IEmbeddableCartridge cartridge : embeddedCartridges) {
			if (!selectedCartridges.contains(cartridge)) {
				cartridgesToRemove.add(cartridge);
			}
		}
		return cartridgesToRemove;
	}

	private static class CartridgeAddRemovePriorityComparator implements Comparator<IEmbeddableCartridge> {

		@Override
		public int compare(IEmbeddableCartridge thisCartridge, IEmbeddableCartridge thatCartridge) {
			// mysql has to be added/removed before phpmyadmin
			if (thisCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return 1;
			} else if (thisCartridge.equals(IEmbeddableCartridge.POSTGRESQL_84)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.POSTGRESQL_84)) {
				return 1;
			} else if (thisCartridge.equals(IEmbeddableCartridge.MONGODB_20)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.MONGODB_20)) {
				return 1;
			}
			return 0;
		}
	}

}
