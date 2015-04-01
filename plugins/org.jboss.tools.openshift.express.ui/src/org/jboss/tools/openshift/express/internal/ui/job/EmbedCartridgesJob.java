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
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.ExpressUIMessages;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.query.CartridgeNameQuery;
import com.openshift.client.cartridge.query.ICartridgeQuery;

/**
 * @author Andre Dietisheim
 */
public class EmbedCartridgesJob extends AbstractDelegatingMonitorJob {

	private List<ICartridge> selectedCartridges;
	private IApplication application;
	private List<IEmbeddedCartridge> addedCartridges;

	public EmbedCartridgesJob(List<ICartridge> selectedCartridges, IApplication application) {
		super(NLS.bind(ExpressUIMessages.ADDING_REMOVING_CARTRIDGES, application.getName()));
		this.selectedCartridges = selectedCartridges;
		this.application = application;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		try {
			removeEmbeddedCartridges(
					getRemovedCartridges(selectedCartridges, application.getEmbeddedCartridges()),
					application, monitor);
			this.addedCartridges = addEmbeddedCartridges(
					getAddedCartridges(selectedCartridges, application.getEmbeddedCartridges()), 
					application, monitor);
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return ExpressUIActivator.createErrorStatus("Could not embed cartridges for application {0}", e,
					application.getName());
		}
	}

	public List<IEmbeddedCartridge> getAddedCartridges() {
		return addedCartridges;
	}

	private void removeEmbeddedCartridges(List<ICartridge> cartridgesToRemove,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToRemove.isEmpty()) {
			return;
		}
		Collections.sort(cartridgesToRemove, new CartridgeAddRemovePriorityComparator());
		for (ICartridge cartridgeToRemove : cartridgesToRemove) {
			if (monitor.isCanceled()) {
				return;
			}
			final IEmbeddedCartridge embeddedCartridge = application.getEmbeddedCartridge(cartridgeToRemove.getName());
			if (embeddedCartridge != null) {
				embeddedCartridge.destroy();
			}
		}
	}

	private List<IEmbeddedCartridge> addEmbeddedCartridges(List<ICartridge> cartridgesToAdd,
			final IApplication application, IProgressMonitor monitor) throws OpenShiftException {
		if (cartridgesToAdd.isEmpty()
				|| monitor.isCanceled()) {
			return Collections.emptyList();
		}
		Collections.sort(cartridgesToAdd, new CartridgeAddRemovePriorityComparator());
		return application.addEmbeddableCartridges(
				(ICartridge[]) cartridgesToAdd.toArray(new ICartridge[cartridgesToAdd.size()]));
	}

	private List<ICartridge> getAddedCartridges(List<ICartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<ICartridge> cartridgesToAdd = new ArrayList<ICartridge>();
		for (ICartridge cartridge : selectedCartridges) {
			if (!embeddedCartridges.contains(cartridge)) {
				cartridgesToAdd.add(cartridge);
			}
		}
		return cartridgesToAdd;
	}

	private List<ICartridge> getRemovedCartridges(List<ICartridge> selectedCartridges,
			List<IEmbeddedCartridge> embeddedCartridges) {
		List<ICartridge> cartridgesToRemove = new ArrayList<ICartridge>();
		for (ICartridge cartridge : embeddedCartridges) {
			if (!selectedCartridges.contains(cartridge)) {
				cartridgesToRemove.add(cartridge);
			}
		}
		return cartridgesToRemove;
	}

	private static class CartridgeAddRemovePriorityComparator implements Comparator<ICartridge> {

		private static final ICartridgeQuery mySqlMatcher = new CartridgeNameQuery(IEmbeddedCartridge.NAME_MYSQL);
		private static final ICartridgeQuery postgresqlMatcher = new CartridgeNameQuery(IEmbeddedCartridge.NAME_POSTGRESQL);
		private static final ICartridgeQuery mongodbMatcher = new CartridgeNameQuery(IEmbeddedCartridge.NAME_MONGODB);
		
		@Override
		public int compare(ICartridge thisCartridge, ICartridge thatCartridge) {
			// mysql has to be added/removed before phpmyadmin
			if (mySqlMatcher.matches(thisCartridge)) {
				return -1;
			} else if (mySqlMatcher.matches(thatCartridge)) {
				return 1;
			} else if (postgresqlMatcher.matches(thisCartridge)) {
				return -1;
			} else if (postgresqlMatcher.matches(thatCartridge)) {
				return 1;
			} else if (mongodbMatcher.matches(thisCartridge)) {
				return -1;
			} else if (mongodbMatcher.matches(thatCartridge)) {
				return 1;
			}
			return 0;
		}

		
	
	}
}
