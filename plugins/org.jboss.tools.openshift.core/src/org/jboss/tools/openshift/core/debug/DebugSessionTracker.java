/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.wst.server.core.IServer;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public interface DebugSessionTracker {

	void startDebugSession(IServer server, int port) throws CoreException;

	void stopDebugSession(IServer server) throws DebugException;

	boolean isDebugSessionAlive(IServer server);

}
