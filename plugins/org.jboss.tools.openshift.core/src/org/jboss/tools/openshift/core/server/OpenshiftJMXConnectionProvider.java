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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.jmx.integration.AbstractJBossJMXConnectionProvider;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionProvider;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.jmx.jolokia.JolokiaConnectionWrapper;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

public class OpenshiftJMXConnectionProvider extends AbstractJBossJMXConnectionProvider {
	public static final String PROVIDER_ID = "org.jboss.tools.openshift.core.server.OpenshiftJMXConnection"; //$NON-NLS-1$

	@Override 
	protected boolean getConnectionPersistenceBehavior() {
		return ON_START;
	}
	
	@Override
	protected boolean belongsHere(IServer server) {
		if( server != null && server.getServerType().getId().equals(OpenShiftServer.SERVER_TYPE_ID) &&
		        OpenShiftServerUtils.isJavaProject(server)) {
			return true;
		}
		return false;
	}

	@Override
	public String getId() {
		return  PROVIDER_ID;
	}

	@Override
	protected IConnectionWrapper createConnection(IServer server) {
		IConnection openshiftCon = OpenShiftServerUtils.getConnection(server);
		IResource resource = OpenShiftServerUtils.getResource(server);
		
		String token = ((Connection)openshiftCon).getToken();
		String projName =  resource.getNamespace();
		List<IPod> pods = ResourceUtils.getPodsForResource(resource, resource.getProject().getResources(ResourceKind.POD));
		if( pods.size() == 0 ) {
			return null;
		}
		String pod =  pods.get(0).getName();
		
		String host = server.getHost(); 
		String url = "https://" + host + ":8443/api/v1/namespaces/" 
				+ projName + "/pods/https:" + pod + ":8778/proxy/jolokia/";
		String headerKey = "Authorization";
		String headerVal = "Bearer " + token;
		
		JolokiaConnectionWrapper cw = new JolokiaConnectionWrapper() {
			public IConnectionProvider getProvider() {
				return ExtensionManager.getProvider(PROVIDER_ID);
			}
			protected void verifyServerReachable() throws IOException {
				super.verifyServerReachable();
			}
		};
		cw.setId(server.getName());
		cw.setUrl(url);
		cw.setType("POST");
		cw.setIgnoreSSLErrors(true);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(headerKey, headerVal);
		cw.setHeaders(headers);
		return cw;
	}

	@Override
	public String getName(IConnectionWrapper wrapper) {
		return ((JolokiaConnectionWrapper)wrapper).getId();
	}
}
