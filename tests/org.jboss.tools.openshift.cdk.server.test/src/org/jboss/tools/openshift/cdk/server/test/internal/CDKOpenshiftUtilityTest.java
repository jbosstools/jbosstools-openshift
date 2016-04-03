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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.junit.Test;

import junit.framework.TestCase;

public class CDKOpenshiftUtilityTest extends TestCase {
	@Test
	public void testOpenshiftConnectionCredentials() throws Exception {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		ServiceManagerEnvironment adb = createADB();
		IServer s = mockServer("openshift33");
		
		createCDKFile("Basic", null, null);
		IConnection con = util.createOpenshiftConnection(s, adb, null);
		assertNotNull(con);
		assertEquals(con.getUsername(), "openshift-dev");
		assertEquals(con.getPassword(), "devel");
		
		createCDKFile("Basic", "test", null);
		con = util.createOpenshiftConnection(s, adb, null);
		assertNotNull(con);
		assertEquals(con.getUsername(), "test");
		assertEquals(con.getPassword(), null);

		createCDKFile("Basic", "test", "pass");
		con = util.createOpenshiftConnection(s, adb, null);
		assertNotNull(con);
		assertEquals(con.getUsername(), "test");
		assertEquals(con.getPassword(), "pass");
	}

	@Test
	public void testOpenshiftConnectionAdded() throws Exception {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		ServiceManagerEnvironment adb = createADB();
		IServer s = mockServer("openshift33");
		
		createCDKFile("Basic", null, null);
		ConnectionsRegistry registry = (ConnectionsRegistry) mock(ConnectionsRegistry.class);
		
		IConnection con = util.createOpenshiftConnection(s, adb, registry);
		assertNotNull(con);
		verify(registry).add(con);
	}

	
	private void createCDKFile(String authType, String user, String pass ) {
		File f = new File(getDotCDKFile());
		if( f.exists()) {
			f.delete();
		}
		if( !f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		
		StringBuilder sb = new StringBuilder();
		if( authType != null ) {
			sb.append("openshift.auth.scheme=");
			sb.append(authType);
			sb.append("\n");
		}
		if( user != null ) {
			sb.append("openshift.auth.username=");
			sb.append(user);
			sb.append("\n");
		}
		if( pass != null ) {
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
	
	private IServer mockServer(String name) {
		IServer server = mock(IServer.class);
		when(server.getName()).thenReturn(name);
		when(server.getAttribute(CDKServer.PROP_FOLDER, (String)null)).thenReturn(getDotCDKFolder());
		return server;
	}
	
	private String getDotCDKFolder() {
		IPath stateLoc = ((Plugin)CDKCoreActivator.getDefault()).getStateLocation();
		IPath folder = stateLoc.append("testFolder");
		return folder.toOSString();
	}
	private String getDotCDKFile() {
		IPath stateLoc = ((Plugin)CDKCoreActivator.getDefault()).getStateLocation();
		IPath folder = stateLoc.append("testFolder").append(".cdk");
		return folder.toOSString();
	}

	private ServiceManagerEnvironment createADB() throws URISyntaxException {
		return createADB("10.1.2.2");
	}

	private ServiceManagerEnvironment createADB(String host) throws URISyntaxException {
		return createADB(host, "2376");
	}

	private ServiceManagerEnvironment createADB(String host, String port) throws URISyntaxException {
		HashMap<String,String> env = new HashMap<>();
		env.put("DOCKER_HOST","tcp://" + host + ":" + port);
		env.put("DOCKER_CERT_PATH","/cert/path/.docker");
		env.put("DOCKER_TLS_VERIFY","1");
		env.put("DOCKER_MACHINE_NAME","e5d7d0a");
		return new ServiceManagerEnvironment(env);
	}

}
