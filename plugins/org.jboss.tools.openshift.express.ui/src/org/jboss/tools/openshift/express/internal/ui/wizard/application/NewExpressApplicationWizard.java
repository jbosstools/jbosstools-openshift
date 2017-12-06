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

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IDomain;

/**
 * A wizard to create a new OpenShift application.
 * 
 * @author Xavier Coulon
 * @author Andre Dietisheim
 * 
 */
public class NewExpressApplicationWizard extends ExpressApplicationWizard {

	/**
	 * Constructor invoked via File->Import
	 */
	public NewExpressApplicationWizard() {
		super(ConnectionsRegistrySingleton.getInstance().getRecentConnection(ExpressConnection.class), null, null, null,
				false, "New OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public NewExpressApplicationWizard(IDomain domain) {
		super(ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance()),
				domain, null, null, false, "New OpenShift Application");
	}

	/**
	 * Constructor invoked via OpenShift Explorer context menu
	 */
	public NewExpressApplicationWizard(ExpressConnection connection) {
		super(connection, connection.getDefaultDomain(), null, null, false, "New OpenShift Application");
	}
}
