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
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.IApplication;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgesJob extends AbstractDelegatingMonitorJob {

	private List<IEmbeddableCartridge> selectedCartridges;
	private IApplication application;
	private List<IEmbeddedCartridge> addedCartridges;
	private boolean dontRemove;

	public EmbedCartridgesJob(List<IEmbeddableCartridge> selectedCartridges, boolean dontRemove,
			IApplication application) {
		super(NLS.bind(OpenShiftExpressUIMessages.ADDING_REMOVING_CARTRIDGES, application.getName()));
		this.selectedCartridges = selectedCartridges;
		this.dontRemove = dontRemove;
		this.application = application;
	}

	public EmbedCartridgesJob(List<IEmbeddableCartridge> selectedCartridges, IApplication application) {
		this(selectedCartridges, false, application);
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		try {
			if (!dontRemove) {
				removeEmbeddedCartridges(
						getRemovedCartridges(selectedCartridges, application.getEmbeddedCartridges()),
						application, monitor);
			}
			this.addedCartridges = addEmbeddedCartridges(
					getAddedCartridges(selectedCartridges, application.getEmbeddedCartridges()), 
					application, monitor);
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return OpenShiftUIActivator.createErrorStatus("Could not embed cartridges for application {0}", e,
					application.getName());
		}
	}

	public List<IEmbeddedCartridge> getAddedCartridges() {
		return addedCartridges;
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToRemove,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToRemove.isEmpty()) {
			return;
		}
		Collections.sort(cartridgesToRemove, new CartridgeAddRemovePriorityComparator());
		for (IEmbeddableCartridge cartridgeToRemove : cartridgesToRemove) {
			if (monitor.isCanceled()) {
				return;
			}
			final IEmbeddedCartridge embeddedCartridge = application.getEmbeddedCartridge(cartridgeToRemove);
			if (embeddedCartridge != null) {
				embeddedCartridge.destroy();
			}
		}
	}

	private List<IEmbeddedCartridge> addEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToAdd,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToAdd.isEmpty()
				|| monitor.isCanceled()) {
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
			} else if (thisCartridge.equals(IEmbeddableCartridge.MONGODB_22)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.MONGODB_22)) {
				return 1;
			}
			return 0;
		}
	}
}
