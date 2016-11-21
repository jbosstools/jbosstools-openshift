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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.VagrantBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CommandTimeoutException;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKLaunchEnvironmentUtil;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironmentLoader;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.LazySSLCertificateCallback;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ISSLCertificateCallback;
import com.openshift.restclient.OpenShiftException;

public class VagrantPoller extends AbstractCDKPoller {

	protected void launchThread() {
		launchThread("CDK Vagrant Poller");
	}	

	protected Map<String, String> createEnvironment(IServer server) {
		return CDKLaunchEnvironmentUtil.createEnvironment(server);
	}
	
	@Override
	public int getTimeoutBehavior() {
		return TIMEOUT_BEHAVIOR_FAIL;
	}

	
	private File getWorkingDirectory(IServer s) throws PollingException {
		String str = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( str != null && new File(str).exists()) {
			return new File(str);
		}
		throw  new PollingException("Working Directory not found: " + str);
	}
		
	
	protected IStatus onePing(IServer server, Map<String, String> env)
			throws PollingException, IOException, TimeoutException {

		String[] args = new String[] { CDKConstants.VAGRANT_CMD_STATUS, CDKConstants.VAGRANT_FLAG_MACHINE_READABLE,
				CDKConstants.VAGRANT_FLAG_NO_COLOR };
		String vagrantcmdloc = VagrantBinaryUtility.getVagrantLocation(server);
		try {
			String[] lines = CDKLaunchUtility.callMachineReadable(
					vagrantcmdloc, args, getWorkingDirectory(server), env);
			IStatus vmStatus = parseOutput(lines);
			if (vmStatus.isOK()) {
				// throws OpenShiftNotReadyPollingException on failure
				checkOpenShiftHealth(server, 4000); 
			}
			return vmStatus;
		} catch (CommandTimeoutException vte) {
			// Try to salvage it, it could be the process never terminated but
			// it got all the output
			List<String> inLines = vte.getInLines();
			if (inLines != null) {
				String[] asArr = inLines.toArray(new String[inLines.size()]);
				IStatus ret = parseOutput(asArr);
				if (ret != null) {
					return ret;
				}
			}
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant status. ", vte);
			throw vte;
		}
	}

	private boolean checkOpenShiftHealth(IServer server, int timeout) throws OpenShiftNotReadyPollingException {
		ServiceManagerEnvironment adb = ServiceManagerEnvironmentLoader.type(server).getOrLoadServiceManagerEnvironment(server, true);
		if( adb == null ) {
			return false;
		}
		String url = adb.getOpenShiftHost() + ":" + adb.getOpenShiftPort();
		return checkOpenShiftHealth(url, timeout);
	}
	protected boolean checkOpenShiftHealth(String url,  int timeout) throws OpenShiftNotReadyPollingException {
		ISSLCertificateCallback sslCallback = new LazySSLCertificateCallback(); 
		IClient client = new ClientBuilder(url)
				.sslCertificateCallback(sslCallback)
				.withConnectTimeout(timeout, TimeUnit.MILLISECONDS)
				.build();
   	
    	Exception e = null;
    	try {
    		if( "ok".equals(client.getServerReadyStatus()))
    			return true;
    	} catch(OpenShiftException ex) {
    		e = ex;
    	}

    	String msg = NLS.bind("The CDK VM is up and running, but OpenShift is unreachable at url {0}. " + 
    	"The VM may not have been registered successfully. Please check your console output for more information", url);
		throw new OpenShiftNotReadyPollingException(CDKCoreActivator.statusFactory().errorStatus(CDKCoreActivator.PLUGIN_ID,
				msg, e, OpenShiftNotReadyPollingException.OPENSHIFT_UNREACHABLE_CODE));
	}
	
	private class VagrantStatus implements CDKConstants {
		
		private HashMap<String, String> kv;
		private String id;
		public VagrantStatus(String vmId) {
			this.id = vmId;
			this.kv = new HashMap<>();
		}
		public void setProperty(String k, String v) {
			kv.put(k, v);
		}
		public String getState() {
			return kv.get(STATE);
		}
	}
	
	
	protected IStatus parseOutput(String[] lines) {
		Map<String, VagrantStatus> status = new HashMap<>();
		if (lines != null && lines.length > 0) {
			for (int i = 0; i < lines.length; i++) {
				String[] csv = lines[i].split(",");
				if (csv.length >= 2) { // avoid arrayindex errors
					String vmId = csv[1];
					if (!StringUtils.isEmpty(vmId)) {
						VagrantStatus vs = getVagrantStatus(status, vmId);
						String k = csv[2];
						String v = csv[3];
						if (k != null) {
							vs.setProperty(k, v);
						}
					} else {
						if (csv.length >= 3) {
							if ("error-exit".equals(csv[2])) {
								return createErrorStatus(csv);
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
		if( stats.isEmpty() ) {
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


	private VagrantStatus getVagrantStatus(Map<String, VagrantStatus> status, String vmId) {
		VagrantStatus vs = status.get(vmId);
		if (vs == null) {
			vs = new VagrantStatus(vmId);
			status.put(vmId, vs);
		}
		return vs;
	}


	private IStatus createErrorStatus(String[] csv) {
		IStatus s = null;
		if (csv.length >= 5) {
			s = CDKCoreActivator.statusFactory().errorStatus(csv[4]);
		} else {
			s = CDKCoreActivator.statusFactory().errorStatus("An error occurred while checking CDK state.");
		}

		CDKCoreActivator.pluginLog().logError("Unable to access CDK status via vagrant status.", new CoreException(s));
		return s;
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
		return new String[]{VagrantStatus.STATE_SHUTOFF, VagrantStatus.STATE_POWEROFF, VagrantStatus.STATE_NOT_CREATED};
	}

	private boolean allStopped(Collection<VagrantStatus> stats) {
		List<String> off = Arrays.asList(getValidStoppedStates());
		Iterator<VagrantStatus> i = stats.iterator();
		VagrantStatus tmp;
		while(i.hasNext()) {
			tmp = i.next();
			if( !off.contains(tmp.getState())) {
				return false;
			}
		}
		return true;
	}

}
