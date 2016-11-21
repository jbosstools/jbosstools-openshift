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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.VagrantBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.OpenShiftNotReadyPollingException;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;

public class CDKLaunchController extends AbstractCDKLaunchController implements ILaunchServerController, IExternalLaunchConstants {

	protected void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
		// Overrides, things that should always match whats in server editor
		final IServer s = ServerUtil.getServer(workingCopy);
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		String workingDir = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		workingCopy.setAttribute(ATTR_WORKING_DIR, workingDir);
    	
    	Map<String, String> env = workingCopy.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String, String>)null);
    	if( env == null ) {
    		env = new HashMap<>();
    	} else {
    		env = new HashMap<>(env); // no guarantee existing map is editable
    	}
    	
    	String userKey = cdkServer.getUserEnvironmentKey();
    	boolean passCredentials = cdkServer.passCredentials();
    	if( passCredentials) {
    		// These environment variables are visible AND persisted in the launch configuration.
    		// It is not safe to persist the password here, but rather add it on-the-fly to the 
    		// program launch later on. 
			env.put(userKey, cdkServer.getUsername());
    	} else {
    		env.remove(userKey);
    	}
		
    	String vLoc = VagrantBinaryUtility.getVagrantLocation(workingCopy);
		if( vLoc != null ) {
			String vagrantCmdFolder = new Path(vLoc).removeLastSegments(1).toOSString();
			CommandLocationLookupStrategy.get().ensureOnPath(env, vagrantCmdFolder);
			workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, vLoc);
		}

    	if( Platform.getOS().equals(Platform.OS_WIN32)) {
    		// We need to set the cygwin flag
    		env.put("VAGRANT_DETECTED_OS", "cygwin");
    	}
		workingCopy.setAttribute(ENVIRONMENT_VARS_KEY, env);
	}
	
	public void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		final IServer s = ServerUtil.getServer(wc);
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		//for testing purposes.
		//we can't mock final methods like getServer(), so we need to be creative 
		initialize(wc, cdkServer.getUsername(), cdkServer.getServer());
	}
	
	//NOT API! Made public for testing purposes
	public void initialize(ILaunchConfigurationWorkingCopy wc, String userName, IServer server) throws CoreException {
		wc.setAttribute(FLAG_INITIALIZED, true);
		String workingDir = server.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		wc.setAttribute(ATTR_WORKING_DIR, workingDir);
		CDKServer cdkServer = (CDKServer)server.loadAdapter(CDKServer.class, new NullProgressMonitor());

    	boolean passCredentials = cdkServer.passCredentials();
    	if( passCredentials) {
    		// These environment variables are visible AND persisted in the launch configuration.
    		// It is not safe to persist the password here, but rather add it on-the-fly to the 
    		// program launch later on. 
    		HashMap<String, String> env = new HashMap<>();
			String userKey = cdkServer.getUserEnvironmentKey();
			env.put(userKey, userName);
			wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
    	}
		wc.setAttribute(ATTR_LOCATION, VagrantBinaryUtility.getVagrantLocation());
		
		String args =  CDKConstants.VAGRANT_CMD_UP + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR;
		wc.setAttribute(ATTR_ARGS, args);
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		final IServer s = ServerUtil.getServer(configuration);
		verifyServer(s);
		
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(configuration);
		beh.setServerStarting();
		
		String vagrantLoc = VagrantBinaryUtility.getVagrantLocation(s);
		if(vagrantLoc == null || !(new File(vagrantLoc).exists())) {
			beh.setServerStopped();
			if( vagrantLoc == null )
				throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Unable to locate vagrant command. Please check to ensure that the command is available on your Path environment variable."));
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Expected location of vagrant command does not exist: " + vagrantLoc));
		}
		
		CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		boolean passCredentials = cdkServer.passCredentials();
		
		if (passCredentials) {
			setBehaviourUserAndPassword(s, beh, cdkServer);
		}
		
		// Poll the server once more 
		IStatus stat = getCDKPoller().getCurrentStateSynchronous(getServer());
		if( stat.isOK()) {
			beh.setServerStarted();
			((Server) beh.getServer()).setMode(ILaunchManager.RUN_MODE);
			return;
		}
		
		String args = configuration.getAttribute(ATTR_ARGS, (String) null);
		Process p = getProcess(s, beh, args);
		if (p == null) {
			beh.setServerStopped();
			throw new CoreException(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to vagrant up has failed."));
		}

		IProcess process = addProcessToLaunch(p, launch, s);
		linkTerminal(p);
		
		IDebugEventSetListener debug = getDebugListener(new IProcess[] { process }, launch);
		DebugPlugin.getDefault().addDebugEventListener(debug);
		beh.putSharedData(AbstractStartJavaServerLaunchDelegate.PROCESS, process);
		beh.putSharedData(AbstractStartJavaServerLaunchDelegate.DEBUG_LISTENER, debug);
	}

	private void verifyServer(final IServer s) throws CoreException {
		if (s == null) {
			throw new CoreException(
					CDKCoreActivator.statusFactory().errorStatus("Unable to locate server from launch configuration."));
		}
	}

	private void setBehaviourUserAndPassword(final IServer s, final ControllableServerBehavior beh, CDKServer cdkServer)
			throws CoreException {
		verifyUser(beh, cdkServer.getUserEnvironmentKey());
		verifyPassword(beh, cdkServer.getPasswordEnvironmentKey());
		
		String pass = null;
		String user = cdkServer.getUsername();
		try {
			pass = cdkServer.getPassword();
		} catch(UsernameChangedException uce) {
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

	private Process getProcess(final IServer s, final ControllableServerBehavior beh, String args)
			throws CoreException {
		try {
			return new CDKLaunchUtility().callInteractive(s, args, getStartupLaunchName(s));
		} catch(IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe);
			beh.setServerStopped();
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}
	}

	private void verifyPassword(final ControllableServerBehavior beh, String passKey) throws CoreException {
		if (passKey == null || passKey.trim().isEmpty()) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
					"Password environment variable id cannot be empty when passing credentials via environment variables."));
		}
	}

	private void verifyUser(final ControllableServerBehavior beh, String userKey) throws CoreException {
		if (userKey == null || userKey.trim().isEmpty()) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
					"Username environment variable id cannot be empty when passing credentials via environment variables."));
		}
	}

	private void verifyVagrantLocation(final ControllableServerBehavior beh, String vagrantLoc) throws CoreException {
		if(vagrantLoc == null) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus(
					"Unable to locate vagrant command. "
					+ "Please check to ensure that the command is available on your Path environment variable."));
		} else if (!(new File(vagrantLoc).exists())) {
			throw new CoreException(CDKCoreActivator.statusFactory()
					.errorStatus("Expected location of vagrant command does not exist: " + vagrantLoc));
		}
	}
	
	
	protected LaunchManager getLaunchManager() {
		return (LaunchManager)DebugPlugin.getDefault().getLaunchManager();
	}
	
	protected AbstractCDKPoller getCDKPoller() {
		VagrantPoller vp = new VagrantPoller();
		return vp;
	}
	
	protected void processTerminatedDelay() {
		try {
			// sleep to allow vagrant to unlock queries. 
			Thread.sleep(3000);
		} catch( InterruptedException ie) {}
	}
	
	private void handleOpenShiftUnavailable(final IControllableServerBehavior beh, final OpenShiftNotReadyPollingException osnrpe) {
		// Log error?  Show dialog?  
		((ControllableServerBehavior)beh).setServerStarted();
		((Server)beh.getServer()).setMode(ILaunchManager.RUN_MODE);
		new Job(osnrpe.getMessage()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return CDKCoreActivator.statusFactory().errorStatus("Error contacting OpenShift", osnrpe);
			}
			
		}.schedule();
	}
	
}
