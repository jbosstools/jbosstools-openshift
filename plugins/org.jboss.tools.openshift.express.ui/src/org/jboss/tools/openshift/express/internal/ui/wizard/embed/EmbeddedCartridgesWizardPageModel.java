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
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizardPageModel extends ObservableUIPojo implements IEmbedCartridgesWizardPageModel {

	private IEmbeddedCartridgesModel wizardModel;
	private List<ICartridge> embeddedCartridges = new ArrayList<ICartridge>();
	private ICartridge selectedEmbeddableCartridge ;

	public EmbeddedCartridgesWizardPageModel(IEmbeddedCartridgesModel wizardModel) {
		this.wizardModel = wizardModel;
	}
	
	public List<ICartridge> loadEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException {
		List<ICartridge> cartridges = wizardModel.getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		return cartridges;
	}

	public void setEmbeddableCartridges(List<ICartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddedCartridges, this.embeddedCartridges = cartridges);
	}

	public List<ICartridge> getEmbeddedCartridges() {
		return embeddedCartridges;
	}

	@Override
	public Set<ICartridge> getCheckedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getCheckedEmbeddableCartridges();
	}

	protected void setCheckedEmbeddableCartridges(List<? extends ICartridge> cartridges) throws OpenShiftException {
		setCheckedEmbeddableCartridges(new HashSet<ICartridge>(cartridges));
	}
	
	@Override
	public void setCheckedEmbeddableCartridges(Set<ICartridge> cartridges) throws OpenShiftException {
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, wizardModel.setCheckedEmbeddableCartridges(cartridges));
	}
	
	@Override
	public void setSelectedEmbeddableCartridge(ICartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGE, selectedEmbeddableCartridge, this.selectedEmbeddableCartridge = cartridge);
	}
	
	@Override
	public ICartridge getSelectedEmbeddableCartridge() {
		return selectedEmbeddableCartridge;
	}

	public boolean isEmbedded(ICartridge cartridge) throws OpenShiftException {
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
	public void checkEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException {
		getCheckedEmbeddableCartridges().add(cartridge);
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, getCheckedEmbeddableCartridges());
	}

	@Override
	public void uncheckEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException {
		getCheckedEmbeddableCartridges().remove(cartridge);
		firePropertyChange(PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES, null, getCheckedEmbeddableCartridges());
	}

	public Set<ICartridge> refreshSelectedEmbeddedCartridges() throws OpenShiftException {
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
