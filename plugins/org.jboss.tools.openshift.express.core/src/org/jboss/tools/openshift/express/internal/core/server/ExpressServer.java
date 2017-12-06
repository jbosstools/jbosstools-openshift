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
package org.jboss.tools.openshift.express.internal.core.server;

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServer;
import org.jboss.ide.eclipse.as.core.server.internal.IExtendedPropertiesProvider;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;

/**
 * @author Rob Stryker
 */
public class ExpressServer extends DeployableServer implements IURLProvider, IExtendedPropertiesProvider {

	public static final String DEFAULT_SERVER_NAME_BASE = "ApplicationName";
	public static final String OPENSHIFT_MODE_ID = "openshift";

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		getServerWorkingCopy().setHost(UrlUtils.cutScheme(ExpressConnectionUtils.getDefaultHostUrl()));
		getServerWorkingCopy()
				.setName(ServerUtils.getServerName(DEFAULT_SERVER_NAME_BASE + ExpressServerUtils.AT_OPENSHIFT_2));
		setAttribute(IDeployableServer.SERVER_MODE, OPENSHIFT_MODE_ID);
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		return Status.OK_STATUS;
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		IStatus status = canModifyModules(new IModule[] { module }, null);
		if (status != null && !status.isOK())
			throw new CoreException(status);
		IModule[] parents = ServerModelUtilities.getParentModules(getServer(), module);
		if (parents.length > 0)
			return parents;
		return new IModule[] { module };
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		return ServerModelUtilities.getChildModules(module);
	}

	@Override
	public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public URL getModuleRootURL(IModule module) {
		String appProjString = ExpressServerUtils.getDeployProjectName(getServer());
		IProject appProj = appProjString == null ? null
				: ResourcesPlugin.getWorkspace().getRoot().getProject(appProjString);
		IProject p = module.getProject();
		boolean shouldIgnore = ExpressServerUtils.getIgnoresContextRoot(getServer()) && p.equals(appProj);
		return JBossServer.getModuleRootURL(module, getServer().getHost(), 80, shouldIgnore ? "" : null);
	}

	@Override
	public ServerExtendedProperties getExtendedProperties() {
		return new ExpressServerExtendedProperties(getServer());
	}

	@Override
	public boolean isUseProjectSpecificSchedulingRuleOnPublish() {
		// ensure we're locking the whole workspace while publishing.
		return false;
	}
}
