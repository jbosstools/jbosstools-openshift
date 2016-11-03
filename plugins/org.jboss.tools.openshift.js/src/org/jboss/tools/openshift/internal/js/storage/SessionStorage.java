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
package org.jboss.tools.openshift.internal.js.storage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

/**
 * Contains {@link Set} of {@link IServer} with Node.js Debugger attached.
 * {@link OpenShiftPublishController} checks if {@link IServer} is tracked by
 * {@link SessionStorage} in order to verify if 'rsync' is required. If
 * there is a Node.js debug session associated with {@link IServer} 'rsync' must
 * not be performed due to the fact that 'rsync' will cause Node.js app restart
 * and debug state will be lost
 * 
 * @author "Ilya Buziuk (ibuziuk)"
 */
public final class SessionStorage {

	private SessionStorage() {
	}

	private static final Set<IServer> INSTANCE = Collections.synchronizedSet(new HashSet<>());

	public static Set<IServer> get() {
		return INSTANCE;
	}

}
