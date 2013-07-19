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
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

/**
 * @author Xavier Coulon
 */
public class ConnectionWizard extends Wizard {

	private final ConnectionWizardPage page;
	
	/**
	 * Constructor to use when connecting with the default connection.
	 */
	public ConnectionWizard() {
		this(ConnectionsModelSingleton.getInstance().getRecentConnection());
	}
	
	public ConnectionWizard(final Connection connection) {
		this(connection, true);
	}

	/**
	 * Constructor to use when connection to use is known.
	 */
	public ConnectionWizard(final Connection connection, boolean allowConnectionChange) {
		this.page = new ConnectionWizardPage(this, new ConnectionWizardModel(connection), allowConnectionChange);
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public boolean performFinish() {
		return page.connect();
	}

	@Override
	public void addPages() {
		addPage(page);
	}
	
	public Connection getConnection() {
		return page.getConnection();
	}
}