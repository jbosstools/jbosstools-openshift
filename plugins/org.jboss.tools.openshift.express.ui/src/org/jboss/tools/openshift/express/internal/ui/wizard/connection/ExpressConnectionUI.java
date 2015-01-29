/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.IConnectionUI;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionUI implements IConnectionUI<ExpressConnection> {

	public ExpressConnectionUI() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean handles(Class<ExpressConnection> clazz) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canConnect(String host) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExpressConnection create() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void edit(ExpressConnection connection) {
		// TODO Auto-generated method stub
		
	}

}
