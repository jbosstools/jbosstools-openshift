/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.internal.util.ComponentUtilities;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
class ServerAdapterFactory {

	public ServerAdapterFactory() {
	}

	public void create(IProject project, IServerType serverType, IRuntime runtime, String mode,
			IApplication application, IUser user, IProgressMonitor monitor) throws OpenShiftException {
		createServerAdapter(project, serverType, runtime, mode, application, user, monitor);
	}

	/**
	 * creates an OpenShift server adapter for the user chosen project.
	 * 
	 * @param monitor
	 *            the monitor to report progress to.
	 * @throws OpenShiftException
	 */
	protected void createServerAdapter(IProject project, IServerType serverType, IRuntime runtime, String mode,
			IApplication application, IUser user, IProgressMonitor monitor) throws OpenShiftException {
		String name = project.getName();
		monitor.subTask(NLS.bind("Creating server adapter for project {0}", name));
		createServerAdapter(Collections.singletonList(project), serverType, runtime, mode, application, user,
				monitor);
	}

	protected void createServerAdapter(List<IProject> importedProjects, IServerType serverType,
			IRuntime runtime, String mode, IApplication application, IUser user, IProgressMonitor monitor) {
		try {
			renameWebContextRoot(importedProjects);
			IServer server = doCreateServerAdapter(serverType, runtime, mode, application, user);
			addModules(getModules(importedProjects), server, monitor);
		} catch (CoreException ce) {
			OpenShiftUIActivator.getDefault().getLog().log(ce.getStatus());
		} catch (OpenShiftException ose) {
			IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Cannot create openshift server adapter", ose);
			OpenShiftUIActivator.getDefault().getLog().log(s);
		}
	}

	private void renameWebContextRoot(List<IProject> importedProjects) {
		for (IProject project : importedProjects) {
			ComponentUtilities.setServerContextRoot(project, "/");
		}
	}

	private IServer doCreateServerAdapter(IServerType serverType, IRuntime rt, String mode, IApplication application,
			IUser user) throws CoreException,
			OpenShiftException {
		Assert.isLegal(serverType != null);
		Assert.isLegal(mode != null);
		Assert.isLegal(application != null);
		Assert.isLegal(user != null);

		String serverNameBase = application.getName() + " OpenShift Server";
		String serverName = org.jboss.ide.eclipse.as.core.util.ServerUtil.getDefaultServerName(serverNameBase);

		IServer server = ExpressServerUtils.createServer(rt, serverType, serverName);
		ExpressServerUtils.fillServerWithOpenShiftDetails(server, application.getApplicationUrl(),
				user.getRhlogin(), user.getPassword(),
				user.getDomain().getNamespace(), application.getName(), application.getUUID(), mode);
		return server;
	}

	private void addModules(List<IModule> modules, IServer server, IProgressMonitor monitor) throws CoreException {
		if (modules == null
				|| modules.size() == 0) {
			return;
		}
		IServerWorkingCopy wc = server.createWorkingCopy();
		IModule[] add = modules.toArray(new IModule[modules.size()]);
		wc.modifyModules(add, new IModule[0], new NullProgressMonitor());
		server = wc.save(true, monitor);
		((Server) server).setModulePublishState(add, IServer.PUBLISH_STATE_NONE);
	}

	private List<IModule> getModules(List<IProject> importedProjects) {
		Iterator<IProject> i = importedProjects.iterator();
		ArrayList<IModule> toAdd = new ArrayList<IModule>();
		while (i.hasNext()) {
			IProject p = i.next();
			IModule[] m = ServerUtil.getModules(p);
			if (m != null && m.length > 0) {
				toAdd.addAll(Arrays.asList(m));
			}
		}
		return toAdd;
	}
}
