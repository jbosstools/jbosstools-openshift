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
package org.jboss.tools.cdk.reddeer.server.ui;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersViewEnums.ServerState;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.condition.ServerHasState;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;

/**
 * Extended Server class for purposes of Container Development Kit Server Adapter
 * @author odockal
 *
 */
public class CDEServer extends Server {
	
	private static final String SSL_DIALOG_NAME = "Untrusted SSL Certificate";
	
	private static Logger log = Logger.getLogger(CDEServer.class);
	
	private boolean certificateAccepted = false;
	
	protected boolean cdk3 = false;
	
	public CDEServer(TreeItem item, ServersView view) {
		super(item, view);
	}
	
	public CDEServer(TreeItem item, ServersView view, boolean cdk3) {
		super(item, view);
		this.cdk3 = cdk3;
	}
	
	public void setCertificateAccepted(boolean accepted) {
		this.certificateAccepted = accepted;
	}
	
	@Override
	protected void operateServerState(String menuItem, final ServerState resultState) {
		ServerState actualState = this.getLabel().getState();
		TimePeriod timeout = TimePeriod.VERY_LONG;
		if (menuItem == "Restart") {
			timeout = TimePeriod.getCustom(480);
		}
		log.debug("Operate server's state from: + '" + actualState + "' to '" + menuItem + "'");
		select();
		new ContextMenu(menuItem).select();
		// waiting until servers's state has changed from initial state into something else, 
		// ie. stopped -> starting or started -> stopping
		new WaitWhile(new ServerHasState(this, actualState), TimePeriod.NORMAL);
		// we might expect that after the state is changed it should not go back into initial state
		// or that problem dialog appears
		try {
			new WaitUntil(new ShellWithTextIsAvailable("Problem Occured"), TimePeriod.NORMAL);
			new DefaultShell("Problem Occured");
			new OkButton().click();
			String message = "Problem occured when trying to " + menuItem
					+ " CDK server adapter";
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Problem Occured dialog did not appear on CDK server " + menuItem);
		}
		try {
			new WaitUntil(new ServerHasState(this, actualState), TimePeriod.NORMAL);
			String message = "Server's state went back to " + actualState;
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Server's state changed and did not go back to " + actualState);
		}
		if ((actualState == ServerState.STOPPING || actualState == ServerState.STOPPED) && !certificateAccepted) {
			confirmSSLCertificateDialog();
		}
		new WaitUntil(new ServerHasState(this, resultState), timeout);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher("Inspecting CDK environment")), TimePeriod.LONG);
		log.debug("Operate server's state finished, the result server's state is: '" + getLabel().getState() + "'");
	}
	
	/**
	 * Methods waits for SSL Certificate dialog shell to appear and then confirms dialog, 
	 * it might happen that certificate is already in place and no dialog is shown,
	 * then WaitTimeoutExpiredException is logged but not raised
	 */
	private void confirmSSLCertificateDialog() {
		try {
			new WaitUntil(new ShellWithTextIsAvailable(SSL_DIALOG_NAME), this.cdk3 ? TimePeriod.getCustom(600) : TimePeriod.getCustom(300));
			new DefaultShell(SSL_DIALOG_NAME);
			new PushButton("Yes").click();
			log.info("SSL Certificate Dialog appeared during " + this.getLabel().getState().toString());
			new WaitWhile(new ShellWithTextIsAvailable(SSL_DIALOG_NAME));
			setCertificateAccepted(true);
		} catch (WaitTimeoutExpiredException ex) {
			String message ="WaitTimeoutExpiredException occured when handling Certificate dialog. "
					+ "Dialog has not been shown";
			log.error(message);
			throw new CDKServerException(message, ex);
		}
	}
}