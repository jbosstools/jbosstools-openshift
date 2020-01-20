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
package org.jboss.tools.openshift.internal.crc.server.core.listeners;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.authorization.IAuthorizationContext;

public class ConfigureCRCFrameworksListener extends UnitedServerListener {

	private static final int CONNECT_TRIES = 3;
	private static final long RECONNECT_DELAY = 3 * 1000l;
	private static final String CRC_HOST_URL = "https://api.crc.testing";
	private static final int CRC_HOST_PORT = 6443;
	private static final String CRC_DEV_USERNAME = "developer";
	private static final String CRC_DEV_PASSWORD = "developer";

	private boolean enabled = true;

	public void enable() {
		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}

	@Override
	public void serverChanged(final ServerEvent event) {
		if (enabled 
				&& canHandleServer(event.getServer())
				&& serverSwitchesToState(event, IServer.STATE_STARTED)) {
			scheduleConfigureFrameworksJob(event);
		}
	}

	private void scheduleConfigureFrameworksJob(final ServerEvent event) {
		new Job("Configuring CRC Openshift Connection...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (canHandleServer(event.getServer()))
					configureFrameworks(event.getServer());
				return Status.OK_STATUS;
			}
		}.schedule(1000);
	}

	protected void configureFrameworks(IServer server) {
		configureOpenShiftConnection(server);
	}

	@Override
	public boolean canHandleServer(IServer server) {
		String[] ok = new String[] { CRC100Server.CRC_100_SERVER_TYPE_ID };
		List<String> valid = Arrays.asList(ok);
		return valid.contains(server.getServerType().getId());
	}

	private void configureOpenShiftConnection(IServer server) {
		String ocLocation = getOcLocation(server);

		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		IConnection connection = util.findExistingOpenshiftConnection(CRC_HOST_URL, CRC_HOST_PORT);
		if (connection != null) {
			ConnectionsRegistrySingleton.getInstance().remove(connection);
		}
		connection = util.createOpenshiftConnection(
				CRC_HOST_URL,
				CRC_HOST_PORT,
				IAuthorizationContext.AUTHSCHEME_BASIC, 
				CRC_DEV_USERNAME,
				CRC_DEV_PASSWORD,
				ocLocation,
				ConnectionsRegistrySingleton.getInstance());
		connection.enablePromptCredentials(false);
		connect(connection);
	}

	private void connect(IConnection connection) {
		boolean connected = false;
		int runs = 0;
		try {
			while (!connected 
					&& runs++ <= CONNECT_TRIES) {
		 		connected = safeConnect(connection);
			}
			warnUnconnected(connected);
		} catch (InterruptedException ie) {
			CDKCoreActivator.pluginLog().logError(ie);
			Thread.currentThread().interrupt();
		}
	}

	private boolean safeConnect(IConnection connection) throws InterruptedException {
		boolean connected = false;
		try {
			connected = connection.connect();
		} catch (OpenShiftException ex) {
			CDKCoreActivator.pluginLog().logError(ex);
			Thread.sleep(RECONNECT_DELAY);
		}
		return connected;
	}

	private void warnUnconnected(boolean connected) {
		if (!connected) {
			new UIJob("Warning: Wait and Refresh connection") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					MessageDialog.openWarning(getDisplay().getActiveShell(), 
							"Wait and Refresh connection",
							"Could not connect to the running CRC instance."
							+ "\nIt may not fully up and running yet. Wait and try refreshing the connection.");
					return Status.OK_STATUS;
				}
			}
			.schedule();
		}
	}

	private String getOcLocation(IServer server) {
		File oc = findOcBin(server);
		return oc == null ? null : oc.getAbsolutePath();
	}
	
	private File findOcBin(IServer server) {
		String home = getServerHome(server);
		String ocBinaryName = OCBinary.getInstance().getName();
		return Paths.get(home, "bin", ocBinaryName).toFile();
	}

	private String getServerHome(IServer server) {
		CRC100Server crc = (CRC100Server)server.loadAdapter(CRC100Server.class, new NullProgressMonitor());
		return crc.getCRCHome(server);
	}
}
