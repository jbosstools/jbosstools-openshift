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
import java.util.function.Consumer;

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
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.cdk.reddeer.core.condition.MultipleWaitConditionHandler;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKServerAdapterType;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Extended Server class for purposes of Container Development Kit Server Adapter
 * @author odockal
 *
 */
public class CDKServer extends DefaultServer {
	
	private static Logger log = Logger.getLogger(CDKServer.class);
	
	private boolean certificateAccepted = false;
	
	private WaitCondition problemDialogWait = new ShellIsAvailable(CDKLabel.Shell.PROBLEM_DIALOG);
	private WaitCondition multipleDialogWait = new ShellIsAvailable(CDKLabel.Shell.MULTIPLE_PROBLEMS_DIALOG);
	private WaitCondition sslDialogWait = new ShellIsAvailable(CDKLabel.Shell.UNTRUSTED_SSL_DIALOG);
	
	private Map<WaitCondition, Consumer<Object>> waitConditionMatrix = new HashMap<WaitCondition, Consumer<Object>>();
	
	private final CDKServerAdapterType serverType;
	
	public CDKServer(TreeItem item) {
		super(item);
		this.serverType = CDKUtils.getCDKServerType(CDKUtils.getServerTypeIdFromItem(item));
		fillUpMatrix();
	}
	
	private void fillUpMatrix() {
		waitConditionMatrix.put(problemDialogWait, 
				(x) -> closeDialogAndThrowException((Shell) problemDialogWait.getResult()));
		waitConditionMatrix.put(multipleDialogWait, 
				(x) -> closeDialogAndThrowException((Shell) multipleDialogWait.getResult()));
		waitConditionMatrix.put(sslDialogWait,
				(x) -> confirmSSLCertificateDialog(sslDialogWait.getResult()));
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
		MultipleWaitConditionHandler waitConditions = new MultipleWaitConditionHandler(
				waitConditionMatrix);
		TimePeriod timeout = TimePeriod.VERY_LONG;
		if (menuItem == CDKLabel.ServerContextMenu.RESTART) { 
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
		waitForProblemDialog(waitConditions, menuItem, TimePeriod.DEFAULT);
		checkInitialStateChange(actualState);
		// decide if we wait for SSL acceptance dialog
		if ((actualState == ServerState.STOPPING || actualState == ServerState.STOPPED) 
				&& !getCertificatedAccepted()) {
			new WaitUntil(waitConditions, TimePeriod.getCustom(1020));
		}
		new WaitUntil(new ServerHasState(this, resultState), timeout);
		waitForProblemDialog(waitConditions, menuItem, TimePeriod.DEFAULT);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher("Inspecting CDK environment")), TimePeriod.DEFAULT); 
		log.debug("Operate server's state finished, the result server's state is: '" + getLabel().getState() + "'");  
	}
	
	public String getServerType() {
		return this.serverType.serverType();
	}
	
	private void waitForProblemDialog(WaitCondition wait, String menuItem, TimePeriod timeout) {
		log.info("Waiting for " + wait.description() + " to be fullfiled");  
		try {
			new WaitUntil(wait, timeout);
		} catch (WaitTimeoutExpiredException exc) {
			log.info(wait.description() + " was not fulfilled during CDK server " + menuItem); 
		}
	}
	
	private void processProblemDialog(DefaultShell shell, String excMessage) {
		log.info("Processing passed shell dialog " + shell.getText()); 
		new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT, false);
		DefaultShell shellDialog = new DefaultShell(shell.getSWTWidget());
		log.info("Shell could have changed after getting another error"); 
		log.info("Actual shell dialog name is " + shellDialog.getText()); 
		CDKUtils.captureScreenshot("CDEServer#ProblemDialog#" + shellDialog.getText()); 
		new OkButton(shellDialog).click();
		throw new CDKServerException(excMessage);
	}
	
	private void checkInitialStateChange(ServerState actualState) {
		try {
			new WaitUntil(new ServerHasState(this, actualState), TimePeriod.DEFAULT);
			String message = "Server's state went back to " + actualState; 
			throw new CDKServerException(message);	
		} catch (WaitTimeoutExpiredException exc) {
			log.info("Server's state changed to " + this.getLabel().getState() + 
					" and did not go back to " + actualState); 
		}
	}
	
	/**
	 * Methods waits for SSL Certificate dialog shell to appear and then confirms dialog, 
	 * it might happen that certificate is already in place and no dialog is shown,
	 * then WaitTimeoutExpiredException is logged but not raised
	 */
	private void confirmSSLCertificateDialog(Shell shell) {
		try {
			DefaultShell certificateDialog = new DefaultShell(shell);
			certificateDialog.setFocus();
			log.info("SSL Certificate Dialog appeared during " + getLabel().getState().toString()); 
			new PushButton(certificateDialog, CDKLabel.Buttons.YES).click(); 
			new WaitWhile(new ShellIsAvailable(certificateDialog));
			setCertificateAccepted(true);
		} catch (WaitTimeoutExpiredException ex) {
			String message ="WaitTimeoutExpiredException occured when handling Certificate dialog. " 
					+ "Dialog has not been shown"; 
			log.error(message);
			throw new CDKServerException(message, ex);
		}
	}
	
	
	private void closeDialogAndThrowException(Shell dialog) {
		log.error("Problems dialog appeared, throwing an exception"); 
		DefaultShell shell = new DefaultShell(dialog);
		processProblemDialog(shell, shell.getText() + 
				" dialog occured during server adapter state was " + getLabel().getState());		 
	}
}