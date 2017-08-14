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
package org.jboss.tools.openshift.internal.core.server.debug;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.server.core.IServer;

import com.openshift.restclient.model.IPod;

/**
 * A debug context to be used in debug mode operations
 * 
 * @author Fred Bricon
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class DebugContext {
	
	public static final String DEFAULT_DEVMODE_KEY = "DEV_MODE";
	public static final String DEFAULT_DEBUG_PORT_KEY = "DEBUG_PORT";
	public static final String DEFAULT_DEBUG_PORT = "8787";

	public static final int NO_DEBUG_PORT = -1;

	private IServer server;
	private boolean debugEnabled;
	private boolean devmodeEnabled;
	private String devmodeKey;
	private String debugPortKey;
	private int debugPort = NO_DEBUG_PORT;
	private IDebugListener listener;
	private IPod pod;

	public DebugContext(IServer server) {
		this(server, DEFAULT_DEVMODE_KEY, DEFAULT_DEBUG_PORT_KEY, DEFAULT_DEBUG_PORT);
	}

	/**
	 * Creates a debugging context instance for the given server behaviour with the
	 * given environment keys/values that should be used in the OpenShift deployment
	 * config.
	 * 
	 * @param behaviour the server behaviour that will be used for this context.
	 * @param devmodeKey
	 *            the env key to use to get/set devmode in the deployment config
	 * @param debugPortKey
	 *            the env key to use to get/set the debug port in the deployment
	 *            config
	 * @param debugPort
	 *            the debug port to use in the deployment config
	 * @return
	 */
	public DebugContext(IServer server, String devmodeKey, String debugPortKey, String debugPort) {
		Assert.isNotNull(server);
		Assert.isNotNull(devmodeKey);
		Assert.isNotNull(debugPortKey);
		Assert.isNotNull(debugPort);

		this.server = server;
		this.devmodeKey = devmodeKey;
		this.debugPortKey = debugPortKey;
		this.debugPort = getDebugPort(debugPort);
	}

	public IServer getServer() {
		return server;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setDevmodeEnabled(boolean devmodeEnabled) {
		this.devmodeEnabled = devmodeEnabled;
	}

	public boolean isDevmodeEnabled() {
		return devmodeEnabled;
	}

	public int getDebugPort() {
		return debugPort;
	}

	public IPod getPod() {
		return this.pod;
	}

	public void setDebugListener(IDebugListener listener) {
		this.listener = listener;
	}

	void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}

	IDebugListener getDebugListener() {
		return listener;
	}

	String getDevmodeKey() {
		return devmodeKey;
	}

	String getDebugPortKey() {
		return debugPortKey;
	}

	void setPod(IPod pod) {
		this.pod = pod;
	}

	private int getDebugPort(String debugPort) {
		if (StringUtils.isBlank(debugPort)) {
			return NO_DEBUG_PORT;
		}
		
		try {
			return Integer.parseInt(debugPort);
		} catch(NumberFormatException e) {
			return NO_DEBUG_PORT;
		}
	}

}