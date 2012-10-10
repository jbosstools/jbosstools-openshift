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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author Xavier Coulon
 */
public class OpenShiftExplorerContentCategory {

	private final Connection user;
	
	public OpenShiftExplorerContentCategory(final Connection user) {
		this.user = user;
	}

	/**
	 * @return the user
	 */
	public Connection getUser() {
		return user;
	}
	
	
}
