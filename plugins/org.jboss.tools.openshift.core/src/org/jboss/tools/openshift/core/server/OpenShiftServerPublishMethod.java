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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IRSyncable;
import com.openshift.restclient.capability.resources.IRSyncable.LocalPeer;
import com.openshift.restclient.capability.resources.IRSyncable.PodPeer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerPublishMethod  {

	public void publishStart(final IServer server, final IProgressMonitor monitor) 
			throws CoreException {
		String deployProjectName = OpenShiftServerUtils.getDeployProjectName(server);
		IProject deployProject = ProjectUtils.getProject(deployProjectName);
		if (!ProjectUtils.isAccessible(deployProject)) {
			throw new CoreException(new Status(IStatus.ERROR,
					OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Server adapter {0} cannot publish. Required project {1} is missing or inaccessible.", 
							server.getName(), deployProjectName)));
		}
	}

	public int publishFinish(IServer server, IProgressMonitor monitor) throws CoreException {
		IService service = OpenShiftServerUtils.getService(server);
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the service to publish to.", server.getName())));
		}

		String podPath = OpenShiftServerUtils.getPodPath(server);
		IProject project = OpenShiftServerUtils.getDeployProject(server);
		MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
				NLS.bind("Could not sync project {0} to all pods running the service {1}", project.getName(), service.getName()), null);
		new RSync(project, service, podPath, server).run(status);

//		boolean allSubModulesPublished = areAllModulesPublished(server);
//
//		if (ProjectUtils.exists(project)) {
//			IContainer deployFolder = ServerUtil.getContainer(ExpressServerUtils.getDeployFolder(server), project);
//			if (allSubModulesPublished
//					|| (deployFolder != null && deployFolder.isAccessible())) {
//				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
//				publish(project, server, monitor);
//			} // else ignore. (one or more modules not published AND magic
//				// folder doesn't exist
//				// The previous exception will be propagated.
//		}
//		
//		return allSubModulesPublished ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;
		return IServer.PUBLISH_STATE_NONE;
	}

	public int publishModule(IServer server, int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		return IServer.PUBLISH_STATE_NONE;
	}
	
	private static class RSync extends OCBinaryOperation {

		private IProject project;
		private IService service;
		private String podPath;
		private IServer server;

		public RSync(IProject project, IService service, String podPath, IServer server) {
			this.project = project;
			this.service = service;
			this.podPath = podPath;
			this.server = server;
		}

		@Override
		protected void runOCBinary(MultiStatus status) {
			for (IPod pod : service.getPods()) {
				try {
					sync(project, pod, podPath, server);
				} catch (IOException | OpenShiftException e) {
					status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
				}
			}
		}

		private void sync(IProject project, IPod pod, String podPath, IServer server) throws IOException {
			File tmpCopy = createFilteredCopy(project, server);
			String sourcePath = tmpCopy.getAbsolutePath() + "/.";
			pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {

				@Override
				public IRSyncable visit(IRSyncable rsyncable) {
					rsyncable.sync(new LocalPeer(sourcePath), new PodPeer(podPath, pod));
					return rsyncable;
				}
			}, null);
		}

		private File createFilteredCopy(IProject project, IServer server) throws IOException {
			File source = project.getLocation().toFile();
			File destination = ServerUtil.getServerStateLocation(server).toFile();
			
			FileUtils.copyDir(source, destination, true, true, true, new FileFilter() {

				@Override
				public boolean accept(File file) {
					String filename = file.getName();
					return !filename.endsWith(".git")
							&& !filename.endsWith(".gitignore")
							&& !filename.endsWith(".svn")
							&& !filename.endsWith(".settings")
							&& !filename.endsWith(".project")
							&& !filename.endsWith(".classpath");
				}});
			return destination;
		}
	}
}
