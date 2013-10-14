/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractDomainHandler extends AbstractHandler {

	protected Connection getConnection(ExecutionEvent event) {
		Connection connection = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), Connection.class);
		if (connection == null) {
			Logger.warn("Could not launch domain wizard, no connection selected");
			return null;
		}

		if (!connection.connect()) {
			Logger.warn("Could not launch domain wizard, connection is not connected");
			return null;
		}
		return connection;
	}
}
