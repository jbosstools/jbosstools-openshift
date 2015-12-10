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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.control.impl.ITerminalControlForText;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.core.util.PollThreadUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.jboss.tools.openshift.cdk.server.ui.internal.util.TerminalUtility;

public class CDKLaunchController extends AbstractSubsystemController implements ILaunchServerController, IExternalLaunchConstants {
	private static final String FLAG_INITIALIZED = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.launch.isInitialized";
	
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
    	if( Platform.getOS().equals(Platform.OS_WIN32)) {
    		// We need to set the cygwin flag
    		env.put("VAGRANT_DETECTED_OS", "cygwin");
    	}
		workingCopy.setAttribute(ENVIRONMENT_VARS_KEY, env);
	}
	
	private void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		final IServer s = ServerUtil.getServer(wc);
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		wc.setAttribute(FLAG_INITIALIZED, true);
		String workingDir = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		wc.setAttribute(ATTR_WORKING_DIR, workingDir);
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
    	if( passCredentials) {
    		// These environment variables are visible AND persisted in the launch configuration.
    		// It is not safe to persist the password here, but rather add it on-the-fly to the 
    		// program launch later on. 
    		HashMap<String, String> env = new HashMap<String, String>();
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			env.put(userKey, cdkServer.getUsername());
			wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
    	}
		wc.setAttribute(ATTR_LOCATION, CDKConstantUtility.getVagrantLocation());
		
		String args =  CDKConstants.VAGRANT_CMD_UP + " " + CDKConstants.VAGRANT_FLAG_PROVISION + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR;
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
			throw new CoreException(CDKCoreActivator.statusFactory().errorStatus("Unable to locate vagrant command: " + vagrantLoc));
		}
		
		
		// Poll the server once more 
		IStatus stat = new VagrantPoller().getCurrentStateSynchronous(getServer());
		if( stat.isOK()) {
			beh.setServerStarted();
			return;
		}
		
		String args = configuration.getAttribute(ATTR_ARGS, (String)null);
		ILaunchConfigurationWorkingCopy wc = new VagrantLaunchUtility().createExternalToolsLaunchConfig(s, args, getStartupLaunchName(s));

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
				try {
					// sleep to allow vagrant to unlock queries. 
					Thread.sleep(1000);
				} catch( InterruptedException ie) {}
				
				// Poll the server once more 
				IStatus stat = new VagrantPoller().getCurrentStateSynchronous(getServer());
				if( stat.isOK()) {
					beh.setServerStarted();
					beh.setRunMode("run");
				} else {
					beh.setServerStopped();
				}	
			}
		}.start();
		DebugPlugin.getDefault().removeDebugEventListener(listener);
	}
	
	private String getStartupLaunchName(IServer s) {
		return "Start " + s.getName();
	}
	
	public void launchViaTerminal(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		final IServer s = ServerUtil.getServer(configuration);
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		final CDKServerBehaviour beh = (CDKServerBehaviour)s.loadAdapter(CDKServerBehaviour.class, new NullProgressMonitor());
		beh.setServerStarting();
		
		final Map<String, Object> props = TerminalUtility.getPropertiesForServer(s);		
		final CustomDone customDone = new CustomDone();
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				openView(props, customDone);
			}
		});
		
		
		// Wait for done
		while(customDone.getStatus() == null ) {
			try {
				Thread.sleep(200);
			} catch(InterruptedException ie) {
				// TODO
			}
		}
		
		String command2 = "vagrant up";
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
		if( passCredentials ) {
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
			String user = cdkServer.getUsername();
			String pass = cdkServer.getPassword();
			command2 = userKey + "=" + user + " " + passKey + "=" + pass + " " + command2;
		}
		
		final String command = "\n" + command2 + "\n";
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				ITerminalControlForText control = TerminalUtility.findTerminalControl(props);
				if( control != null ) {
					OutputStream os = control.getOutputStream();
					try {
						os.write(command.getBytes());
					} catch(IOException ioe) {
						ioe.printStackTrace();
					}
					
					launchPoller(beh);
				}
			}
		});
		
	}
	
	
	private void launchPoller(IControllableServerBehavior beh) {
		// delay the launch of polling until the cmd vagrant up has been actually run. 
		try {
			Thread.sleep(1500);
		} catch(InterruptedException ie) {
			// ignore
		}
		PollThreadUtils.pollServer(beh.getServer(), IServerStatePoller2.SERVER_UP, new VagrantPoller());
	}
	
	
	private void openView(Map<String, Object> props, ITerminalService.Done d) {
		TerminalUtility.openConsole(props, d);
	}
	

	
	private class CustomDone implements ITerminalService.Done {
		private IStatus stat = null;
		public void done(IStatus status) {
			this.stat = status;
		}
		public IStatus getStatus() {
			return stat;
		}
	}
	
}
