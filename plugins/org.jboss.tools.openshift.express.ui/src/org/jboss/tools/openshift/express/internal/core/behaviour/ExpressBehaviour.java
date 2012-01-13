/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.jboss.ide.eclipse.as.core.server.internal.DelegatingServerBehavior;

public class ExpressBehaviour extends DelegatingServerBehavior {
	private IAdaptable publishAdaptableInfo;
	public void publish(int kind, List<IModule[]> modules, IProgressMonitor monitor, IAdaptable info) throws CoreException {
		publishAdaptableInfo = info;
		try {
			super.publish(kind, modules, monitor, info);
		} finally {
			publishAdaptableInfo = null;
		}
	}
	
	public IAdaptable getPublishAdaptableInfo() {
		return publishAdaptableInfo;
	}
	
	@Override
	public boolean canRestartModule(IModule[] module){
		return false;
	}

}
