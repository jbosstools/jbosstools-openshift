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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.util.ArgsUtil;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.MinishiftPoller;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;

public class CDK3LaunchController extends AbstractCDKLaunchController
		implements ILaunchServerController, IExternalLaunchConstants {

	@Override
	public void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		final IServer s = ServerUtil.getServer(wc);
		final CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		// for testing purposes.
		// we can't mock final methods like getServer(), so we need to be creative
		initialize(wc, cdkServer.getUsername(), cdkServer.getServer());
	}

	// NOT API! Made public for testing purposes
	public void initialize(ILaunchConfigurationWorkingCopy wc, String userName, IServer server) throws CoreException {
		wc.setAttribute(FLAG_INITIALIZED, true);
		String workingDir = JBossServerCorePlugin.getServerStateLocation(server).toOSString();
		wc.setAttribute(ATTR_WORKING_DIR, workingDir);
		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());

		boolean passCredentials = cdkServer.passCredentials();
		if (passCredentials) {
			// These environment variables are visible AND persisted in the launch
			// configuration. It is not safe to persist the password here, but rather add it on-the-fly to
			// the program launch later on.
			HashMap<String, String> env = new HashMap<>();
			String userKey = cdkServer.getUserEnvironmentKey();
			env.put(userKey, userName);
			wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
		}
		String cmdLoc = server.getAttribute(CDK3Server.MINISHIFT_FILE, (String) null);
		wc.setAttribute(ATTR_LOCATION, cmdLoc);

		String defaultArgs = "start --vm-driver="
				+ cdkServer.getServer().getAttribute(CDK3Server.PROP_HYPERVISOR, CDK3Server.getHypervisors()[0]);

		String currentVal = wc.getAttribute(ATTR_ARGS, defaultArgs);
		wc.setAttribute(ATTR_ARGS, currentVal);
	}

	@Override
	protected void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
		// Overrides, things that should always match whats in server editor
		final IServer s = ServerUtil.getServer(workingCopy);
		final CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		String workingDir = JBossServerCorePlugin.getServerStateLocation(s).toOSString();
		workingCopy.setAttribute(ATTR_WORKING_DIR, workingDir);

		Map<String, String> env = workingCopy.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String, String>) null);
		env = (env == null ? new HashMap<>() : new HashMap<>(env));
		String msHome = s.getAttribute(CDK3Server.MINISHIFT_HOME, (String) null);
		if (msHome != null) {
			env.put("MINISHIFT_HOME", msHome);
		}

		String userKey = cdkServer.getUserEnvironmentKey();
		boolean passCredentials = cdkServer.passCredentials();
		if (passCredentials) {
			// These environment variables are visible AND persisted in the launch
			// configuration.
			// It is not safe to persist the password here, but rather add it on-the-fly to
			// the
			// program launch later on.
			env.put(userKey, cdkServer.getUsername());
		} else {
			env.remove(userKey);
		}

		setMinishiftLocationOnLaunchConfig(cdkServer, workingCopy, env);
		workingCopy.setAttribute(ENVIRONMENT_VARS_KEY, env);

		// override vm-driver args
		String targetedHypervisor = cdkServer.getServer().getAttribute(CDK3Server.PROP_HYPERVISOR,
				CDK3Server.getHypervisors()[0]);
		String defaultArgs = "start --vm-driver=" + targetedHypervisor;
		String currentVal = workingCopy.getAttribute(ATTR_ARGS, defaultArgs);
		String replaced = ArgsUtil.setArg(currentVal, null, "--vm-driver", targetedHypervisor);
		workingCopy.setAttribute(ATTR_ARGS, replaced);
	}

	private void setMinishiftLocationOnLaunchConfig(CDKServer cdkServer, ILaunchConfigurationWorkingCopy workingCopy,
			Map<String, String> env) throws CoreException {

		String minishiftLoc = cdkServer.getServer().getAttribute(CDK3Server.MINISHIFT_FILE, (String) null);
		if (minishiftLoc == null)
			minishiftLoc = MinishiftBinaryUtility.getMinishiftLocation(workingCopy);

		if (minishiftLoc != null) {
			String minishiftCmdFolder = new Path(minishiftLoc).removeLastSegments(1).toOSString();
			CommandLocationLookupStrategy.get().ensureOnPath(env, minishiftCmdFolder);
			workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, minishiftLoc);
		}
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		final IServer s = ServerUtil.getServer(configuration);
		if (s == null) {
			throw new CoreException(
					CDKCoreActivator.statusFactory().errorStatus("Unable to locate server from launch configuration."));
		}

		final ControllableServerBehavior beh = (ControllableServerBehavior) JBossServerBehaviorUtils
				.getControllableBehavior(configuration);
		beh.setServerStarting();

		String minishiftLoc = MinishiftBinaryUtility.getMinishiftLocation(s);
		if (minishiftLoc == null || !(new File(minishiftLoc).exists())) {
			beh.setServerStopped();
			if (minishiftLoc == null)
				throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
						"Unable to locate minishift command. Please check to ensure that the command is available on your Path environment variable."));
			throw new CoreException(CDKCoreActivator.statusFactory()
					.errorStatus("Expected location of minishift command does not exist: " + minishiftLoc));
		}

		CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		boolean passCredentials = cdkServer.passCredentials();
		if (passCredentials) {
			handleCredentialsDuringLaunch(s, cdkServer, beh);
		}

		// Poll the server once more
		IStatus stat = getCDKPoller().getCurrentStateSynchronous(getServer());
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

		Process p = null;
		try {
			p = new CDKLaunchUtility().callMinishiftInteractive(s, args, getStartupLaunchName(s));
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe);
			beh.setServerStopped();
			DebugPlugin.getDefault().removeDebugEventListener(debug);
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}

		if (p == null) {
			beh.setServerStopped();
			DebugPlugin.getDefault().removeDebugEventListener(debug);
			throw new CoreException(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to minishift up has failed."));
		}

		IProcess process = addProcessToLaunch(p, launch, s, false, minishiftLoc);
		beh.putSharedData(AbstractStartJavaServerLaunchDelegate.PROCESS, process);

	}

	private void handleCredentialsDuringLaunch(IServer s, CDKServer cdkServer, ControllableServerBehavior beh)
			throws CoreException {
		String userKey = cdkServer.getUserEnvironmentKey();
		String passKey = cdkServer.getPasswordEnvironmentKey();
		if (userKey == null || userKey.trim().isEmpty()) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
					"Username environment variable id cannot be empty when passing credentials via environment variables."));
		}
		if (passKey == null || passKey.trim().isEmpty()) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
					"Password environment variable id cannot be empty when passing credentials via environment variables."));
		}

		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD, null);
		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_USER, null);

		String pass = null;
		String user = cdkServer.getUsername();
		try {
			pass = cdkServer.getPassword();
		} catch (UsernameChangedException uce) {
			pass = uce.getPassword();
			user = uce.getUser();
		}

		if (user == null) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("The server " + s.getName()
					+ " has no username associated with it. Please open the server editor and configure the credentials."));
		}

		if (pass == null) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("The server " + s.getName()
					+ " has no password associated with it. Please open the server editor and configure the credentials."));
		}

		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD, pass);
		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_USER, user);
	}

	@Override
	protected AbstractCDKPoller getCDKPoller() {
		MinishiftPoller vp = new MinishiftPoller();
		return vp;
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
}
