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
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardModel extends DomainWizardModel {

	public NewDomainWizardModel(Connection connection) {
		super(connection);
	}

	public void createDomain() throws OpenShiftException {
		Connection connection = getConnection();
		if (connection == null) {
			Logger.error("Could not create domain, missing connection.");
		}
		connection.createDomain(getDomainId());
		ConnectionsModelSingleton.getInstance().fireConnectionChanged(connection);
	}

	@Override
	public boolean isCurrentDomainId(String domainId) {
		return false;
	}

}
