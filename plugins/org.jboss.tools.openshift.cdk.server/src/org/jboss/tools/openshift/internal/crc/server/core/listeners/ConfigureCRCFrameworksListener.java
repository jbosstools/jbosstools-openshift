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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

public class ConfigureCRCFrameworksListener extends UnitedServerListener {
	private boolean enabled = true;

	public void enable() {
		enabled = true;
	}

	public void disable() {
		enabled = false;
	}

	@Override
	public void serverChanged(final ServerEvent event) {
		if (enabled && canHandleServer(event.getServer())) {
			if (serverSwitchesToState(event, IServer.STATE_STARTED)) {
				scheduleConfigureFrameworksJob(event);
			}
		}
	}

	private void scheduleConfigureFrameworksJob(final ServerEvent event) {
		new Job("Configuring CRC Openshift Connection...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (canHandleServer(event.getServer()))
						configureFrameworks(event.getServer());
					return Status.OK_STATUS;
				} catch (CoreException ce) {
					return ce.getStatus();
				}
			}
		}.schedule(1000);
	}

	protected void configureFrameworks(IServer server) throws CoreException {
		configureOpenshift(server);
	}

	@Override
	public boolean canHandleServer(IServer server) {
		String[] ok = new String[] { CRC100Server.CRC_100_SERVER_TYPE_ID };
		List<String> valid = Arrays.asList(ok);
		return valid.contains(server.getServerType().getId());
	}

	private void configureOpenshift(IServer server) {
		String host = "https://api.crc.testing";
		int port = 6443;
		String user = "kubeadmin";
		File passwordFile = findPasswordFile(server);
		File oc = findOcBin(server);
		String ocLoc = oc == null ? null : oc.getAbsolutePath();
		String passwordContent = null;
		if( passwordFile != null ) {
			try {
				passwordContent = new String(Files.readAllBytes(passwordFile.toPath())).trim();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}

		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		IConnection con = util.findExistingOpenshiftConnection(server, host, port);
		if (con == null) {
			con = util.createOpenshiftConnection(server, host, port, 
					"Basic", user, passwordContent, ocLoc, 
					ConnectionsRegistrySingleton.getInstance());
		} else {
			//util.updateOpenshiftConnection(adb, con);
		}
		if (con != null) {
			con.connect();
		}
	}
	
	private File findOcBin(IServer server) {
		CRC100Server crc = (CRC100Server)server.loadAdapter(CRC100Server.class, new NullProgressMonitor());
		String home = crc.getCRCHome(server);
		File fHome = new File(home);
		File bin = new File(fHome, "bin");
		String binName = Platform.getOS().equals(Platform.OS_WIN32) ? "oc.exe" : "oc";
		return new File(bin, binName);
	}

	private File findPasswordFile(IServer server) {
		CRC100Server crc = (CRC100Server)server.loadAdapter(CRC100Server.class, new NullProgressMonitor());
		String home = crc.getCRCHome(server);
		File fHome = new File(home);
		File cache = new File(fHome, "cache");
		File passwordFile = findFile(cache, "kubeadmin-password");
		return passwordFile;
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
