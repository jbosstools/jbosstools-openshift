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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.ide.eclipse.as.core.server.IServerStatePollerType;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.VagrantLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKLaunchEnvironmentUtil;

public class VagrantPoller implements IServerStatePoller2 {
	private IServer server;
	private boolean canceled, done;
	private boolean state;
	private boolean expectedState;
	private PollingException aborted = null;

	@Override
	public IServer getServer() {
		return server;
	}


	@Override
	public void beginPolling(IServer server, boolean expectedState) throws PollingException {
		this.server = server;
		this.canceled = done = false;
		this.expectedState = expectedState;
		this.state = !expectedState;
		launchThread();
	}
	protected void launchThread() {
		Thread t = new Thread(new Runnable(){
			public void run() {
				pollerRun();
			}
		}, "Vagrant Poller"); //$NON-NLS-1$
		t.start();
	}
	

	private synchronized void setStateInternal(boolean done, boolean state) {
		this.done = done;
		this.state = state;
	}
	
	private void pollerRun() {
		setStateInternal(false, state);
    	CDKServer cdkServer = (CDKServer)server.loadAdapter(CDKServer.class, new NullProgressMonitor());
    	String pass = cdkServer.getPassword();
    	Map<String,String> env = CDKLaunchEnvironmentUtil.createEnvironment(server, pass);
		while(aborted == null && !canceled && !done) {
			IStatus stat = onePing(server, env);
			int status = stat.getSeverity();
			boolean completeUp = ( status == IStatus.OK && expectedState);
			boolean completeDown = (status == IStatus.ERROR && !expectedState);
			if( completeUp || completeDown) {
				setStateInternal(true, expectedState);
			}
			try {
				Thread.sleep(700);
			} catch(InterruptedException ie) {} // ignore
		}
	}

	public synchronized boolean isComplete() throws PollingException, RequiresInfoException {
		return done;
	}

	public synchronized boolean getState() throws PollingException, RequiresInfoException {
		return state;
	}

	public void cleanup() {
	}

	public synchronized void cancel(int type) {
		canceled = true;
	}

	public int getTimeoutBehavior() {
		return TIMEOUT_BEHAVIOR_FAIL;
	}

	@Override
	public List<String> getRequiredProperties() {
		// TODO Auto-generated method stub
		return null;
	}


	public IStatus getCurrentStateSynchronous(IServer server) {
		int b = onePing(server);
		Status s;
		if( b == IStatus.OK ) {
			s = new Status(IStatus.OK, CDKCoreActivator.PLUGIN_ID, "Vagrant Instance is Up");
		} else if( b == IStatus.ERROR){
			s = new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Vagrant Instance is shutoff");
		} else {
			s = new Status(IStatus.INFO, CDKCoreActivator.PLUGIN_ID, "Vagrant Instance is indeterminate");
		}
		return s;
	}
	
	private File getWorkingDirectory(IServer s) throws PollingException {
		String str = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( str != null && new File(str).exists()) {
			return new File(str);
		}
		throw  new PollingException("Working Directory not found: " + str);
	}
	
	
	// This *could* prompt for a password, so dont use this method for repeated calls
	private int onePing(IServer server) {
    	CDKServer cdkServer = (CDKServer)server.loadAdapter(CDKServer.class, new NullProgressMonitor());
    	IStatus stat = onePing(server, CDKLaunchEnvironmentUtil.createEnvironment(server, cdkServer.getPassword()));
    	return stat.getSeverity();
	}
		
	private IStatus onePing(IServer server, Map<String,String> env) {

		String[] args = new String[]{CDKConstants.VAGRANT_CMD_STATUS, 
				CDKConstants.VAGRANT_FLAG_MACHINE_READABLE, CDKConstants.VAGRANT_FLAG_NO_COLOR};
    	String vagrantcmdloc = CDKConstantUtility.getVagrantLocation(server);		

	    try {
	    	String[] lines = VagrantLaunchUtility.call(vagrantcmdloc, args,  getWorkingDirectory(server), env);
  	        return parseOutput(lines);
    	} catch(PollingException pe) {
    		aborted = pe;
    	} catch(TimeoutException te) {
    		aborted = new PollingException(te.getMessage(), te);
    	} catch(IOException ioe) {
    		// TODO
    		ioe.printStackTrace();
    	}
		return CDKCoreActivator.statusFactory().infoStatus(CDKCoreActivator.PLUGIN_ID, "Vagrant status indicates the CDK is starting.");
	}
	
