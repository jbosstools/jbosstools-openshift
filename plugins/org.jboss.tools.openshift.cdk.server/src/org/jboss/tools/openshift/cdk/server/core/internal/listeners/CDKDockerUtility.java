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

import java.util.ArrayList;
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

	public String getNextName(IServer server, DockerConnectionManager mgr) {
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
	
	public IDockerConnection findDockerConnection(ADBInfo adb) {
		DockerConnectionManager mgr = org.eclipse.linuxtools.docker.core.DockerConnectionManager.getInstance();
		final String dockerHost = adb.env.get("DOCKER_HOST");
		
		IDockerConnection[] cons = mgr.getConnections();
		String httpHost = dockerHost.replace("tcp://", "http://");
		String httpsHost = dockerHost.replace("tcp://", "https://");
		for( int i = 0; i < cons.length; i++ ) {
			if( cons[i].getUri().equals(dockerHost) || cons[i].getUri().equals(httpHost) || cons[i].getUri().equals(httpsHost)) {
				return cons[i];
			}
		}
		return null;
	}
	
	public boolean dockerConnectionExists(ADBInfo adb) {
		return findDockerConnection(adb) != null;
	}
	
	public void createDockerConnection(IServer server, ADBInfo adb) throws DockerException {
		DockerConnectionManager mgr = org.eclipse.linuxtools.docker.core.DockerConnectionManager.getInstance();
		final String dockerHost = adb.env.get("DOCKER_HOST");

		final Builder tcpConnectionBuilder = new DockerConnection.Builder()
				.name(getNextName(server, mgr)).tcpHost(dockerHost);
		String tlsVerifyString = adb.env.get("DOCKER_TLS_VERIFY");
		boolean tlsVerify = (Integer.parseInt(tlsVerifyString) != 0);
		if( tlsVerify ) {
			String tlsCertPath = adb.env.get("DOCKER_CERT_PATH");
			tcpConnectionBuilder.tcpCertPath(tlsCertPath);
		}
		DockerConnection con = tcpConnectionBuilder.build();
		IDockerConnection[] other = mgr.getConnections();
		mgr.addConnection(con);
	}
}
