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
package org.jboss.tools.openshift.internal.crc.server.core.adapter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.OpenShiftNotReadyPollingException;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CommandTimeoutException;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader.MinishiftVersions;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.ServiceManagerEnvironmentLoader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CRC100Poller extends AbstractCDKPoller {
	private String crcVers;
	
	protected void launchThread() {
		launchThread("CodeReady Containers Poller");
	}

	protected Map<String, String> createEnvironment(IServer server) {
		try {
			crcVers = getCrcVersionString(server);
		} catch (PollingException | IOException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new HashMap<>();
	}
	
	protected boolean shouldUseJsonFlags() {
		String[] versionSegments = crcVers.split("\\.");
		int[] versionSegmentsInt = new int[versionSegments.length];
		for( int i = 0; i < versionSegments.length; i++ ) {
			versionSegmentsInt[i] = Integer.parseInt(versionSegments[i]);
		}
		return compareVersions(versionSegmentsInt, new int[] {1, 17, 0 }) >= 0;
	}

	private int compareVersions(int[] versionSegments, int[] is) {
		if( versionSegments[0] > is[0] )
			return 1;
		if( versionSegments[0] < is[0] )
			return -1;
		if( versionSegments[1] > is[1] )
			return 1;
		if( versionSegments[1] < is[1] )
			return -1;
		if( versionSegments[2] > is[2] )
			return 1;
		if( versionSegments[2] < is[2] )
			return -1;
		return 0;
	}

	@Override
	public int getTimeoutBehavior() {
		return TIMEOUT_BEHAVIOR_FAIL;
	}

	private File getWorkingDirectory(IServer s) {
		File f = JBossServerCorePlugin.getServerStateLocation(s).toFile();
		if (!f.exists()) {
			f.mkdirs();
		}
		return f;
	}
	

	protected String getCrcVersionString(IServer server)
			throws PollingException, IOException, TimeoutException {
		String cmdLoc = CRC100Server.getCRCBinaryLocation(server);
		MinishiftVersions msVers = MinishiftVersionLoader.getVersionProperties(cmdLoc);
		return msVers.getCRCVersion();
	}

	protected IStatus onePing(IServer server, Map<String, String> env)
			throws PollingException, IOException, TimeoutException {
		if( shouldUseJsonFlags()) {
			return runOnePingJson(server, env);
		} else {
			return runOnePingLegacy(server, env);
		}
	}
	protected IStatus runOnePingLegacy(IServer server, Map<String, String> env)
			throws PollingException, IOException, TimeoutException {
		String[] args = new String[] { CDKConstants.VAGRANT_CMD_STATUS };
		String cmdLoc = CRC100Server.getCRCBinaryLocation(server);
		try {
			String[] lines = new CDKLaunchUtility().callMachineReadable(cmdLoc, args, 
					getWorkingDirectory(server), env);
			IStatus stat = parseOutput(lines);
			if (stat.isOK()) {
				checkOpenShiftHealth(server, 4000);
				return stat;
			} else {
				return stat;
			}
		} catch (CommandTimeoutException vte) {
			// Try to salvage it, it could be the process never terminated but
			// it got all the output
			List<String> inLines = vte.getInLines();
			if (inLines != null) {
				String[] asArr = inLines.toArray(new String[inLines.size()]);
				IStatus stat = parseOutput(asArr);
				if (stat.isOK()) {
					checkOpenShiftHealth(server, 4000);
				} else {
					return stat;
				}
			}
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant status. ", vte);
			throw vte;
		}
	}
	protected IStatus runOnePingJson(IServer server, Map<String, String> env)
			throws PollingException, IOException, TimeoutException {
		String[] args = new String[] { "status", "-o", "json" };
		String cmdLoc = CRC100Server.getCRCBinaryLocation(server);
		try {
			String[] lines = new CDKLaunchUtility().callMachineReadable(cmdLoc, args, 
					getWorkingDirectory(server), env);
			String allContent = String.join("\n", lines);
		    ObjectMapper mapper = new ObjectMapper();
		    JsonNode actualObj = mapper.readTree(allContent);
		    JsonNode field = actualObj.get("crcStatus");
		    String status = field.asText();
		    if( "Running".equals(status)) 
		    	return Status.OK_STATUS;
		    if( "Stopped".equals(status)) 
				return CDKCoreActivator.statusFactory()
						.errorStatus("crc status indicates the CodeReady Container is stopped.");
			return StatusFactory.infoStatus(CDKCoreActivator.PLUGIN_ID, "The CRC Container is starting.");
		} catch (CommandTimeoutException vte) {
			// Try to salvage it, it could be the process never terminated but
			// it got all the output
			List<String> inLines = vte.getInLines();
			if (inLines != null) {
				String[] asArr = inLines.toArray(new String[inLines.size()]);
				IStatus stat = parseOutput(asArr);
				if (stat.isOK()) {
					checkOpenShiftHealth(server, 4000);
				} else {
					return stat;
				}
			}
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant status. ", vte);
			throw vte;
		}
	}

	protected IStatus parseOutput(String[] lines) {
		if( lines != null && lines.length > 0 ) {
			String line1 = lines[0];
			if(line1.startsWith("CRC VM:") && line1.trim().endsWith("Running")) {
				return Status.OK_STATUS;
			}
			if(line1.startsWith("CRC VM:") && line1.trim().endsWith("Stopped")) {
				return CDKCoreActivator.statusFactory()
						.errorStatus("crc status indicates the CodeReady Container is stopped.");
			}
		}
		return StatusFactory.infoStatus(CDKCoreActivator.PLUGIN_ID, "The CRC Container is starting.");
	}

	private boolean checkOpenShiftHealth(IServer server, int timeout) throws OpenShiftNotReadyPollingException {
		ServiceManagerEnvironment adb = ServiceManagerEnvironmentLoader.type(server)
				.getOrLoadServiceManagerEnvironment(server, true, true);

		if (adb == null) {
			return false;
		}
		String url = adb.getOpenShiftHost() + ":" + adb.getOpenShiftPort();
		return checkOpenShiftHealth(url, timeout);
	}

	protected boolean checkOpenShiftHealth(String url, int timeout) throws OpenShiftNotReadyPollingException {
//		ISSLCertificateCallback sslCallback = new LazySSLCertificateCallback();
//		IClient client = new ClientBuilder(url).sslCertificateCallback(sslCallback)
//				.withConnectTimeout(timeout, TimeUnit.MILLISECONDS).build();
//
//		Exception e = null;
//		try {
//			String v = client.getServerReadyStatus();
//			if ("ok".equals(v))
//				return true;
//		} catch (OpenShiftException ex) {
//			e = ex;
//		}
//
//		String msg = NLS.bind(
//				"The CDK VM is up and running, but OpenShift is unreachable at url {0}. "
//						+ "The VM may not have been registered successfully. Please check your console output for more information",
//				url);
//		throw new OpenShiftNotReadyPollingException(CDKCoreActivator.statusFactory().errorStatus(
//				CDKCoreActivator.PLUGIN_ID, msg, e, OpenShiftNotReadyPollingException.OPENSHIFT_UNREACHABLE_CODE));
		
		// TODO
		return true;
	}

}