	private class VagrantStatus implements CDKConstants {
		
		private HashMap<String, String> kv;
		private String id;
		public VagrantStatus(String vmId) {
			this.id = vmId;
			this.kv = new HashMap<String, String>();
		}
		public void setProperty(String k, String v) {
			kv.put(k, v);
		}
		public String getState() {
			return kv.get(STATE);
		}
	}
	
	
	protected IStatus parseOutput(String[] lines) {
		HashMap<String, VagrantStatus> status = new HashMap<String, VagrantStatus>();
		if( lines != null && lines.length > 0 ) {
			for( int i = 0; i < lines.length; i++ ) {
				String[] csv = lines[i].split(",");
				if( csv.length >=2 ) { // avoid arrayindex errors
					String timestamp = csv[0];
					String vmId = csv[1];
					if( vmId != null && !vmId.isEmpty() ) {
						VagrantStatus vs = status.get(vmId);
						if( vs == null ) {
							vs = new VagrantStatus(vmId);
							status.put(vmId, vs);
						}
						String k = csv[2];
						String v = csv[3];
						if( k != null ) {
							vs.setProperty(k,v);
						}
					} else {
					   if( csv.length >= 3) {
						   if( csv[2].equals("error-exit")) {
							   CoreException ce = null;
							   IStatus s = null;
							   if( csv.length >= 5) {
								   s = CDKCoreActivator.statusFactory().errorStatus(csv[4]);
							   } else {
								   s = CDKCoreActivator.statusFactory().errorStatus("An error occurred while checking CDK state.");
							   }
							   
							   ce = new CoreException(s);
							   CDKCoreActivator.pluginLog().logError("Unable to access CDK status via vagrant status.", ce);
							   return s;
						   }
					   }
					  }
				} //else {
				  // The given line isn't csv or doesn't have at least 2 items in the csv array
				  // and so should be ignored
				  //return IStatus.INFO;
				  //}
			}
		}		
		Collection<VagrantStatus> stats = status.values();
		if( stats.size() == 0 ) {
			return CDKCoreActivator.statusFactory().errorStatus("Unable to retrieve vagrant status for the given CDK");
		}
		if( allRunning(stats)) {
			return Status.OK_STATUS;
		}
		if( allStopped(stats)) {
			return CDKCoreActivator.statusFactory().errorStatus("Vagrant status indicates the CDK is stopped: " + String.join("\n", Arrays.asList(lines)));
		}
		return CDKCoreActivator.statusFactory().infoStatus(CDKCoreActivator.PLUGIN_ID, "Vagrant status indicates the CDK is starting.");
	}
	
	private boolean allRunning(Collection<VagrantStatus> stats) {
		List<String> on = Arrays.asList(getValidRunningStates());
		Iterator<VagrantStatus> i = stats.iterator();
		while(i.hasNext()) {
			if( !on.contains(i.next().getState())) {
				return false;
			}
		}
		return true;
	}

	private String[] getValidRunningStates() {
		return new String[]{VagrantStatus.STATE_RUNNING};
	}

	private String[] getValidStoppedStates() {
		return new String[]{VagrantStatus.STATE_SHUTOFF, VagrantStatus.STATE_POWEROFF};
	}

	private boolean allStopped(Collection<VagrantStatus> stats) {
		List<String> off = Arrays.asList(getValidStoppedStates());
		Iterator<VagrantStatus> i = stats.iterator();
		while(i.hasNext()) {
			if( !off.contains(i.next().getState())) {
				return false;
			}
		}
		return true;
	}


	@Override
	public void provideCredentials(Properties credentials) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public IServerStatePollerType getPollerType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPollerType(IServerStatePollerType type) {
		// TODO Auto-generated method stub
		
	}

}
