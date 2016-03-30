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
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection.Builder;
import org.eclipse.wst.server.core.IServer;

public class CDKDockerUtility {

	private DockerConnectionManager mgr;

	public CDKDockerUtility(DockerConnectionManager mgr) {
		this.mgr = mgr;
	}
	
	public CDKDockerUtility() {
		this(org.eclipse.linuxtools.docker.core.DockerConnectionManager.getInstance());
	}
	
	public String getNextName(IServer server) {
		// Cache all names
		IDockerConnection[] cons = mgr.getConnections();
		List<String> names = Arrays.stream(cons).map(IDockerConnection::getName).collect(Collectors.toList());

		// Find a name that doesnt match existing connection
		final String nameBase = server.getName();
		String name = nameBase;
		int count = 0;
		boolean done = false;
		while(!done) {
			if( names.contains(name)) {
				count++;
				name = nameBase + " (" + count + ")";
			} else {
				done = true;
			}
		}
		return name;
	}
	
	public IDockerConnection findDockerConnection(ServiceManagerEnvironment adb) {
		final String dockerHost = adb == null ? null : adb.env == null ? null : adb.env.get("DOCKER_HOST");
		
		if( dockerHost != null ) {
			IDockerConnection[] cons = mgr.getConnections();
			String httpHost = dockerHost.replace("tcp://", "http://");
			String httpsHost = dockerHost.replace("tcp://", "https://");
			for( int i = 0; i < cons.length; i++ ) {
				if( cons[i].getUri().equals(dockerHost) || cons[i].getUri().equals(httpHost) || cons[i].getUri().equals(httpsHost)) {
					return cons[i];
				}
			}
		}
		return null;
	}
	
	public boolean dockerConnectionExists(ServiceManagerEnvironment adb) {
		return findDockerConnection(adb) != null;
	}
	
	public IDockerConnection buildDockerConnection(IServer server, ServiceManagerEnvironment adb) throws DockerException {
		final String dockerHost = adb.env.get("DOCKER_HOST");

		final Builder tcpConnectionBuilder = new DockerConnection.Builder()
				.name(getNextName(server)).tcpHost(dockerHost);
		String tlsVerifyString = adb.env.get("DOCKER_TLS_VERIFY");
		boolean tlsVerify = tlsVerifyString == null ? false : (Integer.parseInt(tlsVerifyString) != 0);
		if( tlsVerify ) {
			String tlsCertPath = adb.env.get("DOCKER_CERT_PATH");
			tcpConnectionBuilder.tcpCertPath(tlsCertPath);
		}
		return tcpConnectionBuilder.build();
	}
	
	public IDockerConnection createDockerConnection(IServer server, ServiceManagerEnvironment adb) throws DockerException {
		IDockerConnection con = buildDockerConnection(server, adb);
		mgr.addConnection(con);
		return con;
	}
}
