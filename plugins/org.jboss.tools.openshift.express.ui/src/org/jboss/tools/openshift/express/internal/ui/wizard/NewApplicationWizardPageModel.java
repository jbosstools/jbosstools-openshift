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
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class NewApplicationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION = "application";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";

	private ApplicationWizardModel wizardModel;

	private List<ICartridge> cartridges = new ArrayList<ICartridge>();
	private ICartridge selectedCartridge;
	private StringPreferenceValue selectedCartridgePreference;

	public NewApplicationWizardPageModel(ApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		this.selectedCartridgePreference = new StringPreferenceValue(
				"org.jboss.tools.openshift.express.internal.ui.wizard.NewApplicationWizard.selectedCartridge",
				OpenShiftUIActivator.PLUGIN_ID);
	}

	private ICartridge initSelectedCartridge() {
		String selectedCartridgeName = selectedCartridgePreference.get();
		if (getSelectedCartridge() != null) {
			selectedCartridgeName = getSelectedCartridge().getName();
		}
		if (selectedCartridgeName == null
				|| selectedCartridgeName.length() == 0) {
			selectedCartridgeName = ICartridge.JBOSSAS_7.getName();
		}
		ICartridge selectedCartridge = getCartridgeByName(selectedCartridgeName);
		if (selectedCartridge == null
				&& getCartridges().size() > 0) {
			selectedCartridge = getCartridges().get(0);
		}
		return selectedCartridge;
	}

	public String getName() {
		return wizardModel.getName();
	}

	public void setName(String name) {
		wizardModel.setName(name);
		firePropertyChange(PROPERTY_NAME, wizardModel.getName(), wizardModel.setName(name));
	}

	public void loadCartridges() throws OpenShiftException {
		setCartridges(wizardModel.getUser().getCartridges());
	}

	public void setCartridges(List<ICartridge> cartridges) {
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
		setSelectedCartridge(initSelectedCartridge());
	}

	public List<ICartridge> getCartridges() {
		return cartridges;
	}

	public ICartridge getSelectedCartridge() {
		return selectedCartridge;
	}

	public void setSelectedCartridge(ICartridge cartridge) {
		wizardModel.setCartridge(cartridge);
		if (cartridge != null) {
			selectedCartridgePreference.store(cartridge.getName());
		}
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE, selectedCartridge, this.selectedCartridge = cartridge);
	}

	private ICartridge getCartridgeByName(String name) {
		ICartridge matchingCartridge = null;
		for (ICartridge cartridge : getCartridges()) {
			if (name.equals(cartridge.getName())) {
				matchingCartridge = cartridge;
				break;
			}
		}
		return matchingCartridge;
	}

	public boolean hasApplication(String name) {
		try {
			return wizardModel.hasApplication(name);
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(
					OpenShiftUIActivator.createErrorStatus("Could not get application by name", e));
			return false;
		}
	}
}
