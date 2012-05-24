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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class EmbedCartridgeWizardPageModel extends ObservableUIPojo implements IEmbedCartridgesWizardPageModel {

	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";

	private ApplicationWizardModel wizardModel;
	
	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private Set<IEmbeddableCartridge> selectedCartridges;

	public EmbedCartridgeWizardPageModel(ApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	private void loadEmbeddedCartridges() throws OpenShiftException, SocketTimeoutException {
		selectedCartridges = new HashSet<IEmbeddableCartridge>();
		IApplication application = wizardModel.getApplication();
		if (application == null
				|| application.getEmbeddedCartridges() == null) {
			return;
		}
		setSelectedEmbeddableCartridges(application.getEmbeddedCartridges());
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

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges()
			throws OpenShiftException, SocketTimeoutException {
		if (selectedCartridges == null) {
			loadEmbeddedCartridges();
		}
		return selectedCartridges;
	}

	protected void setSelectedEmbeddableCartridges(List<? extends IEmbeddableCartridge> cartridges) throws SocketTimeoutException, OpenShiftException {
		setSelectedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>(cartridges));
	}
	
	public void setSelectedEmbeddableCartridges(Set<IEmbeddableCartridge> cartridges) throws SocketTimeoutException, OpenShiftException {
		Set<IEmbeddableCartridge> oldValue = new HashSet<IEmbeddableCartridge>(getSelectedEmbeddableCartridges());
		if (cartridges != selectedCartridges) {
			selectedCartridges.clear();
			selectedCartridges.addAll(cartridges);
		}
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, oldValue, selectedCartridges);
	}

	public boolean isSelected(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		return getSelectedEmbeddableCartridges().contains(cartridge);
	}
	
	public boolean hasApplication(ICartridge cartridge) throws SocketTimeoutException, OpenShiftException {
		return wizardModel.getUser().hasApplicationOfType(cartridge);
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	@Override
	public IDomain getDomain() throws SocketTimeoutException, OpenShiftException {
		return wizardModel.getUser().getDefaultDomain();
	}

	@Override
	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException,SocketTimeoutException {
		getSelectedEmbeddableCartridges().add(cartridge);
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, null, getSelectedEmbeddableCartridges());
	}

	@Override
	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException,SocketTimeoutException {
		getSelectedEmbeddableCartridges().remove(cartridge);
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, null, getSelectedEmbeddableCartridges());
	}

	public Set<IEmbeddableCartridge> refreshSelectedEmbeddedCartridges() 
			throws OpenShiftException, SocketTimeoutException {
		getApplication().refresh();
		setSelectedEmbeddableCartridges(getApplication().getEmbeddedCartridges());
		return getSelectedEmbeddableCartridges();
	}

	@Override
	public UserDelegate getUser() {
		return wizardModel.getUser();
	}
	
}
