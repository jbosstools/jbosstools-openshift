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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;

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

	private List<IEmbeddableCartridge> embeddableCartridges = new ArrayList<IEmbeddableCartridge>();
	private List<IEmbeddableCartridge> selectedCartridges;

	public EmbedCartridgeWizardPageModel(ApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	public void loadSelectedEmbeddableCartridges() throws OpenShiftException {
		selectedCartridges = new ArrayList<IEmbeddableCartridge>();
		IApplication application = wizardModel.getApplication();
		if (application == null
				|| application.getEmbeddedCartridges() == null) {
			return;
		}
		List<IEmbeddableCartridge> embeddedCartridges = application.getEmbeddedCartridges();
		selectedCartridges.addAll(embeddedCartridges);
	}

	public List<IEmbeddableCartridge> loadEmbeddableCartridges() throws OpenShiftException {
		List<IEmbeddableCartridge> cartridges = wizardModel.getUser().getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		return cartridges;
	}

	public void setEmbeddableCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddableCartridges, this.embeddableCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddableCartridges() {
		return embeddableCartridges;
	}

	public List<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		if (selectedCartridges == null) {
			loadSelectedEmbeddableCartridges();
		}
		return selectedCartridges;
	}

	public boolean hasApplication(ICartridge cartridge) {
		try {
			return wizardModel.getUser().hasApplication(cartridge);
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(
					OpenShiftUIActivator.createErrorStatus("Could not get application by cartridge", e));
			return false;
		}
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException {
		return wizardModel.createApplication(name, ICartridge.JENKINS_14, monitor);
	}
	
	/**
	 * Embeds/removes the cartridges that were added/removed in this wizard
	 * page.
	 * 
	 * @return the cartridges that were added.
	 * @throws OpenShiftException
	 */
	public List<IEmbeddableCartridge> embedCartridges() throws OpenShiftException {
		if (selectedCartridges == null) {
			return Collections.emptyList();
		}
		List<IEmbeddableCartridge> addedCartridges = new ArrayList<IEmbeddableCartridge>();
		List<IEmbeddableCartridge> removedCartridges = new ArrayList<IEmbeddableCartridge>();
		computeAdditionsAndRemovals(addedCartridges, removedCartridges, selectedCartridges);
		addEmbeddedCartridges(addedCartridges);
		removeEmbeddedCartridges(removedCartridges);
		return addedCartridges;
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> removedCartridges) throws OpenShiftException {
		if (removedCartridges.isEmpty()) {
			return;
		}
		Collections.sort(removedCartridges, new CartridgeComparator());
		getApplication().removeEmbbedCartridges(removedCartridges);
	}

	private void addEmbeddedCartridges(List<IEmbeddableCartridge> addedCartridges) throws OpenShiftException {
		if (addedCartridges.isEmpty()) {
			return;
		}
		Collections.sort(addedCartridges, new CartridgeComparator());
		getApplication().addEmbbedCartridges(addedCartridges);
	}

	private void computeAdditionsAndRemovals(List<IEmbeddableCartridge> addedCartridges,
			List<IEmbeddableCartridge> removedCartridges, List<IEmbeddableCartridge> selectedCartridges)
			throws OpenShiftException {
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
