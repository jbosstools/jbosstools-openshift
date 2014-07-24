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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class RemoveConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Connection[] connections = UIUtils.getElements(HandlerUtil.getCurrentSelection(event), Connection.class);
		if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event)
				, "Remove connection"
				, NLS.bind("You are about to remove the connection(s) {0}.\n\n"
						+ "Do you want to continue?", getConnectionIds(connections)))) {
			for(Connection connection : connections){
				ConnectionsModelSingleton.getInstance().removeConnection(connection);
			}
		}
		return null;
	}
	
	String getConnectionIds(Connection[] connections){
		String ids="";
		boolean first = true;
		for(Connection connection : connections){
			if(!first){
				ids += ", ";
			}
			ids += connection.getId();
			first = false;
		}
		return ids;
	}
}
