/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.tm.internal.terminal.control.impl.ITerminalControlForText;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.ILauncherDelegate;
import org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants;
import org.eclipse.tm.terminal.view.ui.launcher.LauncherDelegateManager;
import org.eclipse.tm.terminal.view.ui.manager.ConsoleManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class TerminalUtility {

	public static void openConsole(Map<String, Object> props, ITerminalService.Done d) {
		IViewPart vp = ViewUtils.openTerminalView();
	   	ITerminalService terminal = TerminalServiceFactory.getService();
		// If not available, we cannot fulfill this request
		if (terminal != null) {
			terminal.openConsole(props, d);
		}
	}
	
	public static Map<String, Object> getPropertiesForServer(IServer s) {

		final Map<String, Object> props = new HashMap<>();
		String workingDir = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		props.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, workingDir);
		props.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, "org.eclipse.tm.terminal.connector.local.LocalConnector"); //$NON-NLS-1$
		props.put(ITerminalsConnectorConstants.PROP_IP_HOST, "localhost");
		props.put(ITerminalsConnectorConstants.PROP_TIMEOUT, Integer.valueOf(0));
		props.put(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE, Integer.valueOf(300));
		props.put(ITerminalsConnectorConstants.PROP_ENCODING, null);
		props.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "org.eclipse.tm.terminal.connector.local.launcher.local");
		props.put(ITerminalsConnectorConstants.PROP_TITLE, s.getName());
		
		return props;
	}
	
	
	public static ITerminalControlForText findTerminalControl(Map<String, Object> props) {
		// Do the rest
		String id = (String)props.get(ITerminalsConnectorConstants.PROP_ID);
		id =  id != null ? id : IUIConstants.ID;
		String secondaryId = (String)props.get(ITerminalsConnectorConstants.PROP_SECONDARY_ID);
		String title = (String)props.get(ITerminalsConnectorConstants.PROP_TITLE);
		Object data = props.get(ITerminalsConnectorConstants.PROP_DATA);
		final ITerminalConnector connector = createTerminalConnector(props);
		CTabItem item = ConsoleManager.getInstance().findConsole(id,  secondaryId, title, connector, data);
		if( item != null ) {
			Object data22 = item.getData();
			if( data22 instanceof ITerminalControlForText) {
				return (ITerminalControlForText)data22;
			}
		}
		return null;
	}
	
	

	private static ITerminalConnector createTerminalConnector(Map<String, Object> properties) {
		Assert.isNotNull(properties);
		// The terminal connector result object
		ITerminalConnector connector = null;
		// Get the launcher delegate id from the properties
		String delegateId = (String)properties.get(ITerminalsConnectorConstants.PROP_DELEGATE_ID);
		if (delegateId != null) {
			// Get the launcher delegate
			ILauncherDelegate delegate = LauncherDelegateManager.getInstance().getLauncherDelegate(delegateId, false);
			if (delegate != null) {
				// Create the terminal connector
				connector = delegate.createTerminalConnector(properties);
			}
		}
		return connector;
	}
	
}
