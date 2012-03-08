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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.wizards.ConfigureProjectWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.archives.webtools.modules.LocalZippedPublisherUtil;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethod;
import org.jboss.ide.eclipse.as.core.server.IPublishCopyCallbackHandler;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServerBehavior;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;

public class ExpressPublishMethod implements IJBossServerPublishMethod {

	public ExpressPublishMethod() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void publishStart(DeployableServerBehavior behaviour,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public int publishFinish(DeployableServerBehavior behaviour,
			IProgressMonitor monitor) throws CoreException {
		
		String destProjName = ExpressServerUtils.getExpressDeployProject(behaviour.getServer());
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if( destProj != null ) {
			if( destProj.exists() ) {
				refreshProject(destProj, submon(monitor, 100));
				commitAndPushProject(destProj, behaviour, submon(monitor, 100));
			}
		}

        return areAllPublished(behaviour) ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;	
    }
	
	protected boolean areAllPublished(DeployableServerBehavior behaviour) {
        IModule[] modules = behaviour.getServer().getModules();
        boolean allpublished= true;
        for (int i = 0; i < modules.length; i++) {
        	if(behaviour.getServer().getModulePublishState(new IModule[]{modules[i]})!=IServer.PUBLISH_STATE_NONE)
                allpublished=false;
        }
        return allpublished;
	}

	@Override
	public int publishModule(DeployableServerBehavior behaviour, int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		
		// If this action is not user-initiated, bail!
		IAdaptable a = ((ExpressBehaviour)behaviour).getPublishAdaptableInfo();
		if( a == null )
			return -1;
		String s = (String)a.getAdapter(String.class);
		if( s == null || !s.equals("user"))
			return -1;
		
		if( module.length > 1 )
			return 0;
		
		// Magic Project
		String destProjName = ExpressServerUtils.getExpressDeployProject(behaviour.getServer());
		
		if( isInDestProjectTree(destProjName, module))
			return IServer.PUBLISH_STATE_NONE;
		
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		
		if( destProj.equals(module[module.length-1].getProject()))
			return 0;
		
		String destinationFolder = ExpressServerUtils.getExpressDeployFolder(behaviour.getServer());
		
		IContainer destFolder = "".equals(destinationFolder) ? destProj : (IContainer)destProj.findMember(new Path(destinationFolder));
		IPath dest = destFolder.getLocation();
		
		if( module.length == 0 ) return IServer.PUBLISH_STATE_NONE;
		int modulePublishState = behaviour.getServer().getModulePublishState(module);
		int publishType = behaviour.getPublishType(kind, deltaKind, modulePublishState);

		IModuleResourceDelta[] delta = new IModuleResourceDelta[]{};
		if( deltaKind != ServerBehaviourDelegate.REMOVED)
			delta = behaviour.getPublishedResourceDelta(module);

		try {
			LocalZippedPublisherUtil util = new LocalZippedPublisherUtil();
			IStatus status = util.publishModule(behaviour.getServer(), dest.toString(), module, publishType, delta, monitor);

			IPath path = util.getOutputFilePath(module); // where it's deployed
			IResource addedResource = destFolder.getFile(new Path(path.lastSegment()));
			IResource[] resource = new IResource[]{addedResource};
			final AddToIndexOperation operation = new AddToIndexOperation(resource);
			try {
				operation.execute(monitor);
			} catch (CoreException e) {
				// TODO
			}

		} catch( Exception e ) {
			e.printStackTrace();
		}
		return 0;
	}
	
	protected boolean isInDestProjectTree(String magicProject, IModule[] module) {
		IProject magic = magicProject == null ? null : 
			ResourcesPlugin.getWorkspace().getRoot().getProject(magicProject);
		IProject moduleProject = module == null ? null : module.length == 0 ? null : module[module.length-1].getProject();
		if( magic == null || moduleProject == null )
			return false;
		
		IPath moduleProjectRoot = moduleProject.getLocation();
		IPath magicProjectRoot = magic.getLocation();
		boolean ret = magicProjectRoot.isPrefixOf(moduleProjectRoot);
		return ret;
	}

	protected PushOperationResult commitAndPushProject(IProject p,
			DeployableServerBehavior behaviour, IProgressMonitor monitor) throws CoreException {
		Repository repository = EGitUtils.getRepository(p);

		int changed = EGitUtils.countCommitableChanges(p, behaviour.getServer(), new NullProgressMonitor() );
		String remoteName = behaviour.getServer().getAttribute(ExpressServerUtils.ATTRIBUTE_REMOTE_NAME, 
				ExpressServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT);
		PushOperationResult result = null;
		boolean committed = false;
		try {
			if( changed != 0 && requestCommitAndPushApproval(p.getName(), changed)) {
				monitor.beginTask("Publishing " + p.getName(), 300);
				EGitUtils.commit(p, new SubProgressMonitor(monitor, 100));
				committed = true;
			} 
			
			if( committed || (changed == 0 && requestPushApproval(p.getName()))) {
				if( !committed )
					monitor.beginTask("Publishing " + p.getName(), 200);
				result = EGitUtils.push(remoteName, EGitUtils.getRepository(p), new SubProgressMonitor(monitor, 100));
				monitor.done();
			}
		} catch(CoreException ce) {
			// Comes if either commit or push has failed
			if( ce.getMessage().contains("UP_TO_DATE"))
				return null;
			
			try {
				result = EGitUtils.pushForce(remoteName, repository, new SubProgressMonitor(monitor, 100));
				monitor.done();
			} catch(CoreException ce2) {
				// even the push force failed, and we don't have a valid result to check :( 
				// can only throw it i guess
				throw ce2;  
			}
		}
		
		if( result != null ) {
			ConsoleUtils.appendGitPushToConsole(behaviour.getServer(), result);
		}
		return result;
	}
	
	
	private void shareProjects(final IProject[] projects) {
		Display.getDefault().asyncExec(new Runnable() { 
			public void run() {
				String msg = ExpressMessages.bind(ExpressMessages.shareProjectMessage, projects.length);
				String title = ExpressMessages.shareProjectTitle;
				boolean approved = requestApproval(msg, title);
				if( approved ) {
					ConfigureProjectWizard.shareProjects(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
							projects);
				}
			}
		});
	}
	
	protected String getModuleProjectName(IModule[] module) {
		return module[module.length-1].getProject().getName();
	}
	
	protected boolean requestCommitAndPushApproval(String projName, int changed) {
		String msg = NLS.bind(ExpressMessages.requestCommitAndPushMsg, changed, projName);
		String title = NLS.bind(ExpressMessages.requestCommitAndPushTitle, projName);
		return requestApproval(msg, title);
	}

	protected boolean requestPushApproval(String projName) {
		String msg = NLS.bind(ExpressMessages.requestPushMsg, projName);
		String title = NLS.bind(ExpressMessages.requestPushTitle, projName);
		return requestApproval(msg, title);
	}

	protected boolean requestApproval(final String message, final String title) {
		final boolean[] b = new boolean[1];
		Display.getDefault().syncExec(new Runnable() { 
			public void run() {
		        b[0] = MessageDialog.openQuestion(getActiveShell(), title, message);
			}
		});
		return b[0];
	}
	
	protected static Shell getActiveShell() {
		Display display = Display.getDefault();
		final Shell[] ret = new Shell[1];
		display.syncExec(new Runnable() {
			public void run() {
				ret[0] = Display.getCurrent().getActiveShell();
			}
		});
		return ret[0];
	}
	
	@Override
	public IPublishCopyCallbackHandler getCallbackHandler(IPath path,
			IServer server) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPublishDefaultRootFolder(IServer server) {
		IDeployableServer s = ServerConverter.getDeployableServer(server);
		return s.getDeployFolder();
	}
	protected void refreshProject(final IProject project,IProgressMonitor monitor) throws CoreException {
		// Already inside a workspace scheduling rule
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}


    public static IProgressMonitor submon( final IProgressMonitor parent, final int ticks ) {
    	return submon( parent, ticks, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL );
    }
    public static IProgressMonitor submon( final IProgressMonitor parent,
            final int ticks, final int style ) {
    	return ( parent == null ? new NullProgressMonitor() : new SubProgressMonitor( parent, ticks, style ) );
    }

}
