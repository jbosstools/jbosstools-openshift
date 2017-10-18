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

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ServerHasState;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.DefaultServer;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.adapter.CDKServerAdapterType;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Extended Server class for purposes of Container Development Kit Server Adapter
 * @author odockal
 *
 */
public class CDEServer extends DefaultServer {
	
	private static final String SSL_DIALOG_NAME = "Untrusted SSL Certificate";
	
	private static Logger log = Logger.getLogger(CDEServer.class);
	
	private boolean certificateAccepted = false;
	
	private final CDKServerAdapterType serverType;
	
	public CDEServer(TreeItem item) {
		super(item);
		this.serverType = CDKUtils.getCDKServerType(CDKUtils.getServerTypeIdFromItem(item));
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
		new ContextMenuItem(menuItem).select();
		// waiting until servers's state has changed from initial state into something else, 
		// ie. stopped -> starting or started -> stopping
		new WaitWhile(new ServerHasState(this, actualState), TimePeriod.DEFAULT);
		// we might expect that after the state is changed it should not go back into initial state
		// or that problem dialog appears
		try {
			new WaitUntil(new ShellIsAvailable("Problem Occured"), TimePeriod.DEFAULT);
			new DefaultShell("Problem Occured");
			new OkButton().click();
			String message = "Problem occured when trying to " + menuItem
					+ " CDK server adapter";
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Problem Occured dialog did not appear on CDK server " + menuItem);
		}
		try {
			new WaitUntil(new ServerHasState(this, actualState), TimePeriod.DEFAULT);
			String message = "Server's state went back to " + actualState;
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Server's state changed and did not go back to " + actualState);
		}
		if ((actualState == ServerState.STOPPING || actualState == ServerState.STOPPED) && !certificateAccepted) {
			confirmSSLCertificateDialog();
		}
		new WaitUntil(new ServerHasState(this, resultState), timeout);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher("Inspecting CDK environment")), TimePeriod.DEFAULT);
		log.debug("Operate server's state finished, the result server's state is: '" + getLabel().getState() + "'");
	}
	
	/**
	 * Methods waits for SSL Certificate dialog shell to appear and then confirms dialog, 
	 * it might happen that certificate is already in place and no dialog is shown,
	 * then WaitTimeoutExpiredException is logged but not raised
	 */
	private void confirmSSLCertificateDialog() {
		try {
			log.info(isCDK3() ? "CDK 3 server adapter has extended timeout set to 720s" : "CDK 2.x server adapter has timeout set to 300s");
			new WaitUntil(new ShellIsAvailable(SSL_DIALOG_NAME), isCDK3() ? TimePeriod.getCustom(720) : TimePeriod.VERY_LONG);
			new DefaultShell(SSL_DIALOG_NAME);
			new PushButton("Yes").click();
			log.info("SSL Certificate Dialog appeared during " + this.getLabel().getState().toString());
			new WaitWhile(new ShellIsAvailable(SSL_DIALOG_NAME));
			setCertificateAccepted(true);
		} catch (WaitTimeoutExpiredException ex) {
			String message ="WaitTimeoutExpiredException occured when handling Certificate dialog. "
					+ "Dialog has not been shown";
			log.error(message);
			throw new CDKServerException(message, ex);
		}
	}
	
	private boolean isCDK3() {
		return (this.serverType == CDKServerAdapterType.CDK3 ||
				this.serverType == CDKServerAdapterType.CDK32);
	}
	
	public String getServerType() {
		return this.serverType.serverType();
	}
}