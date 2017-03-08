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
	
	public static final int NO_DEBUG_PORT = -1;
	
	private int debugPort = NO_DEBUG_PORT;
	
	private boolean isDebugEnabled;
	
	private IPod pod;

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
