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
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * @author Xavier Coulon
 */
public class ConnectionWizard extends Wizard {

	private final ConnectionWizardPage page;
	
	public static final String NEW_CONNECTION_TITLE = "New OpenShift Connection";
	public static final String EDIT_CONNECTION_TITLE = "Edit OpenShift Connection";
	
	/**
	 * Constructor to use when connecting with the default connection.
	 * New connection title is used as default wizard title.
	 * 
	 */
	public ConnectionWizard() {
		this(ConnectionsRegistrySingleton.getInstance().getRecentConnection(), 
				NEW_CONNECTION_TITLE);
	}
	
	/**
	 * Constructor to use when connecting with the default connection.
	 * 
	 * @param title wizard title
	 */
	public ConnectionWizard(String title) {
		this(ConnectionsRegistrySingleton.getInstance().getRecentConnection(), title);
	}
	
	public ConnectionWizard(final IConnection connection) {
		this(connection, NEW_CONNECTION_TITLE);
	}
	
	public ConnectionWizard(final IConnection connection, String title) {
		this(connection, null, title);
	}

	/**
	 * Constructor to use when connection to use is known.
	 * New connection title is used as default wizard title.
	 * @param context  A context that is useful to ConnectionEditors
	 */
	public ConnectionWizard(final IConnection connection, Object context) {
		this(connection, context, NEW_CONNECTION_TITLE);
	}
	
	/**
	 * Constructor to use when connection to use is known.
	 * @param context  A context that is useful to ConnectionEditors
	 */
	public ConnectionWizard(final IConnection connection, Object context, String title) {
		this.page = new ConnectionWizardPage(this, new ConnectionWizardModel(connection, context));
		setNeedsProgressMonitor(true);
		if (title != null) {
			setWindowTitle(title);
		}
	}
	
	@Override
	public boolean performFinish() {
		return page.connect();
	}

	@Override
	public void addPages() {
		addPage(page);
	}
}