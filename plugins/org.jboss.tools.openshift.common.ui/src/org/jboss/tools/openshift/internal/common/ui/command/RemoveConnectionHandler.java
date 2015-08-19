/*******************************************************************************
 * Copyright (c) 2013-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.command;

import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class RemoveConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IConnection[] connections = UIUtils.getElements(HandlerUtil.getCurrentSelection(event), IConnection.class);
		if( connections.length == 0 )
			return null;

		if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event)
				, "Remove connection"
				, NLS.bind("You are about to remove the connection(s):\n{0}\n\n"
						+ "Do you want to continue?\n\n",
						StringUtils.toString(
								Arrays.asList(connections),
								new StringUtils.ToStringConverter<IConnection>() {

									@Override
									public String toString(IConnection connection) {
										return connection == null ? "" : ("\n" + connection.toString());
									}
								})
						)
				)) {
			for(IConnection connection : connections){
				if( connection != null )
					ConnectionsRegistrySingleton.getInstance().remove(connection);
			}

		}
		return null;
	}
}
