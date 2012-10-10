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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;

/**
 * @author Andre Dietisheim
 */
public class ConnectToOpenShiftWizardModel implements IConnectionAwareModel {
	
	protected Connection user;
		
	public ConnectToOpenShiftWizardModel() {
	}

	/**
	 * Constructor 
	 * @param user the user to use to connect to OpenShift.
	 */
	public ConnectToOpenShiftWizardModel(final Connection user) {
		this.user = user;
	}
	
	@Override
	public Connection getConnection() {
		return user == null ? ConnectionsModel.getDefault().getRecentConnection() : user;
	}

	@Override
	public Connection setConnection(Connection user) {
		this.user = user;
		return user;
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

}
