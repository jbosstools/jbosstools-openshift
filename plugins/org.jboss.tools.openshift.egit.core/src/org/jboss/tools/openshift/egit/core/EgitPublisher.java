/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethod;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublisher;

public class EgitPublisher implements IJBossServerPublisher {

	@Override
	public boolean accepts(String method, IServer server, IModule[] module) {
		return false;
	}

	@Override
	public int getPublishState() {
		return 0;
	}

	@Override
	public IStatus publishModule(IJBossServerPublishMethod method, IServer server, IModule[] module, int publishType,
			IModuleResourceDelta[] delta, IProgressMonitor monitor) throws CoreException {
		return null;
	}

}
