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

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

public class ExpressServer extends ServerDelegate implements IURLProvider {
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
		if( mods.length == 1 && add.length == 1 && add[0].equals(mods[0]))
			return Status.OK_STATUS;
		
		
		boolean canModify = mods.length == 0 && add.length == 1;
		canModify &= remove.length == 0;
		
		if(!canModify ) 
			return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, ExpressMessages.cannotModifyModules);
		
		// Make sure if there's a requried mod, the one being added matches it
		String requiredMod = getAttribute(ExpressServerUtils.ATTRIBUTE_APPLICATION_NAME, (String)null);
		if( requiredMod != null && !requiredMod.equals(add[0].getName())) {
			return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, 
					NLS.bind(ExpressMessages.additionNotRequiredModule, requiredMod));
		}
		return Status.OK_STATUS;
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

	@Override
	public URL getModuleRootURL(IModule module) {
		return JBossServer.getModuleRootURL(module, getServer().getHost(), 80);
	}
}
