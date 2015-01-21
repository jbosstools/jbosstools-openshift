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

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 */
class ConnectionWizardModel implements IConnectionAwareModel {
	
	protected IConnection connection;

	ConnectionWizardModel(final IConnection connection) {
		this.connection = connection;
	}
	
	@Override
	public IConnection getConnection() {
		return connection;
	}

	@Override
	public IConnection setConnection(IConnection connection) {
		this.connection = connection;
		return connection;
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

}
