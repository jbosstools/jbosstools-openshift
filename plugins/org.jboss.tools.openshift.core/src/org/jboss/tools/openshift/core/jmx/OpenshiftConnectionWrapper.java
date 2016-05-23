/*******************************************************************************
 * Copyright (c) 2006 Jeff Mesnil
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Rob Stryker" <rob.stryker@redhat.com> - Initial implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.jmx.core.IConnectionProvider;
import org.jboss.tools.jmx.core.IConnectionProviderEventEmitter;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.jmx.core.IJMXRunnable;
import org.jboss.tools.jmx.core.JMXActivator;
import org.jboss.tools.jmx.core.JMXCoreMessages;
import org.jboss.tools.jmx.core.JMXException;
import org.jboss.tools.jmx.core.tree.NodeUtils;
import org.jboss.tools.jmx.core.tree.Root;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.portforwarding.PortForwardingUtils;
import org.jboss.tools.openshift.internal.core.server.debug.DebuggingContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugUtils;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;

public class OpenshiftConnectionWrapper implements IConnectionWrapper {
	private JMXConnector connector;
	private MBeanServerConnection connection;
	private Root root;
	private boolean isLoading;
	private boolean isConnected;
	private IServer server;
	private OpenshiftConnectionProvider provider;
	private JMXConnectorProvider factory;

	public OpenshiftConnectionWrapper(OpenshiftConnectionProvider provider, IServer server,
			JMXConnectorProvider factory) throws MalformedURLException {
		this.provider = provider;
		this.server = server;
		this.factory = factory;
		this.isConnected = false;
		this.isLoading = false;
	}

	public IConnectionProvider getProvider() {
		return provider;
	}

	public String getName() {
		return "Connection for " + server.getId();
	}

	public boolean canControl() {
		return true;
	}

	public synchronized void connect() throws IOException {

		IDeploymentConfig dc = OpenShiftServerUtils.getDeploymentConfig(server);
		DebuggingContext debugContext = OpenShiftDebugUtils.get().getDebuggingContext(dc);
		HashMap<String, Object> env = new HashMap<>();
		env.put(JMXConnector.CREDENTIALS,
				new String[] { debugContext.getAdminUsername(), debugContext.getAdminPassword() });
		IPod firstPod = OpenShiftDebugUtils.get().getFirstPod(dc);
		if (firstPod != null) {
			int port = getLocalAdminPort(firstPod);
			String url = "service:jmx:remoting-jmx://localhost:" + port;
			// try to connect
			connector = factory.newJMXConnector(new JMXServiceURL(url), env);
			connector.connect();
			connection = connector.getMBeanServerConnection();
			isConnected = true;
			provider.fireChanged(this);
		}
	}

	public synchronized void disconnect() throws IOException {
		// close
		root = null;
		isConnected = false;
		try {
			if (connector != null) {
				connector.close();
			}
		} finally {
			provider.fireChanged(this);
		}
		connector = null;
		connection = null;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public Root getRoot() {
		return root;
	}

	public void loadRoot(IProgressMonitor monitor) throws CoreException {
		if (isConnected && root == null && !isLoading) {
			try {
				isLoading = true;
				root = NodeUtils.createObjectNameTree(this, monitor);
			} finally {
				isLoading = false;
			}
		}
	}

	public void run(IJMXRunnable runnable) throws JMXException {
		try {
			runnable.run(connection);
		} catch (Exception e) {
			IStatus s = new Status(IStatus.ERROR, JMXActivator.PLUGIN_ID,
					JMXCoreMessages.DefaultConnection_ErrorRunningJMXCode, e);
			throw new JMXException(s);
		}
	}

	public void run(IJMXRunnable runnable, HashMap<String, String> prefs) throws JMXException {
		run(runnable);
	}

	private int getLocalAdminPort(IPod pod) {
		return PortForwardingUtils.getForwardablePorts(pod).stream().filter(p -> p.getRemotePort() == 9999)
				.mapToInt(p -> p.getLocalPort()).findAny().orElse(9999);
	}

}
