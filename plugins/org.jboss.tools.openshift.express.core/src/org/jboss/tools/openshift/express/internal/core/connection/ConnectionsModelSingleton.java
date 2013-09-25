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
package org.jboss.tools.openshift.express.internal.core.connection;

/**
 * @author Andre Dietisheim
 */
public class ConnectionsModelSingleton {

	private static ConnectionsModel model;

	public static ConnectionsModel getInstance() {
		if (model == null)
			model = new ConnectionsModel();
		return model;
	}

	private ConnectionsModelSingleton() {
		// inhibit instantiation
	}
}