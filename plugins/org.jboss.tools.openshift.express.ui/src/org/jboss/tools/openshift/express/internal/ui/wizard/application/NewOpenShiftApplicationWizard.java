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

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

import com.openshift.client.IDomain;

/**
 * A wizard to create a new OpenShift application.
 * 
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * 
 */
public class NewOpenShiftApplicationWizard extends OpenShiftApplicationWizard {

	/**
	 * Constructor invoked via File->Import
	 */
	public NewOpenShiftApplicationWizard() {
		super(ConnectionsModelSingleton.getInstance().getRecentConnection(),
				null, null, null, false, true, "New OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public NewOpenShiftApplicationWizard(IDomain domain) {
		super(ConnectionsModelSingleton.getInstance().getConnectionByResource(domain.getUser()),
				domain, null, null, false, false, "New OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public NewOpenShiftApplicationWizard(Connection connection) {
		super(connection, null, null, null, false, false, "New OpenShift Application");
	}
}
