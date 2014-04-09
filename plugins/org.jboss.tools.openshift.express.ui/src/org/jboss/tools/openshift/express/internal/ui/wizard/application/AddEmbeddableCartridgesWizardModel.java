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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.core.CodeAnythingCartridge;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.IEmbeddedCartridgesModel;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IDomain;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class AddEmbeddableCartridgesWizardModel extends ObservablePojo implements IEmbeddedCartridgesModel {

	private IOpenShiftApplicationWizardModel wizardModel;
	private Set<ICartridge> checkedEmbeddableCartridges;

	public AddEmbeddableCartridgesWizardModel(IOpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		this.checkedEmbeddableCartridges = new HashSet<ICartridge>(getEmbeddedCartridges());
	}

	@Override
	public IDomain getDomain() {
		return wizardModel.getDomain();
	}

	@Override
	public List<ICartridge> getEmbeddableCartridges() {
		List<ICartridge> cartridges = new ArrayList<ICartridge>(wizardModel.getEmbeddableCartridges());
		cartridges.add(new CodeAnythingCartridge());
		cartridges.removeAll(wizardModel.getSelectedEmbeddableCartridges());
		return cartridges;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <C extends ICartridge> List<C> getEmbeddedCartridges() {
		return (List<C>) new ArrayList<ICartridge>(wizardModel.getSelectedEmbeddableCartridges());
	}

	@Override
	public boolean isEmbedded(ICartridge cartridge) {
		return wizardModel.hasEmbeddableCartridge(cartridge);
	}
	
	@Override
	public Set<ICartridge> setCheckedEmbeddableCartridges(Set<ICartridge> cartridges) {
		return checkedEmbeddableCartridges = cartridges;
	}

	@Override
	public Set<ICartridge> getCheckedEmbeddableCartridges() {
		return checkedEmbeddableCartridges;
	}

	@Override
	public void refresh() {
	}

	@Override
	public Connection getConnection() {
		return wizardModel.getConnection();
	}

	@Override
	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	@Override
	public IStandaloneCartridge getStandaloneCartridge() {
		return wizardModel.getStandaloneCartridge();
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return wizardModel.getApplicationScale();
	}

}
