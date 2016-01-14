/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServer;
import org.jboss.ide.eclipse.as.core.server.internal.IExtendedPropertiesProvider;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServer extends DeployableServer implements IURLProvider, IExtendedPropertiesProvider {

	public static final String OPENSHIFT3_MODE_ID = "openshift3";

	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		setAttribute(Server.PROP_AUTO_PUBLISH_SETTING, Server.AUTO_PUBLISH_RESOURCE);
	}
	
	@Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        IStatus status = canModifyModules(new IModule[] { module }, null);
        if (status != null && !status.isOK())
            throw  new CoreException(status);
        IModule[] parents = ServerModelUtilities.getParentModules(getServer(), module);
        if(parents.length>0)
        	return parents;
        return new IModule[] { module };
    }

	@Override
	public URL getModuleRootURL(IModule module) {
		IProject moduleProject = module.getProject();
		String deployProjectName = OpenShiftServerUtils.getDeployProjectName(getServer());
		IProject deployProject = ProjectUtils.getProject(deployProjectName);
		String contextRoot = getContextRoot(moduleProject, deployProject);
		return JBossServer.getModuleRootURL(module, getServer().getHost(), 80, contextRoot);
	}

	private String getContextRoot(IProject moduleProject, IProject deployProject) {
		String contextRoot = null;
		if (OpenShiftServerUtils.isIgnoresContextRoot(getServer()) 
				&& moduleProject.equals(deployProject)) {
			contextRoot = "";
		}
		return contextRoot;
	}
	
	@Override
	public ServerExtendedProperties getExtendedProperties() {
		return new OpenShiftServerExtendedProperties(getServer());
	}

	@Override
	public boolean isUseProjectSpecificSchedulingRuleOnPublish() {
		// ensure we're locking the whole workspace while publishing.
		return false;
	}
}
