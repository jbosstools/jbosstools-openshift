/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.js.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.debug.DebugSessionTracker;
import org.jboss.tools.openshift.internal.js.storage.SessionStorage;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public class NodeDebugSessionTracker implements DebugSessionTracker {

	@Override
	public void startDebugSession(IServer server, int port) throws CoreException {
		NodeDebugLauncher.launch(server, port);
	}

	@Override
	public boolean isDebugSessionAlive(IServer server) {
		return SessionStorage.get().contains(server);
	}

}
