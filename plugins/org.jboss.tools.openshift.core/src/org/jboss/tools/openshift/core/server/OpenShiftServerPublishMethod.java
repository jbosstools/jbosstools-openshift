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
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModule;
import org.jboss.ide.eclipse.as.core.modules.ResourceModuleResourceUtil;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilter;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilterProvider;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.LocalFilesystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.util.PublishControllerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.publish.LocalZippedModulePublishRunner;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;
import org.jboss.tools.runtime.core.extract.ExtractUtility;
import org.jboss.tools.runtime.core.extract.IOverwrite;

import com.openshift.restclient.authorization.ResourceForbiddenException;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerPublishMethod  {

	private RSync rsync = null;
	public void publishStart(final IServer server, final IProgressMonitor monitor) 
			throws CoreException {
		IProject deployProject = getMagicProject(server);
		if (!ProjectUtils.isAccessible(deployProject)) {
			throw new CoreException(new Status(IStatus.ERROR,
					OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Server adapter {0} cannot publish. Required project {1} is missing or inaccessible.", 
							server.getName(), deployProject.getName())));
		}
		rsync = createRSync(server, monitor);
		File localDeploymentDirectory = getLocalDeploymentDirectory(server);
		MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
				NLS.bind("Could not sync all pods to folder {0}", localDeploymentDirectory.getAbsolutePath()), null);
		rsync.syncPodsToDirectory(localDeploymentDirectory, status);
		
		// If the magic project is *also* a module on the server, do nothing
		if( !modulesIncludesMagicProject(server, deployProject)) {
			// The project not also a module, so let's see if there exists a module at all
			IModule projectModule = findProjectModule(deployProject);
			if( projectModule == null ) { 
				// This project is not a module, so we'll do a simple copy
				publishMagicProjectSimpleCopy(server, localDeploymentDirectory);
			} else {
				// This is a project-module which must be assembled and published (ie dynamic web, ear project, etc)
				publishModule(server, new IModule[]{projectModule}, PublishControllerUtil.FULL_PUBLISH, monitor);
			}
		}
	}
	
	private IModule findProjectModule(IServer s) {
		IProject deployProject = getMagicProject(s);
		return findProjectModule(deployProject);
	}
	
	private IModule findProjectModule(IProject p) {
		IModule[] all = org.eclipse.wst.server.core.ServerUtil.getModules(p);
		for( int i = 0; i < all.length; i++ ) {
			ModuleDelegate md = (ModuleDelegate)all[i].loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
			if( md instanceof ProjectModule && !(md instanceof org.eclipse.jst.j2ee.internal.deployables.BinaryFileModuleDelegate)) {
				return all[i];
			}
		}
		return null;
	}

	public static IPath getModuleNestedDeployPath(IModule[] moduleTree, String rootFolder, IServer server) {
		return new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(moduleTree, rootFolder, server);
	}
	
	private LocalZippedModulePublishRunner createZippedRunner(IServer server, IModule m, IPath p) {
		return new LocalZippedModulePublishRunner(server, m,p, getModulePathFilterProvider());
	}

	private IModulePathFilterProvider getModulePathFilterProvider() {
		return new IModulePathFilterProvider() {
			public IModulePathFilter getFilter(IServer server, IModule[] module) {
				return ResourceModuleResourceUtil.findDefaultModuleFilter(module[module.length-1]);
			}
		};
	}
	private void publishMagicProjectSimpleCopy(IServer server, File localDeploymentDirectory) throws CoreException {
		// TODO this is the dumb logic. If the magic project is in fact a 
		// dynamic web project, we need to package it, not simply copy. 
		String sourcePath = OpenShiftServerUtils.getSourcePath(server);
		if (StringUtils.isEmpty(sourcePath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the source to publish.", server.getName())));
		}
		File source = new File(sourcePath);
		FileUtils.copyDir(source, localDeploymentDirectory, true, true, true, new FileFilter() {

			@Override
			public boolean accept(File file) {
				String filename = file.getName();
				return !filename.endsWith(".git")
						&& !filename.endsWith(".gitignore")
						&& !filename.endsWith(".svn")
						&& !filename.endsWith(".settings")
						&& !filename.endsWith(".project")
						&& !filename.endsWith(".classpath");
			}
		});
	}

	private boolean modulesIncludesMagicProject(IServer server, IProject deployProject) {
		// If we have no modules, OR the module list doesn't include magic project, 
		// we need to initiate a publish for the magic project
		IModule[] all = server.getModules();
		if( all != null ) {
			for( int i = 0; i < all.length; i++ ) {
				if( all[i].getProject().equals(deployProject))
					return true;
			}
		}
		return false;
	}
	
	private IProject getMagicProject(IServer server) {
		String deployProjectName = OpenShiftServerUtils.getDeployProjectName(server);
		return ProjectUtils.getProject(deployProjectName);
	}
	
	private RSync createRSync(IServer server, IProgressMonitor monitor) throws CoreException {
		String location = OCBinary.getInstance().getLocation();
		if( location == null ) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Binary for oc-tools could not be found. Please open the OpenShift 3 Preference Page and set the location of the oc binary."));
		}
		
		
		IService service = null;
		try {
			service = OpenShiftServerUtils.getService(server);
		} catch(ResourceForbiddenException rfe) {
			// ignore
		}
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the service to publish to.", server.getName())));
		}

		String podPath = OpenShiftServerUtils.getPodPath(server);
		if (StringUtils.isEmpty(podPath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the destination directory to publish to.", server.getName())));
		}
		
		return new RSync(service, podPath, server);
	}

	public int publishFinish(IServer server, IProgressMonitor monitor) throws CoreException {
		try {
			if( rsync != null ) {
				File deployFolder = getLocalDeploymentDirectory(server);
				IService service = OpenShiftServerUtils.getService(server);
				MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
						NLS.bind("Could not sync {0} to all pods running the service {1}", deployFolder, service.getName()), null);
				rsync.syncDirectoryToPods(deployFolder, status);
				
				// Remove all *.dodeploy files from this folder. 
				Stream.of(deployFolder.listFiles())
						.filter(p->p.getName().endsWith(".dodeploy"))
						.forEach(p->p.delete());
				return status.isOK() ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;
			}
			return IServer.PUBLISH_INCREMENTAL;
		} finally {
			rsync = null;
		}
	}
	

	public int publishModule(IServer server, int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		
		if( module.length > 1) {
			return IServer.PUBLISH_STATE_NONE;
		}
		
		int publishType = PublishControllerUtil.getPublishType(server, module, kind, deltaKind);
		if( publishType != PublishControllerUtil.NO_PUBLISH) {
			if( publishType == PublishControllerUtil.REMOVE_PUBLISH) {
				return removeModule(server, module, monitor);
			}
			return publishModule(server, module, publishType, monitor);
		} else {
			return IServer.PUBLISH_STATE_NONE;
		}
	}
	
	protected int removeModule(IServer server, IModule[] module, IProgressMonitor monitor) throws CoreException {
		IModule projectModule = findProjectModule(server);
		File localDeploymentDirectory = getLocalDeploymentDirectory(server);
		IPath outputFileFullPath = new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(
				module, localDeploymentDirectory.getAbsolutePath(), server);
		if( module[0].equals(projectModule)) {
			String suffix = ServerModelUtilities.getDefaultSuffixForModule(module[0]);
			if( new File(localDeploymentDirectory, "ROOT" + suffix).exists()) {
				// Magic project gets a 'root.ear' or 'root.war' name, for now, if it exists. 
				outputFileFullPath = new Path(localDeploymentDirectory.getAbsolutePath()).append("ROOT" + suffix);
			}
		}
		IStatus s = new LocalFilesystemController().deleteResource(outputFileFullPath, monitor);
		return s.isOK() ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_UNKNOWN;
	}
	
	protected int publishModule(IServer server, IModule[] module, int publishType, IProgressMonitor monitor) throws CoreException {
		File localDeploymentDirectory = getLocalDeploymentDirectory(server);
		File tempDeploymentDirectory = getLocalTempDeploymentDirectory(server);
		IModule projectModule = findProjectModule(server);
		// Default to normal locations
		IPath outputFileFullPath = new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(
				module, tempDeploymentDirectory.getAbsolutePath(), server);
		IPath unzipLocation =  new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(
				module, localDeploymentDirectory.getAbsolutePath(), server);

		if( module[0].equals(projectModule)) {
			String suffix = ServerModelUtilities.getDefaultSuffixForModule(module[0]);
			if( new File(localDeploymentDirectory, "ROOT" + suffix).exists()) {
				// Magic project gets a 'root.ear' or 'root.war' name, for now, if it exists. 
				// TODO is this guaranteed path?  doesn't seem like it. What if it's an ear? 
				outputFileFullPath = new Path(tempDeploymentDirectory.getAbsolutePath()).append("ROOT" + suffix);
				unzipLocation = new Path(localDeploymentDirectory.getAbsolutePath()).append("ROOT" + suffix);
			}
		}
		
		
		packageAndUnzip(server, module[0], outputFileFullPath, unzipLocation, monitor );
		
		
		// A hack only valid for eap pods. 
		if( publishType == PublishControllerUtil.FULL_PUBLISH) {
			IPath marker = unzipLocation.removeLastSegments(1).append(unzipLocation.lastSegment() + ".dodeploy");
			try {
				marker.toFile().createNewFile();
			} catch(IOException ioe) {
			}
		}
		return IServer.PUBLISH_STATE_NONE;
	}
	
	private int packageAndUnzip(IServer server, IModule module, IPath outputFileFullPath, IPath unzipLocation, IProgressMonitor monitor)
			throws CoreException {
		LocalZippedModulePublishRunner runner = createZippedRunner(server, module, outputFileFullPath);
		monitor.beginTask("Moving module to " + unzipLocation.lastSegment(), 100);
		runner.fullPublishModule(SubMonitor.convert(monitor, 20));
		
		if( unzipLocation.toFile().exists() && unzipLocation.toFile().isFile()) {
			unzipLocation.toFile().delete();
		}
		IOverwrite yes = new IOverwrite() {
			public int overwrite(File file) {
				return IOverwrite.ALL;
			}
		};
		new ExtractUtility(outputFileFullPath.toFile(), ExtractUtility.FORMAT_ZIP).extract(unzipLocation.toFile(), yes, monitor);
		return IServer.PUBLISH_STATE_NONE;
	}	

	private File getLocalDeploymentDirectory(IServer server) {
		return ServerUtil.getServerStateLocation(server).append("deploy").toFile();
	}
	private File getLocalTempDeploymentDirectory(IServer server) {
		return ServerUtil.getServerStateLocation(server).append("tempDeploy").toFile();
	}

}
