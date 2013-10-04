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

import java.io.IOException;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardModel extends EditDomainWizardModel {

	public NewDomainWizardModel(Connection connection) {
		super(connection);
	}

	public void createDomain() throws OpenShiftException, IOException {
		Connection connection = getConnection();
		if (connection == null) {
			Logger.error("Could not create domain, missing connection.");
		}
		connection.createDomain(getDomainId());
	}

	@Override
	public boolean isCurrentDomainId(String domainId) {
		return false;
	}

}
