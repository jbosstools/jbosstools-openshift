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

import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKDockerUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

public class CDKDockerUtilityTest extends TestCase {

	private DockerConnectionManager mgr;
	private CDKDockerUtility util;

	@Override
	@Before
	public void setUp() {
		mgr = mock(DockerConnectionManager.class);
		when(mgr.getConnections()).thenReturn(new IDockerConnection[]{});
		util = new CDKDockerUtility(mgr);
	}

	@Test
	public void testDockerConnectionExists() throws Exception {
		assertFalse(util.dockerConnectionExists(null));

		ServiceManagerEnvironment adb = createADB();
		assertFalse(util.dockerConnectionExists(adb));	

		IDockerConnection existingConnection = mock(IDockerConnection.class);
		when(existingConnection.getUri()).thenReturn("https://10.1.2.2:2376");
		when(mgr.getConnections()).thenReturn(new IDockerConnection[]{existingConnection });

		assertTrue(util.dockerConnectionExists(adb));

		ServiceManagerEnvironment wrongHost = createADB("10.1.2.3");
		assertFalse(util.dockerConnectionExists(wrongHost));

		ServiceManagerEnvironment wrongPort = createADB("10.1.2.2", "500");
		assertFalse(util.dockerConnectionExists(wrongPort));
	}

	@Test
	public void testGetNextName() throws Exception {
		String name = "foo";
		IServer server = mockServer(name);
		assertEquals(name, util.getNextName(server));	

		IDockerConnection existingConnection = mock(IDockerConnection.class);
		when(existingConnection.getName()).thenReturn(name);
		when(mgr.getConnections()).thenReturn(new IDockerConnection[]{existingConnection });

		assertEquals(name+" (1)", util.getNextName(server));	
	}

	@Test
	public void testCreateDockerConnection() throws Exception {
		String name = "foo";
		IServer server = mockServer(name);
		ServiceManagerEnvironment adb = createADB();
		IDockerConnection dockerConnection = util.createDockerConnection(server, adb);
		assertNotNull(dockerConnection);
		verify(mgr).addConnection(dockerConnection);

		assertEquals(name, dockerConnection.getName());	
		assertEquals("https://10.1.2.2:2376", dockerConnection.getUri());
		assertEquals("/cert/path/.docker", dockerConnection.getTcpCertPath());
	}

	private ServiceManagerEnvironment createADB() throws URISyntaxException {
		return createADB("10.1.2.2");
	}

	private ServiceManagerEnvironment createADB(String host) throws URISyntaxException {
		return createADB(host, "2376");
	}

	private IServer mockServer(String name) {
		IServer server = mock(IServer.class);
		when(server.getName()).thenReturn(name);
		return server;
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
