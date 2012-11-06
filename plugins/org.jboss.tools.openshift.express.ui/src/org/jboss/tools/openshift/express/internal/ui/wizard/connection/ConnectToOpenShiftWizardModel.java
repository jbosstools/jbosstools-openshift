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

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 */
public class ConnectToOpenShiftWizardModel implements IConnectionAwareModel {
	
	protected Connection connection;
		
	public ConnectToOpenShiftWizardModel() {
	}

	public ConnectToOpenShiftWizardModel(final Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Connection getConnection() {
		return connection == null ? ConnectionsModelSingleton.getInstance().getRecentConnection() : connection;
	}

	@Override
	public Connection setConnection(Connection connection) {
		this.connection = connection;
		return connection;
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

}
