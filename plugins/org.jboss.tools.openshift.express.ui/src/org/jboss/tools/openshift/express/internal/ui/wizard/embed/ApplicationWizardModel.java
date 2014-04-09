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

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardModel extends ObservablePojo implements IEmbeddedCartridgesModel {

	private IApplication application;
	private Connection connection;
	private Set<ICartridge> checkedEmbeddableCartridges;

	public ApplicationWizardModel(IApplication application, Connection connection) {
		Assert.isLegal(application != null, "No application provided");
		this.application = application;
		this.connection = connection;
		this.checkedEmbeddableCartridges = new HashSet<ICartridge>(getEmbeddedCartridges());
	}

	@Override
	public IDomain getDomain() {
		return application.getDomain();
	}

	@Override
	public List<ICartridge> getEmbeddableCartridges() {
		return new ArrayList<ICartridge>(connection.getEmbeddableCartridges());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends ICartridge> List<C> getEmbeddedCartridges() {
		return (List<C>) application.getEmbeddedCartridges();
	}

	@Override
	public boolean isEmbedded(ICartridge cartridge) throws OpenShiftException {
		return application.hasEmbeddedCartridge(cartridge.getName());
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
		application.refresh();
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	public IApplication getApplication() {
		return application;
	}

	@Override
	public String getApplicationName() {
		return application.getName();
	}

	@Override
	public IStandaloneCartridge getStandaloneCartridge() {
		return application.getCartridge();
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return application.getApplicationScale();
	}
}
