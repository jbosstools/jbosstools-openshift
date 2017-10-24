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
package org.jboss.tools.openshift.core.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.jmx.integration.AbstractJBossJMXConnectionProvider;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionProvider;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.jmx.jolokia.JolokiaConnectionWrapper;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

public class OpenshiftJMXConnectionProvider extends AbstractJBossJMXConnectionProvider {
	
	private static final String AUTHORIZATION_HEADER_KEY = "Authorization"; //$NON-NLS-1$
	private static final String AUTHORIZATION_HEADER_VALUE_PREFIX = "Bearer "; //$NON-NLS-1$
	
	public static final String PROVIDER_ID = "org.jboss.tools.openshift.core.server.OpenshiftJMXConnection"; //$NON-NLS-1$

	@Override 
	protected boolean getConnectionPersistenceBehavior() {
		return ON_START;
	}
	
	@Override
	protected boolean belongsHere(IServer server) {
		return server != null
				&& OpenShiftServer.SERVER_TYPE_ID.equals(server.getServerType().getId())
				&& OpenShiftServerUtils.isJavaProject(server);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	protected IConnectionWrapper createConnection(IServer server) {
		IConnection openshiftCon = OpenShiftServerUtils.getConnection(server);
		String url = computeJolokiaURL(server);
		if(url != null) {
			JolokiaConnectionWrapper cw = new JolokiaConnectionWrapper() {
				@Override
				public IConnectionProvider getProvider() {
					return ExtensionManager.getProvider(PROVIDER_ID);
				}
				
			};
			cw.setId(server.getName());
			cw.setUrl(url);
			cw.setType("POST");
			cw.setIgnoreSSLErrors(true);
			Map<String, String> headers = new HashMap<>();
			headers.put(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE_PREFIX + ((Connection)openshiftCon).getToken());
			cw.setHeaders(headers);
			return cw;
		} else {
			return null;
		}
	}

	protected String computeJolokiaURL(IServer server) {
		IResource resource = OpenShiftServerUtils.getResource(server, new NullProgressMonitor());
		if (resource != null) {		
			String projName =  resource.getNamespace();
			List<IPod> pods = ResourceUtils.getPodsFor(resource, resource.getProject().getResources(ResourceKind.POD));
			if( !pods.isEmpty() ) {
				String podName =  pods.get(0).getName();
				String host = server.getHost();
				String portUrlPart = getOpenShiftPort(server);
				return "https://" + host + portUrlPart + "/api/v1/namespaces/" + projName + "/pods/https:" + podName + ":8778/proxy/jolokia/";
			}
		}
		return null;
	}

	protected String getOpenShiftPort(IServer server) {
		int port = -1;
		try {
			URL connectionUrl = new URL(server.getAttribute(OpenShiftServerUtils.ATTR_CONNECTIONURL, ""));
			port = connectionUrl.getPort();
		} catch (MalformedURLException e) {
			OpenShiftCoreActivator.logError("Cannot determine port for JMX Connection from OpenShift connection URL", e); //$NON-NLS-1$
		}
		return port != -1 ? ":" + port : "";
	}

	@Override
	public String getName(IConnectionWrapper wrapper) {
		return ((JolokiaConnectionWrapper)wrapper).getId();
	}
}
