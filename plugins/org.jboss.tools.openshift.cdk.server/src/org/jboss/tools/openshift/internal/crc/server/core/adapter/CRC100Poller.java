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
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.OpenShiftNotReadyPollingException;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CommandTimeoutException;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.ServiceManagerEnvironmentLoader;

public class CRC100Poller extends AbstractCDKPoller {
	public CRC100Poller() {
	}

	protected void launchThread() {
		launchThread("CodeReady Containers Poller");
	}

	protected Map<String, String> createEnvironment(IServer server) {
		return new HashMap<>();
	}

	@Override
	public int getTimeoutBehavior() {
		return TIMEOUT_BEHAVIOR_FAIL;
	}

	private File getWorkingDirectory(IServer s) throws PollingException {
		File f = JBossServerCorePlugin.getServerStateLocation(s).toFile();
		if (!f.exists()) {
			f.mkdirs();
		}
		return f;
	}

	protected IStatus onePing(IServer server, Map<String, String> env)
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
				String[] asArr = (String[]) inLines.toArray(new String[inLines.size()]);
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
		return CDKCoreActivator.statusFactory().infoStatus(CDKCoreActivator.PLUGIN_ID,
				"The CRC Container is starting.");
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
