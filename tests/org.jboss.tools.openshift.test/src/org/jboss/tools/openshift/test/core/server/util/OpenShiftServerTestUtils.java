/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.util;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public class OpenShiftServerTestUtils {

	public static void cleanup() {
		IServer[] all = ServerCore.getServers();
		for (int i = 0; i < all.length; i++) {
			try {
				all[i].delete();
			} catch (CoreException ce) {
				ce.printStackTrace();
			}
		}
	}

	public static IServer createOpenshift3Server(String name, String profile)
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		return createOpenshift3Server(name, profile, null, null);
	}

	public static IServer createOpenshift3Server(String name, String profile, IFile file)
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		return createOpenshift3Server(name, profile, null, null, file);
	}

	public static IServer createOpenshift3Server(String name, String profile, IService service,
			IOpenShiftConnection connection) throws CoreException, UnsupportedEncodingException, MalformedURLException {
		 return createOpenshift3Server(name, profile, service, connection, null);
	}

	public static IServer createOpenshift3Server(String name, String profile, IService service,
			IOpenShiftConnection connection, IFile file) throws CoreException, UnsupportedEncodingException, MalformedURLException {
		IServerWorkingCopy workingCopy = createOpenshift3ServerWorkingCopy(name, profile, service, connection, file);
		return workingCopy.save(true, null);
	}
	
	public static IServerWorkingCopy createOpenshift3ServerWorkingCopy(String name, IFile file) throws CoreException {
		IServerType type = OpenShiftServerUtils.getServerType();
		return type.createServer(name, file, null);
	}

	public static IServerWorkingCopy createOpenshift3ServerWorkingCopy(String name) throws CoreException {
		return createOpenshift3ServerWorkingCopy(name, null);
	}

	public static IServerWorkingCopy createOpenshift3ServerWorkingCopy(String name, String profile, IService service,
			IOpenShiftConnection connection) throws CoreException, UnsupportedEncodingException, MalformedURLException {
		return createOpenshift3ServerWorkingCopy(name, profile, service, connection, null);
	}

	public static IServerWorkingCopy createOpenshift3ServerWorkingCopy(String name, String profile, IService service,
			IOpenShiftConnection connection, IFile file) throws CoreException, UnsupportedEncodingException, MalformedURLException {
		IServerWorkingCopy wc = createOpenshift3ServerWorkingCopy(name, file);
		String serviceId = service == null ? null : OpenShiftResourceUniqueId.get(service);
		String connectionUrl = connection == null ? null : ConnectionURL.forConnection(connection).getUrl();
		OpenShiftServerUtils.updateServer(name, "http://www.example.com", "dummy", connectionUrl, "dummy", serviceId,
				"dummy", "dummy", "dummy", "dummy", "dummy", profile, wc);
		return wc;
	}

	public static IServer mockServer(IServerWorkingCopy workingCopy, IResource resource, Connection connection)
			throws UnsupportedEncodingException, MalformedURLException {
		IServer server = mock(IServer.class);

		if (connection != null) {
			String connectionUrl = ConnectionURL.forConnection(connection).toString();
			doReturn(connectionUrl).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_CONNECTIONURL), anyString());
		}

		String resourceUniqueId = OpenShiftResourceUniqueId.get(resource);
		doReturn(resourceUniqueId).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		doReturn(workingCopy).when(server).createWorkingCopy();
		return server;
	}

	public static IServer mockServer(IResource resource, Connection connection)
			throws UnsupportedEncodingException, MalformedURLException {
		return mockServer(mockServerWorkingCopy(), resource, connection);
	}

	public static IControllableServerBehavior mockServerBehaviour(IServer server) {
		IControllableServerBehavior behaviour = mock(IControllableServerBehavior.class);
		doReturn(server).when(behaviour).getServer();
		return behaviour;
	}

	public static IServerWorkingCopy mockServerWorkingCopy() {
		return mock(IServerWorkingCopy.class);
	}
}
