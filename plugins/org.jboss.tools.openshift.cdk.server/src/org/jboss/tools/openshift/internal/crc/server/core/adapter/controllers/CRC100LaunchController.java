/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.crc.server.core.adapter.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.util.ArgsUtil;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.AbstractCDKLaunchController;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Poller;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;
import org.jboss.tools.openshift.internal.crc.server.ui.view.SetupCRCJob;

public class CRC100LaunchController extends AbstractCDKLaunchController
		implements ILaunchServerController, IExternalLaunchConstants {

	@Override
	public void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		final IServer s = getServerFromLaunch(wc);
		String bin = s.getAttribute(CRC100Server.PROPERTY_BINARY_FILE, (String)null);
		wc.setAttribute(FLAG_INITIALIZED, true);
		String workingDir = new File(bin).getParentFile().getAbsolutePath();
		wc.setAttribute(ATTR_WORKING_DIR, workingDir);
	}

	protected IServer getServerFromLaunch(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		return ServerUtil.getServer(wc);
	}

	@Override
	protected void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
		// Overrides, things that should always match whats in server editor
		final IServer s = getServerFromLaunch(workingCopy);
		final CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		performOverrides(workingCopy, s, cdkServer);
	}

	/*
	 * Not expected to be extended. 
	 */
	protected void performOverrides(ILaunchConfigurationWorkingCopy workingCopy, IServer s, CDKServer cdkServer)
			throws CoreException {
		String workingDir = JBossServerCorePlugin.getServerStateLocation(s).toOSString();
		workingCopy.setAttribute(ATTR_WORKING_DIR, workingDir);

		Map<String, String> env = workingCopy.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String, String>) null);
		env = (env == null ? new HashMap<>() : new HashMap<>(env));

		setCRCLocationOnLaunchConfig(s, workingCopy, env);
		workingCopy.setAttribute(ENVIRONMENT_VARS_KEY, env);

		String secretFile = s.getAttribute(CRC100Server.PROPERTY_PULL_SECRET_FILE, (String)null);
		String secretArgs = secretFile == null ? "" : "-p \"" + secretFile + "\""; 
		String defaultArgs = "start " + secretArgs;
		
		String currentVal = workingCopy.getAttribute(ATTR_ARGS, defaultArgs);
		String replaced = ArgsUtil.setArg(currentVal, "-p", null, secretFile);
		workingCopy.setAttribute(ATTR_ARGS, replaced);

		// This is a bit of a hack for JBIDE-25350   - The launch config will APPEAR to be renamed, but, 
		// the official renaming will only occur if the user makes changes to the launch config and saves it.
		// Otherwise this is simply cosmetic. 
		String wcName = workingCopy.getName();
		String serverName = s.getName();
		if (!wcName.equals(serverName))
			workingCopy.rename(serverName);
	}

	protected CRC100Server getCRCServer(IServer server) {
		return (CRC100Server)server.loadAdapter(CRC100Server.class, new NullProgressMonitor());
	}
	protected String getCRCHome(IServer server) {
		CRC100Server s1 = getCRCServer(server);
		return s1 == null ? null : s1.getCRCHome(server);
	}

	private void setCRCLocationOnLaunchConfig(IServer s, ILaunchConfigurationWorkingCopy workingCopy,
			Map<String, String> env) throws CoreException {
		String binLoc = s.getAttribute(CRC100Server.PROPERTY_BINARY_FILE, (String) null);
		if (binLoc != null) {
			String cmdFolder = new Path(binLoc).removeLastSegments(1).toOSString();
			CommandLocationLookupStrategy.get().ensureOnPath(env, cmdFolder);
			workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, binLoc);
		}
	}

	private IServer launchGetServer(ILaunchConfiguration configuration) throws CoreException {
		final IServer s = ServerUtil.getServer(configuration);
		if (s == null) {
			throw new CoreException(
					CDKCoreActivator.statusFactory().errorStatus("Unable to locate server from launch configuration."));
		}
		return s;
	}
	
	private String launchGetCRCBinary(IServer s, ControllableServerBehavior beh) throws CoreException {
		String crcBin = BinaryUtility.CRC_BINARY.getLocation(s);
		if (crcBin == null || !(new File(crcBin).exists())) {
			beh.setServerStopped();
			if (crcBin == null)
				throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
						"Unable to locate crc binary. Please set a correct value in the server editor."));
			throw new CoreException(CDKCoreActivator.statusFactory()
					.errorStatus("Expected location of crc command does not exist: " + crcBin
							+ "\nPlease set a correct value in the server editor."));
		}
		return crcBin;
	}
	
	private void launchCheckSetupCRC(IServer s, CRC100Server crcServer, ControllableServerBehavior beh) throws CoreException {
		CRC100Server crc = (CRC100Server)crcServer;
		if( !crc.isInitialized()) {
			int[] retmain = new int[1];
			retmain[0] = -1;
			String home = crc.getCRCHome(s);
			Display.getDefault().syncExec(() -> {
				Shell sh = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				String msgText = "Your CRC installation has not been properly initialized. Would you like us to run crc setup for you?";
				String msg = NLS.bind(msgText, home);
				MessageDialog messageDialog = new MessageDialog(sh, "Warning: CRC has not been properly initialized!", null, msg, MessageDialog.WARNING, 
						new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				retmain[0] = messageDialog.open();
			});
			if( retmain[0] == IDialogConstants.OK_ID) {
				Job j = new SetupCRCJob(s, null, true, true);
				j.schedule();
				
				try {
					j.join();
				} catch (InterruptedException e) {
					// Ignore and finish up ASAP
				}
			}
			if( !crc.isInitialized()) { 
				beh.setServerStopped();
				throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
						"The server cannot be started until the CRC has been initialized."));
			}
		}
	}
	
	private Process launchCreateProcess(IServer s, ControllableServerBehavior beh, 
			String args, IDebugEventSetListener listener) throws CoreException {

		Process p = null;
		try {
			p = new CDKLaunchUtility().callCRCConsole(s, args, getStartupLaunchName(s));
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe);
			beh.setServerStopped();
			DebugPlugin.getDefault().removeDebugEventListener(listener);
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}

		if (p == null) {
			beh.setServerStopped();
			DebugPlugin.getDefault().removeDebugEventListener(listener);
			throw new CoreException(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to minishift up has failed."));
		}
		return p;
	}
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		final IServer s = launchGetServer(configuration);
		final ControllableServerBehavior beh = (ControllableServerBehavior) JBossServerBehaviorUtils
				.getControllableBehavior(configuration);
		beh.setServerStarting();
		String minishiftLoc = launchGetCRCBinary(s, beh);
		CRC100Server cdkServer = (CRC100Server) s.loadAdapter(CRC100Server.class, new NullProgressMonitor());
		launchCheckSetupCRC(s, cdkServer, beh);
		
		// Poll the server once more
		IStatus stat = getCRCPoller(s).getCurrentStateSynchronous(s);
		if (stat.isOK()) {
			beh.setServerStarted();
			((Server) beh.getServer()).setMode("run");
			return;
		}

		String args = configuration.getAttribute(ATTR_ARGS, (String) null);

		// Add listener first
		IDebugEventSetListener debug = getDebugListener(launch);
		DebugPlugin.getDefault().addDebugEventListener(debug);
		beh.putSharedData(AbstractStartJavaServerLaunchDelegate.DEBUG_LISTENER, debug);

		Process p = launchCreateProcess(s, beh, args, debug);
		IProcess process = addProcessToLaunch(p, launch, s, false, minishiftLoc);
		beh.putSharedData(AbstractStartJavaServerLaunchDelegate.PROCESS, process);
	}


	@Override
	protected void processTerminatedDelay() {
		try {
			// sleep to allow vagrant to unlock queries.
			Thread.sleep(3000);
		} catch (InterruptedException ie) {
			// Ignore and continue
		}

	}

	@Override
	protected AbstractCDKPoller getCDKPoller(IServer server) {
		return getCRCPoller(server);
	}

	protected AbstractCDKPoller getCRCPoller(IServer server) {
		return new CRC100Poller();
	}
}
