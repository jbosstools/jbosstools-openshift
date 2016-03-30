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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller.OpenShiftNotReadyPollingException;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;

public class CDKLaunchController extends AbstractSubsystemController implements ILaunchServerController, IExternalLaunchConstants {
	public static final String FLAG_INITIALIZED = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.launch.isInitialized";
	
	@Override
	public IStatus canStart(String launchMode) {
		return Status.OK_STATUS;
	}

	@Override
	public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
			throws CoreException {
		if( !isInitialized(workingCopy)) {
			initialize(workingCopy);
		}
		performOverrides(workingCopy);
	}
	
	private void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
		// Overrides, things that should always match whats in server editor
		final IServer s = ServerUtil.getServer(workingCopy);
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		String workingDir = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		workingCopy.setAttribute(ATTR_WORKING_DIR, workingDir);
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
    	
    	Map<String, String> env = workingCopy.getAttribute(ENVIRONMENT_VARS_KEY, (Map)null);
    	if( env == null ) {
    		env = new HashMap<String, String>();
    	} else {
    		env = new HashMap<String,String>(env); // no guarantee existing map is editable
    	}
    	if( passCredentials) {
    		// These environment variables are visible AND persisted in the launch configuration.
    		// It is not safe to persist the password here, but rather add it on-the-fly to the 
    		// program launch later on. 
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			env.put(userKey, cdkServer.getUsername());
    	}
		
    	String vLoc = CDKConstantUtility.getVagrantLocation();
		if( vLoc != null ) {
			String vagrantCmdFolder = new Path(vLoc).removeLastSegments(1).toOSString();
			CommandLocationLookupStrategy.get().ensureOnPath(env, vagrantCmdFolder);
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
    	boolean passCredentials = server.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
    	if( passCredentials) {
    		// These environment variables are visible AND persisted in the launch configuration.
    		// It is not safe to persist the password here, but rather add it on-the-fly to the 
    		// program launch later on. 
    		HashMap<String, String> env = new HashMap<String, String>();
			String userKey = server.getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			env.put(userKey, userName);
			wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
    	}
		wc.setAttribute(ATTR_LOCATION, CDKConstantUtility.getVagrantLocation());
		
		String args =  CDKConstants.VAGRANT_CMD_UP + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR;
		wc.setAttribute(ATTR_ARGS, args);
	}
	
	private boolean isInitialized(ILaunchConfigurationWorkingCopy wc) throws CoreException{
		return wc.hasAttribute(FLAG_INITIALIZED) && wc.getAttribute(FLAG_INITIALIZED, (Boolean)false);
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		final IServer s = ServerUtil.getServer(configuration);
		if( s == null ) {
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Unable to locate server from launch configuration."));
		}
		
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(configuration);
		beh.setServerStarting();
		
		String vagrantLoc = CDKConstantUtility.getVagrantLocation(s);
		if(vagrantLoc == null || !(new File(vagrantLoc).exists())) {
			beh.setServerStopped();
			if( vagrantLoc == null )
				throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Unable to locate vagrant command. Please check to ensure that the command is available on your Path environment variable."));
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Expected location of vagrant command does not exist: " + vagrantLoc));
		}
		
		CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
    	String pass = null;
    	String user = cdkServer.getUsername();
    	try {
    		pass = cdkServer.getPassword();
    	} catch(UsernameChangedException uce) {
    		pass = uce.getPassword();
    		user = uce.getUser();
    	}
    	
    	if( user == null ) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("The server " + s.getName() + " has no username associated with it. Please open the server editor and configure the credentials."));
    	}

    	if( pass == null ) {
			beh.setServerStopped();
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("The server " + s.getName() + " has no password associated with it. Please open the server editor and configure the credentials."));
    	}

		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD, pass);
		beh.putSharedData(CDKServerBehaviour.PROP_CACHED_USER, user);

		// Poll the server once more 
		IStatus stat = new VagrantPoller().getCurrentStateSynchronous(getServer());
		if( stat.isOK()) {
			beh.setServerStarted();
			return;
		}
		
		String args = configuration.getAttribute(ATTR_ARGS, (String)null);
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			wc = new VagrantLaunchUtility().createExternalToolsLaunchConfig(s, args, getStartupLaunchName(s));
		} catch(CoreException ce) {
			CDKCoreActivator.pluginLog().logError(ce);
			beh.setServerStopped();
			throw ce;
		}

		try {
			// Run the launch, mark server as starting, add debug listeners, etc
			ILaunch launch2 = wc.launch("run", monitor);
			final IProcess[] processes = launch2.getProcesses();
			
			if( processes != null && processes.length >= 1 && processes[0] != null ) {
				IDebugEventSetListener debug = getDebugListener(processes);
				if( beh != null ) {
					final IProcess launched = processes[0];
					beh.putSharedData(AbstractStartJavaServerLaunchDelegate.PROCESS, launched);
					beh.putSharedData(AbstractStartJavaServerLaunchDelegate.DEBUG_LISTENER, debug);
				}
				DebugPlugin.getDefault().addDebugEventListener(debug);
			}
		} catch(CoreException ce) {
			beh.setServerStopped();
			throw ce;
		}
	}

	private IDebugEventSetListener getDebugListener(final IProcess[] processes) {
		return new IDebugEventSetListener() { 
			public void handleDebugEvents(DebugEvent[] events) {
				if (events != null) {
					int size = events.length;
					for (int i = 0; i < size; i++) {
						if (processes[0] != null && processes[0].equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
							processTerminated(getServer(), processes[0], this);
						}
					}
				}
			}
		};
	}
	
	private void processTerminated(IServer server,IProcess process, IDebugEventSetListener listener) {
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(server);
		new Thread() {
			public void run() {
				handleProcessTerminated(beh);
			}
		}.start();
		DebugPlugin.getDefault().removeDebugEventListener(listener);
	}
	
	
	private void handleProcessTerminated(ControllableServerBehavior beh) {
		try {
			// sleep to allow vagrant to unlock queries. 
			Thread.sleep(1000);
		} catch( InterruptedException ie) {}
		
		// Poll the server once more 
		VagrantPoller vp = new VagrantPoller();
		IStatus stat = vp.getCurrentStateSynchronous(getServer());
		if( stat.isOK()) {
			beh.setServerStarted();
			beh.setRunMode("run");
		} else {
			// The vm is now in a confused state.  
			if( vp.getPollingException() instanceof OpenShiftNotReadyPollingException) {
				// The vm is running but openshift isn't available.  
				handleOpenShiftUnavailable(beh, (OpenShiftNotReadyPollingException)vp.getPollingException());
			} else {
				beh.setServerStopped();
			}
		}
	}
	
	private void handleOpenShiftUnavailable(final IControllableServerBehavior beh, final OpenShiftNotReadyPollingException osnrpe) {
		// Log error?  Show dialog?  
		((ControllableServerBehavior)beh).setServerStarted();
		new Job(osnrpe.getMessage()) {
			protected IStatus run(IProgressMonitor monitor) {
				return CDKCoreActivator.statusFactory().errorStatus("Error contacting OpenShift", osnrpe);
			}
			
		}.schedule();
	}
	
	private String getStartupLaunchName(IServer s) {
		return "Start " + s.getName();
	}
	
}
