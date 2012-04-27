/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class EmbedCartridgeWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";

	private ApplicationWizardModel wizardModel;

	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private List<IEmbeddableCartridge> selectedCartridges;

	public EmbedCartridgeWizardPageModel(ApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	public void loadConfiguredEmbeddedCartridges() throws OpenShiftException, SocketTimeoutException {
		selectedCartridges = new ArrayList<IEmbeddableCartridge>();
		IApplication application = wizardModel.getApplication();
		if (application == null
				|| application.getEmbeddedCartridges() == null) {
			return;
		}
		selectedCartridges.addAll(application.getEmbeddedCartridges());
	}

	public List<IEmbeddableCartridge> loadEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException {
		List<IEmbeddableCartridge> cartridges = wizardModel.getUser().getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		return cartridges;
	}

	public void setEmbeddableCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddedCartridges, this.embeddedCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddedCartridges() {
		return embeddedCartridges;
	}

	public List<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException {
		if (selectedCartridges == null) {
			loadConfiguredEmbeddedCartridges();
		}
		return selectedCartridges;
	}

	public boolean hasApplication(ICartridge cartridge) throws SocketTimeoutException, OpenShiftException {
		return wizardModel.getUser().hasApplicationOfType(cartridge);
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException, SocketTimeoutException {
		return wizardModel.createApplication(name, ICartridge.JENKINS_14, ApplicationScale.NO_SCALE, IGearProfile.SMALL, monitor);
	}
	
	/**
	 * Embeds/removes the cartridges that were added/removed in this wizard
	 * page.
	 * 
	 * @return the cartridges that were added (embedded).
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException 
	 */
	public List<IEmbeddedCartridge> embedCartridges() throws OpenShiftException, SocketTimeoutException {
		if (selectedCartridges == null) {
			return Collections.emptyList();
		}
		List<IEmbeddableCartridge> cartridgesToAdd = new ArrayList<IEmbeddableCartridge>();
		List<IEmbeddableCartridge> cartridgesToRemove = new ArrayList<IEmbeddableCartridge>();
		computeAdditionsAndRemovals(cartridgesToAdd, cartridgesToRemove, selectedCartridges);
		final List<IEmbeddedCartridge> addedCartridges = addEmbeddedCartridges(cartridgesToAdd);
		removeEmbeddedCartridges(cartridgesToRemove);
		return addedCartridges;
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToRemove) throws OpenShiftException, SocketTimeoutException {
		if (cartridgesToRemove.isEmpty()) {
			return;
		}
		//Collections.sort(removedCartridges, new CartridgeComparator());
		for(IEmbeddableCartridge cartridgeToRemove : cartridgesToRemove) {
			final IEmbeddedCartridge embeddedCartridge = getApplication().getEmbeddedCartridge(cartridgeToRemove);
			if(embeddedCartridge != null) {
				embeddedCartridge.destroy();
			}
		}
	}

	private List<IEmbeddedCartridge> addEmbeddedCartridges(List<IEmbeddableCartridge> cartridgesToAdd) throws OpenShiftException, SocketTimeoutException {
		if (cartridgesToAdd.isEmpty()) {
			return Collections.emptyList();
		}
		//Collections.sort(addedCartridges, new CartridgeComparator());
		return getApplication().addEmbeddableCartridges(cartridgesToAdd);
	}

	private void computeAdditionsAndRemovals(List<IEmbeddableCartridge> addedCartridges,
			List<IEmbeddableCartridge> removedCartridges, List<IEmbeddableCartridge> selectedCartridges)
			throws OpenShiftException, SocketTimeoutException {
		ListDiff listDiff = Diffs.computeListDiff(getApplication().getEmbeddedCartridges(), selectedCartridges);
		for (ListDiffEntry entry : listDiff.getDifferences()) {
			if (entry.isAddition()) {
				addedCartridges.add((IEmbeddableCartridge) entry.getElement());
			} else {
				removedCartridges.add((IEmbeddableCartridge) entry.getElement());
			}
		}
	}

	private static class CartridgeComparator implements Comparator<IEmbeddableCartridge> {

		@Override
		public int compare(IEmbeddableCartridge thisCartridge, IEmbeddableCartridge thatCartridge) {
			// mysql has to be added/removed before phpmyadmin
			if (thisCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return 1;
			}
			return 0;
		}
	}

}
