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

import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ADBInfo;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKDockerUtility;
import org.junit.Test;

import junit.framework.TestCase;

public class CDKDockerUtilityTest extends TestCase {
	
	@Test
	public void testFindDockerConnection() throws Exception {
		DockerConnectionManager mgr = org.eclipse.linuxtools.docker.core.DockerConnectionManager.getInstance();
		IDockerConnection con = null;
		try {
			MockCDKDockerUtility util = new MockCDKDockerUtility("name1");
			ADBInfo adb = createADB();
			ADBInfo wrongHost = createADB("10.1.2.3");
			ADBInfo wrongPort = createADB("10.1.2.2", "500");
			assertTrue(mgr.getConnections().length == 0);
			assertFalse(util.dockerConnectionExists(adb));
			con = util.createDockerConnection(null, adb);
			assertTrue(util.dockerConnectionExists(adb));
			assertFalse(util.dockerConnectionExists(wrongHost));
			assertFalse(util.dockerConnectionExists(wrongPort));
			mgr.removeConnection(con);
			assertFalse(util.dockerConnectionExists(adb));
			assertTrue(mgr.getConnections().length == 0);
		} finally {
			if( con != null ) {
				mgr.removeConnection(con);
			}
		}
	}
	
	private ADBInfo createADB() throws URISyntaxException {
		return createADB("10.1.2.2");
	}
	
	private ADBInfo createADB(String host) throws URISyntaxException {
		return createADB(host, "2376");
	}
	
	private ADBInfo createADB(String host, String port) throws URISyntaxException {
		HashMap<String,String> env = new HashMap<>();
		env.put("DOCKER_HOST","tcp://" + host + ":" + port);
		env.put("DOCKER_CERT_PATH","/home/rob/Downloads/cdk/github/openshift-vagrant/cdk-v2/.vagrant/machines/cdk/virtualbox/.docker");
		env.put("DOCKER_TLS_VERIFY","1");
		env.put("DOCKER_MACHINE_NAME","e5d7d0a");
		return new ADBInfo(env);
	}
	
	private class MockCDKDockerUtility extends CDKDockerUtility {
		private String name;
		public MockCDKDockerUtility(String name) {
			this.name = name;
		}
		public String getNextName(IServer server, DockerConnectionManager mgr) {
			return name;
		}
	}
}
