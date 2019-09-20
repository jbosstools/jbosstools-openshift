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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

public class ConfigureCRCFrameworksListener extends UnitedServerListener {

	private static final String CRC_HOST = "https://api.crc.testing";
	private static final int CRC_HOST_PORT = 6443;
	private static final String CRC_DEV_USERNAME = "developer";
	private static final String CRC_DEV_PASSWORD = "developer";
	private boolean enabled = true;

	public void enable() {
		enabled = true;
	}

	public void disable() {
		enabled = false;
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
		configureOpenshift(server);
	}

	@Override
	public boolean canHandleServer(IServer server) {
		String[] ok = new String[] { CRC100Server.CRC_100_SERVER_TYPE_ID };
		List<String> valid = Arrays.asList(ok);
		return valid.contains(server.getServerType().getId());
	}

	private void configureOpenshift(IServer server) {
		String host = CRC_HOST;
		String user = CRC_DEV_USERNAME;
		String pass = CRC_DEV_PASSWORD;
		File oc = findOcBin(server);
		String ocLoc = oc == null ? null : oc.getAbsolutePath();

		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		IConnection con = util.findExistingOpenshiftConnection(server, host, CRC_HOST_PORT);
		if (con == null) {
			con = util.createOpenshiftConnection(server, host, CRC_HOST_PORT,
					"Basic", user, pass, ocLoc, 
					ConnectionsRegistrySingleton.getInstance());
		} else {
			con.setUsername(user);
			con.setPassword(pass);
			ConnectionsRegistrySingleton.getInstance().update(con, con);
		}
		if (con != null) {
			con.connect();
		}
	}
	
	private File findOcBin(IServer server) {
		String home = getServerHome(server);
		String ocBinaryName = OCBinary.getInstance().getName();
		return Paths.get(home, "bin", ocBinaryName).toFile();
	}

	private String getAdminPassword(IServer server) {
		File passwordFile = findAdminPasswordFile(server);
		String passwordContent = null;
		if( passwordFile != null ) {
			try {
				passwordContent = new String(Files.readAllBytes(passwordFile.toPath())).trim();
			} catch(IOException ioe) {
				CDKCoreActivator.pluginLog().logError(NLS.bind(
						"Could not load password file {0}", passwordFile.getAbsolutePath()),
						ioe);
			}
		}
		return passwordContent;
	}
	private File findAdminPasswordFile(IServer server) {
		String home = getServerHome(server);
		File cache = Paths.get(home, "cache").toFile();
		File passwordFile = findFile(cache, "kubeadmin-password");
		return passwordFile;
	}

	private String getServerHome(IServer server) {
		CRC100Server crc = (CRC100Server)server.loadAdapter(CRC100Server.class, new NullProgressMonitor());
		return crc.getCRCHome(server);
	}

	private File findFile(File root, String name) {
		if( root == null || !root.exists())
			return null;
		if( root.isFile() ) 
			return root.getName().equals(name) ? root : null;
		File[] children = root.listFiles();
		for( int i = 0; i < children.length; i++ ) {
			File ret = findFile(children[i], name);
			if( ret != null )
				return ret;
		}
		return null;
	}
}
