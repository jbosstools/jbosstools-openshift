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
package org.jboss.tools.openshift.internal.core.server.debug;

import com.openshift.restclient.model.IPod;

public class DebuggingContext {
	
	private int debugPort = -1;
	
	private boolean isDebugEnabled;
	
	private IPod pod;
	
	private String adminUsername;
	private String adminPassword;

	private IDebugListener listener;

	public int getDebugPort() {
		return debugPort;
	}

	public void setDebugPort(int debugPort) {
		this.debugPort = debugPort;
	}

	public boolean isDebugEnabled() {
		return isDebugEnabled;
	}

	public void setDebugEnabled(boolean isDebugEnabled) {
		this.isDebugEnabled = isDebugEnabled;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public IPod getPod() {
		return pod;
	}

	public void setPod(IPod pod) {
		this.pod = pod;
	}

	public IDebugListener getDebugListener() {
		return listener;
	}

	public void setDebugListener(IDebugListener listener) {
		this.listener = listener;
	}
}
