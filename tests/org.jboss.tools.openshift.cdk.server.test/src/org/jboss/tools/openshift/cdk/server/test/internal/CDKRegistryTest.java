/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.test.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ConfigureDependentFrameworksListener;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.MinishiftServiceManagerEnvironmentLoader;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.cdk.server.core.internal.registry.CDKRegistryProvider;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.junit.After;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Test that the registry locator works as expected
 * 
 * Problems with this test:  At the end, the SSL Certificate dialog appears
 * and I don't know how to stop it. For that reason, this test is run last. 
 */

public class CDKRegistryTest extends TestCase {
	@Test
	public void testRegistryURL() throws Exception {
		ConfigureDependentFrameworksListener configureListener = (ConfigureDependentFrameworksListener) CDKCoreActivator
				.getDefault().getConfigureDependentFrameworksListener();
		configureListener.disable();

		CredentialService.getCredentialModel().addDomain("redhat.com", "redhat.com", true);
		CredentialService.getCredentialModel()
				.addCredentials(CredentialService.getCredentialModel().getDomain("redhat.com"), "user", "password");
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		createCDKFile("Basic", null, null);
		IServer cdkServer = createServer("openshift33");

		ServiceManagerEnvironment adb = createLoader(cdkServer);
		IConnection con = util.createOpenshiftConnection(adb, ConnectionsRegistrySingleton.getInstance());
		assertNotNull(con);
		// Can't test the registry provider model bc it hides the internal details
		CDKRegistryProvider prov = new CDKRegistryProvider() {
			protected ServiceManagerEnvironment getServiceManagerEnvironment(IServer server) {
				try {
					return createLoader(server);
				} catch (Exception e) {
					fail(e.getMessage());
				}
				return null;
			}
		};
		IStatus reg = prov.getRegistryURL(con);
		assertNotNull(reg);
		assertFalse(reg.isOK());

		ControllableServerBehavior beh = (ControllableServerBehavior) cdkServer
				.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		beh.setServerStarted();
		reg = prov.getRegistryURL(con);
		assertNotNull(reg);
		assertTrue(reg.isOK());

		configureListener.enable();
		beh.setServerStopped();
	}

	@After
	public void cleanup() {
		CredentialService.getCredentialModel()
				.removeCredentials(CredentialService.getCredentialModel().getDomain("redhat.com"), "user");

		ArrayList<IConnection> cons = new ArrayList(ConnectionsRegistrySingleton.getInstance().getAll());
		Iterator<IConnection> con = cons.iterator();
		while (con.hasNext()) {
			ConnectionsRegistrySingleton.getInstance().remove(con.next());
		}
		IServer[] all = ServerCore.getServers();
		for (int i = 0; i < all.length; i++) {
			try {
				all[i].delete();
			} catch (CoreException ce) {
				ce.printStackTrace();
			}
		}
	}

	private void createCDKFile(String authType, String user, String pass) {
		File f = new File(getDotCDKFile());
		if (f.exists()) {
			f.delete();
		}
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}

		StringBuilder sb = new StringBuilder();
		if (authType != null) {
			sb.append("openshift.auth.scheme=");
			sb.append(authType);
			sb.append("\n");
		}
		if (user != null) {
			sb.append("openshift.auth.username=");
			sb.append(user);
			sb.append("\n");
		}
		if (pass != null) {
			sb.append("openshift.auth.password=");
			sb.append(pass);
			sb.append("\n");
		}

		Path path = Paths.get(getDotCDKFile());
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private IServer createServer(String name) {
		try {
			IServerType stt = getServerType(CDKServer.CDK_V3_SERVER_TYPE);
			assertNotNull(stt);
			IServerWorkingCopy swc = stt.createServer(name, null, null, new NullProgressMonitor());
			try {
				File f = File.createTempFile("minishift", System.currentTimeMillis() + "");
				swc.setAttribute(CDK3Server.MINISHIFT_FILE, f.getAbsolutePath());
				f.createNewFile();
			} catch (IOException ioe) {
				swc.setAttribute(CDK3Server.MINISHIFT_FILE, "/home/user/minishift_folder/minishift");
			}
			swc.setAttribute(CDKServer.PROP_USERNAME, "user");
			swc.setAttribute(CDK3Server.PROP_HYPERVISOR, "virtualbox");
			return swc.save(true, new NullProgressMonitor());
		} catch (CoreException ce) {
			fail(ce.getMessage());
		}
		return null;
	}

	private IServerType getServerType(String id) {
		IServerType[] allTypes = ServerCore.getServerTypes();
		for (int i = 0; i < allTypes.length; i++) {
			if (allTypes[i].getId().equals(id))
				return allTypes[i];
		}
		return null;
	}

	private String getDotCDKFolder() {
		IPath stateLoc = ((Plugin) CDKCoreActivator.getDefault()).getStateLocation();
		IPath folder = stateLoc.append("testFolder");
		return folder.toOSString();
	}

	private String getDotCDKFile() {
		IPath stateLoc = ((Plugin) CDKCoreActivator.getDefault()).getStateLocation();
		IPath folder = stateLoc.append("testFolder").append(".cdk");
		return folder.toOSString();
	}

	private static class MinishiftServiceManagerEnvironmentLoaderMock extends MinishiftServiceManagerEnvironmentLoader {
		private Map<String, String> dockerMap;
		private String registry;

		public MinishiftServiceManagerEnvironmentLoaderMock(Map<String, String> dockerMap, String registry) {
			this.dockerMap = dockerMap;
			this.registry = registry;
		}

		protected Map<String, String> loadDockerEnv(IServer server) {
			return dockerMap;
		}

		@Override
		protected Properties loadOpenshiftConsoleDetails(IServer server, boolean suppressError) {
			Properties p = new Properties();
			p.put("HOST", "192.168.99.100");
			p.put("PORT", "8443");
			p.put("CONSOLE_URL", "https://192.168.99.100:8443");
			return p;
		}

		@Override
		protected String getOpenshiftRegistry(IServer server, boolean suppressErrors) {
			return registry;
		}
	}

	private ServiceManagerEnvironment createLoader(IServer server) throws URISyntaxException {
		return createLoader(server, "192.168.99.100");
	}

	private ServiceManagerEnvironment createLoader(IServer server, String host) throws URISyntaxException {
		return createLoader(server, host, "2376", "172.30.1.1:5000");
	}

	private ServiceManagerEnvironment createLoader(IServer server, String host, String port, String registry)
			throws URISyntaxException {
		HashMap<String, String> env = new HashMap<>();
		env.put("DOCKER_HOST", "tcp://" + host + ":" + port);
		env.put("DOCKER_CERT_PATH", "/cert/path/.docker");
		env.put("DOCKER_TLS_VERIFY", "1");
		env.put("DOCKER_MACHINE_NAME", "e5d7d0a");
		return new MinishiftServiceManagerEnvironmentLoaderMock(env, registry).loadServiceManagerEnvironment(server,
				true);
	}

}
