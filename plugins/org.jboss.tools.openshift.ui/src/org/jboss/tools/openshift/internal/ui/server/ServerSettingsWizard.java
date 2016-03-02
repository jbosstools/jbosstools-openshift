/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IService;

/**
 * A single-page wizard to configure a Server Adapter from the OpenShift
 * Explorer view.
 */
public class ServerSettingsWizard extends Wizard {

	private final ServerSettingsWizardPage serverSettingsWizardPage;
	
	private IServer createdServer = null;
	
	/**
	 * Default constructor.
	 * @param server the working copy of the {@link IServer} to create
	 * @param connection the current OpenShift {@link Connection}
	 */
	public ServerSettingsWizard(final IServerWorkingCopy server, final Connection connection) {
		this.serverSettingsWizardPage = new ServerSettingsWizardPage(this, server, connection);
	}
	
	/**
	 * Full constructor.
	 * @param server the working copy of the {@link IServer} to create
	 * @param connection the current OpenShift {@link Connection}
	 * @param service the selected service
	 */
	public ServerSettingsWizard(final IServerWorkingCopy server, final Connection connection, final IService service) {
		this.serverSettingsWizardPage = new ServerSettingsWizardPage(this, server, connection, service);
	}
	
	@Override
	public void addPages() {
		addPage(this.serverSettingsWizardPage);
	}
	
	@Override
	public boolean performFinish() {
		this.serverSettingsWizardPage.getModel().updateServer();
		try {
			this.createdServer = this.serverSettingsWizardPage.getModel().getServer().save(true, new NullProgressMonitor());
		} catch (CoreException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Failed to create the Server Adapter", e);
		}
		return true;
	}

	/**
	 * @return the {@link IServer} that was created or <code>null</code> if the
	 *         operation failed or was cancelled.
	 */
	public IServer getCreatedServer() {
		return createdServer;
	}
}
