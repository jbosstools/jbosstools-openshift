/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 */
public class ConnectionWizardModel implements IConnectionAware<IConnection> {
	
	protected IConnection connection;
	private Object context;

	public ConnectionWizardModel(final IConnection connection, Object context) {
		this.connection = connection;
		this.context = context;
	}

	@Override
	public Object getContext() {
		return this.context;
	}
	
	@Override
	public IConnection getConnection() {
		return connection;
	}

	@Override
	public void setConnection(IConnection connection) {
		this.connection = connection;
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

}
