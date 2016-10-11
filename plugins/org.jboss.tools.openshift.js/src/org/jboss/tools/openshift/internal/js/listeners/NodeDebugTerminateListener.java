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
package org.jboss.tools.openshift.internal.js.listeners;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.js.storage.SessionStorage;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public class NodeDebugTerminateListener implements IDebugEventSetListener {
	ILaunchConfiguration nodeDebugLaunch;
	IServer server;

	public NodeDebugTerminateListener(ILaunchConfiguration nodeDebugLaunch, IServer server) {
		this.nodeDebugLaunch = nodeDebugLaunch;
		this.server = server;
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getKind() == DebugEvent.TERMINATE) {
				Object source = event.getSource();
				if (source instanceof IProcess) {
					ILaunch launch = ((IProcess) source).getLaunch();
					if (launch != null) {
						ILaunchConfiguration lc = launch.getLaunchConfiguration();
						if (lc != null && lc.equals(nodeDebugLaunch)) {
							try {
								// Debug session has just ended - removing server from session tracker
								SessionStorage.get().remove(server);
								server.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
							} finally {
								DebugPlugin.getDefault().removeDebugEventListener(this);
							}
						}
					}
				}
			}
		}
	}

}
