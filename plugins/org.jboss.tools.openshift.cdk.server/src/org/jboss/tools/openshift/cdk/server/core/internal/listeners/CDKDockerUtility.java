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
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.core.TCPConnectionSettings;
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
		final String dockerHost = adb == null ? null : adb.env == null ? null : getDockerHost(adb);
		
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
		final String dockerHost = getDockerHost(adb);
		final String tlsCertPath = getTlsCertPath(adb);
		TCPConnectionSettings set = new TCPConnectionSettings(dockerHost, tlsCertPath);
		return new DockerConnection.Builder()
				.name(getNextName(server)).tcpConnection(set);
	}

	/**
	 * Looks-up the host name and port to connect to Docker.
	 * @param adb the {@link ServiceManagerEnvironment}
	 * @return the host name and port or <code>null</code> if it was not set.
	 */
	private String getDockerHost(ServiceManagerEnvironment adb) {
		return adb.env.get("DOCKER_HOST");
	}
	
	/**
	 * Looks-up the path to the client certificates to connect to Docker
	 * @param adb the {@link ServiceManagerEnvironment}
	 * @return the value of {@code DOCKER_CERT_PATH} in the environment variable
	 *         or <code>null</code> if it was not present or if the {@code DOCKER_TLS_VERIFY} was not present or
	 *         not set to {@code 1}.
	 */
	private String getTlsCertPath(final ServiceManagerEnvironment adb) {
		final String tlsVerifyString = adb.env.get("DOCKER_TLS_VERIFY");
		boolean tlsVerify = tlsVerifyString == null ? false : (Integer.parseInt(tlsVerifyString) != 0);
		if( tlsVerify ) {
			String tlsCertPath = adb.env.get("DOCKER_CERT_PATH");
			if( tlsCertPath != null ) {
				tlsCertPath = tlsCertPath.trim();
				if( tlsCertPath.startsWith("\'") && tlsCertPath.endsWith("\'") && tlsCertPath.length() > 1) {
					tlsCertPath = tlsCertPath.substring(1, tlsCertPath.length()-1);
				}
			}
			return tlsCertPath;
		}		
		return null;
	}

	public IDockerConnection createDockerConnection(IServer server, ServiceManagerEnvironment adb) throws DockerException {
		IDockerConnection con = buildDockerConnection(server, adb);
		mgr.addConnection(con);
		return con;
	}
}
