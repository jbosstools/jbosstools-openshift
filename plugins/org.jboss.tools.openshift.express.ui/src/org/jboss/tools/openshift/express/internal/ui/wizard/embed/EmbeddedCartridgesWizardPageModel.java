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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy.IApplicationProperties;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizardPageModel extends ObservableUIPojo implements IApplicationProperties {

	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_CHECKED_CARTRIDGES = "checkedCartridges";

	private EmbeddedCartridgesWizardModel applicationCartridges;
	private List<ICartridge> embeddableCartridges = new ArrayList<ICartridge>();
	private ICartridge selectedCartridge;

	public EmbeddedCartridgesWizardPageModel(EmbeddedCartridgesWizardModel applicationCartridges) {
		this.applicationCartridges = applicationCartridges;
	}
	
	public void loadOpenShiftResources() throws OpenShiftException {
		loadEmbeddableCartridges();
	}
	
	private void loadEmbeddableCartridges() throws OpenShiftException {
		List<ICartridge> cartridges = applicationCartridges.getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		setCheckedCartridges(applicationCartridges.getEmbeddedCartridges());
	}

	public void setEmbeddableCartridges(List<ICartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddableCartridges, this.embeddableCartridges = cartridges);
	}

	public List<ICartridge> getEmbeddableCartridges() {
		return embeddableCartridges;
	}

	/**
	 * Returns the embeddable cartridges that are checked in the checkbox table.
	 * These are the cartridges that will get added once the wizard is finished.
	 * 
	 * @return the checked cartridges.
	 * @throws OpenShiftException
	 */
	public Set<ICartridge> getCheckedCartridges() {
		return applicationCartridges.getCheckedEmbeddableCartridges();
	}

	public void setCheckedCartridges(Set<ICartridge> cartridges) throws OpenShiftException {
		firePropertyChange(PROPERTY_CHECKED_CARTRIDGES, null, applicationCartridges.setCheckedEmbeddableCartridges(cartridges));
	}
	
	public void uncheckAll() throws OpenShiftException {
		setCheckedCartridges(new HashSet<ICartridge>());
	}

	public void setSelectedCartridge(ICartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE, selectedCartridge, this.selectedCartridge = cartridge);
	}
	
	public ICartridge getSelectedCartridge() {
		return selectedCartridge;
	}

	public boolean isEmbedded(ICartridge cartridge) throws OpenShiftException {
		return applicationCartridges.isEmbedded(cartridge);
	}

	public boolean hasApplication(IStandaloneCartridge cartridge) throws OpenShiftException {
		IDomain domain = getDomain();
		if (domain == null) {
			return false;
		}
		return domain.hasApplicationByCartridge(cartridge);
	}

	public void checkEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException {
		getCheckedCartridges().add(cartridge);
		firePropertyChange(PROPERTY_CHECKED_CARTRIDGES, null, getCheckedCartridges());
	}

	public void uncheckEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException {
		getCheckedCartridges().remove(cartridge);
		firePropertyChange(PROPERTY_CHECKED_CARTRIDGES, null, getCheckedCartridges());
	}

	public Set<ICartridge> refreshSelectedEmbeddedCartridges() throws OpenShiftException {
		applicationCartridges.refresh();
		setCheckedCartridges(applicationCartridges.getEmbeddedCartridges());
		return getCheckedCartridges();
	}

	public IDomain getDomain() throws OpenShiftException {
		return applicationCartridges.getDomain();
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return applicationCartridges.getApplicationScale();
	}

	@Override
	public ICartridge getStandaloneCartridge() {
		return applicationCartridges.getStandaloneCartridge();
	}

	@Override
	public String getApplicationName() {
		return applicationCartridges.getApplicationName();
	}
}
