/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

public class ExpressServer extends ServerDelegate {
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		setAttribute(IDeployableServer.SERVER_MODE, ExpressBehaviourDelegate.OPENSHIFT_ID);
	}

	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		// safety
		add = add == null ? new IModule[0] : add;
		remove = remove == null ? new IModule[0] : remove;
		
		// Can only add a module if the server has zero, and even then, can only add 1. 
		IModule[] mods = getServer().getModules();
		boolean canModify = mods.length == 0 && add.length == 1;
		canModify &= remove.length == 0;
		return canModify ? Status.OK_STATUS : new Status(IStatus.ERROR, 
				OpenShiftUIActivator.PLUGIN_ID, ExpressMessages.cannotModifyModules);
	}

    public IModule[] getRootModules(IModule module) throws CoreException {
        IStatus status = canModifyModules(new IModule[] { module }, null);
        if (status != null && !status.isOK())
            throw  new CoreException(status);
        IModule[] parents = ServerModelUtilities.getParentModules(getServer(), module);
        if(parents.length>0)
        	return parents;
        return new IModule[] { module };
    }

	public IModule[] getChildModules(IModule[] module) {
		return ServerModelUtilities.getChildModules(module);
	}
	
	public void modifyModules(IModule[] add, IModule[] remove,
			IProgressMonitor monitor) throws CoreException {
	}

}
