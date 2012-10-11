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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.ConnectToOpenShiftWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizardPage;

/**
 * @author Xavier Coulon
 */
public class ConnectToOpenShiftWizard extends Wizard {

	private final ConnectionWizardPage page;
	
	/**
	 * Constructor to use when connecting with the default connection.
	 */
	public ConnectToOpenShiftWizard() {
		this(ConnectionsModel.getDefault().getRecentConnection());
	}
	
	/**
	 * Constructor to use when connection to use is known.
	 */
	public ConnectToOpenShiftWizard(final Connection connection) {
		this.page = new ConnectionWizardPage(this, new ConnectToOpenShiftWizardModel(connection));
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