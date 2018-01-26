/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.condition.WaitCondition;
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
public class CDKServer extends DefaultServer {
	
	private static final String SSL_DIALOG_NAME = "Untrusted SSL Certificate";
	
	private static final String MULTIPLE_PROBLEMS_DIALOG = "Multiple problems have occurred";
	
	private static final String PROBLEM_DIALOG = "Problem Dialog";
	
	private static Logger log = Logger.getLogger(CDKServer.class);
	
	private boolean certificateAccepted = false;
	
	private final CDKServerAdapterType serverType;
	
	public CDKServer(TreeItem item) {
		super(item);
		this.serverType = CDKUtils.getCDKServerType(CDKUtils.getServerTypeIdFromItem(item));
	}
	
	public void setCertificateAccepted(boolean accepted) {
		this.certificateAccepted = accepted;
	}
	
	public boolean getCertificatedAccepted() {
		return this.certificateAccepted;
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
		// later on, we might get "Multiple problems have occurred" dialog
		waitForProblemDialog(PROBLEM_DIALOG, menuItem, TimePeriod.DEFAULT);
		checkInitialStateChange(actualState);
		// decide if we wait for SSL acceptance dialog
		if ((actualState == ServerState.STOPPING || actualState == ServerState.STOPPED) 
				&& !getCertificatedAccepted()) {
			new WaitUntil(new ProblemOrCertificateDialogIsThrown(
					true, MULTIPLE_PROBLEMS_DIALOG, PROBLEM_DIALOG), TimePeriod.getCustom(900));
		}
		new WaitUntil(new ServerHasState(this, resultState), timeout);
		waitForProblemDialog(MULTIPLE_PROBLEMS_DIALOG, menuItem, TimePeriod.DEFAULT);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher("Inspecting CDK environment")), TimePeriod.DEFAULT);
		log.debug("Operate server's state finished, the result server's state is: '" + getLabel().getState() + "'");
	}
	
	public String getServerType() {
		return this.serverType.serverType();
	}
	
	private void waitForProblemDialog(String shellName, String menuItem, TimePeriod timeout) {
		try {
			new WaitUntil(new ShellIsAvailable(shellName), timeout);
			processProblemDialog(shellName, "Problem occured when trying to " 
								+ menuItem + " CDK server adapter");
		} catch (WaitTimeoutExpiredException exc) {
			log.info(shellName + " dialog did not appear on CDK server " + menuItem);
		}
	}
	
	private void processProblemDialog(String shellName, String excMessage) {
		CDKUtils.captureScreenshot("CDEServer#BeforeClosingProblemDialog");
		new OkButton(new DefaultShell(shellName)).click();
		throw new CDKServerException(excMessage);
	}
	
	private void checkInitialStateChange(ServerState actualState) {
		try {
			new WaitUntil(new ServerHasState(this, actualState), TimePeriod.DEFAULT);
			String message = "Server's state went back to " + actualState;
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Server's state changed and did not go back to " + actualState);
		}
	}
	
	private class ProblemOrCertificateDialogIsThrown extends AbstractWaitCondition {
		
		private WaitCondition sslDialog;
		private boolean expectDialog;
		private Map<String, WaitCondition> problemDialogs = new HashMap<String, WaitCondition>();
		
		public ProblemOrCertificateDialogIsThrown(boolean expectDialog, String... dialogNames) {
			this.expectDialog = expectDialog;
			this.sslDialog = new ShellIsAvailable(SSL_DIALOG_NAME);
			for (String dialog : dialogNames) {
				problemDialogs.put(dialog, new ShellIsAvailable(dialog));
			}
		}

		@Override
		public boolean test() {
			if (expectDialog && sslDialog.test()) {
				confirmSSLCertificateDialog();
				return true;
			}
			for (Entry<String, WaitCondition> dialogName : problemDialogs.entrySet()) {
				if (dialogName.getValue().test()) {
					closeDialogAndThrowException(dialogName.getKey());
				}
			}
			return false;
		}
		
		private void closeDialogAndThrowException(String dialog) {
			log.error("Problems dialog appeared, throwing an exception");
			processProblemDialog(dialog, dialog + 
					" occured during server adapter state was " + getLabel().getState());			
		}
		
		@Override
		public String description() {
			return expectDialog ? " appears either Untrusted SSL Certificate dialog "
					+ "or Multiple problems have occured dialog ..." 
					: " appears Multiple problems have occured dialog ...";
		}
		
		/**
		 * Methods waits for SSL Certificate dialog shell to appear and then confirms dialog, 
		 * it might happen that certificate is already in place and no dialog is shown,
		 * then WaitTimeoutExpiredException is logged but not raised
		 */
		private void confirmSSLCertificateDialog() {
			try {
				DefaultShell certificateDialog = new DefaultShell(SSL_DIALOG_NAME);
				certificateDialog.setFocus();
				log.info("SSL Certificate Dialog appeared during " + getLabel().getState().toString());
				new PushButton(certificateDialog, "Yes").click();
				new WaitWhile(new ShellIsAvailable(SSL_DIALOG_NAME));
				setCertificateAccepted(true);
			} catch (WaitTimeoutExpiredException ex) {
				String message ="WaitTimeoutExpiredException occured when handling Certificate dialog. "
						+ "Dialog has not been shown";
				log.error(message);
				throw new CDKServerException(message, ex);
			}
		}
	}
}