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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.IApplicationProperties;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizardPageModel extends ObservableUIPojo implements IEmbedCartridgesWizardPageModel {

	private IEmbeddedCartridgesModel wizardModel;
	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private IEmbeddableCartridge selectedEmbeddableCartridge ;

	public EmbeddedCartridgesWizardPageModel(IEmbeddedCartridgesModel wizardModel) {
		this.wizardModel = wizardModel;
	}
	
	public List<IEmbeddableCartridge> loadEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException {
		List<IEmbeddableCartridge> cartridges = wizardModel.getEmbeddableCartridges();
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

	@Override
	public Set<IEmbeddableCartridge> getCheckedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getCheckedEmbeddableCartridges();
	}

	protected void setCheckedEmbeddableCartridges(List<? extends IEmbeddableCartridge> cartridges) throws OpenShiftException {
		setCheckedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>(cartridges));
	}
	
	@Override
	public void setCheckedEmbeddableCartridges(Set<IEmbeddableCartridge> cartridges) throws OpenShiftException {
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, wizardModel.setCheckedEmbeddableCartridges(cartridges));
	}
	
	@Override
	public void setSelectedEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGE, selectedEmbeddableCartridge, this.selectedEmbeddableCartridge = cartridge);
	}
	
	@Override
	public IEmbeddableCartridge getSelectedEmbeddableCartridge() {
		return selectedEmbeddableCartridge;
	}

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException {
		return wizardModel.isEmbedded(cartridge);
	}

	public boolean hasApplication(IStandaloneCartridge cartridge) throws OpenShiftException {
		IDomain domain = getDomain();
		if (domain == null) {
			return false;
		}
		return domain.hasApplicationByCartridge(cartridge);
	}

	@Override
	public void checkEmbeddedCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		getCheckedEmbeddableCartridges().add(cartridge);
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, getCheckedEmbeddableCartridges());
	}

	@Override
	public void uncheckEmbeddedCartridge(IEmbeddableCartridge cartridge) 
			throws OpenShiftException {
		getCheckedEmbeddableCartridges().remove(cartridge);
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, getCheckedEmbeddableCartridges());
	}

	public Set<IEmbeddableCartridge> refreshSelectedEmbeddedCartridges() throws OpenShiftException {
		wizardModel.refresh();
		setCheckedEmbeddableCartridges(wizardModel.getEmbeddedCartridges());
		return getCheckedEmbeddableCartridges();
	}

	public IApplicationProperties getApplicationProperties() {
		return wizardModel;
	}

	@Override
	public IDomain getDomain() throws OpenShiftException {
		return wizardModel.getDomain();
	}
}
