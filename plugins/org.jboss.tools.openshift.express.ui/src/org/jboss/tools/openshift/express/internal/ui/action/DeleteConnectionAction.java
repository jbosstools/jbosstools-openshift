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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Xavier Coulon
 */
public class DeleteConnectionAction extends AbstractOpenShiftAction {

	public DeleteConnectionAction() {
		super(OpenShiftExpressUIMessages.DELETE_CONNECTION_ACTION, true);
	}

	@Override
	public void run() {
		final Connection connection = UIUtils.getFirstElement(getSelection(), Connection.class);
		if (connection == null) {
			return;
		}
		ConnectionsModelSingleton.getInstance().removeConnection(connection);
	}

}