/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.ui;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersViewEnums.ServerState;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.condition.ServerHasState;

/**
 * Extended Server class for purposes of Container Development Kit Server Adapter
 * @author odockal
 *
 */
public class CDEServer extends Server {
	
	private static Logger log = Logger.getLogger(CDEServer.class);
	
	private static final TimePeriod TIMEOUT = TimePeriod.getCustom(600);
	
	public CDEServer(TreeItem item, ServersView view) {
		super(item, view);
	}
	
	@Override
	protected void operateServerState(String menuItem, final ServerState resultState) {
		ServerState actualState = this.getLabel().getState();
		log.debug("Operate server's state: '" + menuItem + "'");
		select();
		new ContextMenu(menuItem).select();
		new WaitUntil(new JobIsRunning(), menuItem == "Restart" ? TimePeriod.getCustom(900) : TIMEOUT);
		if (actualState == ServerState.STOPPING || actualState == ServerState.STOPPED) {
			confirmSSLCertificateDialog();
		}
		new WaitUntil(new ServerHasState(this, resultState), TIMEOUT);
		new WaitWhile(new JobIsRunning(), TIMEOUT);
		log.debug("Operate server's state finished, the result server's state is: '" + getLabel().getState() + "'");
	}
	
	/**
	 * Methods waits for SSL Certificate dialog shell to appear and then confirms dialog, 
	 * it might happen that certificate is already in place and no dialog is shown,
	 * then WaitTimeoutExpiredException is logged but not raised
	 */
	private void confirmSSLCertificateDialog() {
		try {
			new WaitUntil(new ShellWithTextIsAvailable("Untrusted SSL Certificate"), TimePeriod.getCustom(300));
			new DefaultShell("Untrusted SSL Certificate");
			new PushButton("Yes").click();
			log.info("SSL Certificate Dialog appeared during " + this.getLabel().getState().toString());
			new WaitWhile(new ShellWithTextIsAvailable("Untrusted SSL Certificate"));
		} catch (WaitTimeoutExpiredException ex) {
			log.info("WaitTimeoutExpiredException occured when handling Certificate dialog. "
					+ "Dialog has not been shown");
		}
	}
}
