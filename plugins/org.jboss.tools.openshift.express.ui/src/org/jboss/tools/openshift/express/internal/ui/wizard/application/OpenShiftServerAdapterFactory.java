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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.net.SocketTimeoutException;
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
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Rob Stryker
 * @author André Dietisheim
 */
public class OpenShiftServerAdapterFactory {

	OpenShiftServerAdapterFactory() {
	}

	public IServer create(IProject project, IOpenShiftWizardModel wizardModel, IProgressMonitor monitor) throws OpenShiftException {
		return createAdapterAndModules(project, wizardModel.getServerType(), wizardModel.getRuntime(),  
				wizardModel.getApplication(), wizardModel.getDomain(), wizardModel.getRemoteName(), monitor);
	}

	public IServer create(IProject project, IServerType serverType, IRuntime runtime,
			IApplication application, IDomain domain, IProgressMonitor monitor) throws OpenShiftException {
		return createAdapterAndModules(project, serverType, runtime, application, domain, null, monitor);
	}

	/**
	 * creates an OpenShift server adapter for the user chosen project.
	 * 
	 * @param monitor
	 *            the monitor to report progress to.
	 * @return 
	 * @throws OpenShiftException
	 */
	protected IServer createAdapterAndModules(IProject project, IServerType serverType, IRuntime runtime,
			IApplication application, IDomain domain, String remoteName, IProgressMonitor monitor)
			throws OpenShiftException {
		monitor.subTask(NLS.bind("Creating server adapter for project {0}", project.getName()));
		
		IServer server = null;
		try {
			server = createAdapter(serverType, runtime, application, domain, project.getName(), remoteName);
			server = addModules(getModules(Collections.singletonList(project)), server, monitor);
		} catch (CoreException ce) {
			OpenShiftUIActivator.getDefault().getLog().log(ce.getStatus());
		} catch (OpenShiftException ose) {
			IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Cannot create openshift server adapter", ose);
			OpenShiftUIActivator.getDefault().getLog().log(s);
		} catch (SocketTimeoutException ste) {
			IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Cannot create openshift server adapter", ste);
			OpenShiftUIActivator.getDefault().getLog().log(s);
		}
		return server;
	}
	
	private IServer createAdapter(IServerType serverType, IRuntime rt,
			IApplication application, IDomain domain, String deployProject, String remoteName) throws CoreException,
			OpenShiftException, SocketTimeoutException {
		Assert.isLegal(serverType != null);
		Assert.isLegal(application != null);

		String serverName = OpenShiftServerUtils.getDefaultServerName(application);
		IServer server = OpenShiftServerUtils.createServer(rt, serverType, serverName);
		OpenShiftServerUtils.fillServerWithOpenShiftDetails(
				server, deployProject, remoteName, serverName, application, domain);
		return server;
	}
	
	public void addModules(IServer server, List<IProject> importedProjects, IProgressMonitor monitor) throws CoreException {
		addModules(getModules(importedProjects), server, monitor);
	}

	private IServer addModules(List<IModule> modules, IServer server, IProgressMonitor monitor) throws CoreException {
		if (modules == null
				|| modules.size() == 0) {
			return server;
		}
		IServerWorkingCopy wc = server.createWorkingCopy();
		IModule[] add = modules.toArray(new IModule[modules.size()]);
		wc.modifyModules(add, new IModule[0], new NullProgressMonitor());
		server = wc.save(true, monitor);
		((Server) server).setModulePublishState(add, IServer.PUBLISH_STATE_NONE);
		return server;
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
