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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.wizards.ConfigureProjectWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethod;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServerBehavior;
import org.jboss.ide.eclipse.as.core.server.xpl.PublishCopyUtil.IPublishCopyCallbackHandler;
import org.jboss.tools.openshift.egit.core.EGitUtils;

public class ExpressPublishMethod implements IJBossServerPublishMethod {

	private ArrayList<IProject> projectsLackingGitRepo = null;

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
		if( projectsLackingGitRepo != null ) {
			IProject[] projects = (IProject[]) projectsLackingGitRepo.toArray(new IProject[projectsLackingGitRepo.size()]);
			shareProjects(projects);
			projectsLackingGitRepo = null;
		}
        IModule[] modules = behaviour.getServer().getModules();
        boolean allpublished= true;
        for (int i = 0; i < modules.length; i++) {
        	if(behaviour.getServer().getModulePublishState(new IModule[]{modules[i]})!=IServer.PUBLISH_STATE_NONE)
                allpublished=false;
        }
        return allpublished ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;	
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
		
		
		int state = behaviour.getServer().getModulePublishState(module);
		IProject p = module[module.length-1].getProject();
		
		if( deltaKind == ServerBehaviourDelegate.REMOVED)
			return IServer.PUBLISH_STATE_NONE;  // go ahead and remove it
		
		Repository repository = EGitUtils.getRepository(p);
		if (repository==null) {
			if( projectsLackingGitRepo == null )
				projectsLackingGitRepo = new ArrayList<IProject>();
			projectsLackingGitRepo.add(p);
			return IServer.PUBLISH_STATE_UNKNOWN;
		}
		
		int changed = EGitUtils.countCommitableChanges(p, new NullProgressMonitor() );
		if( changed != 0 && requestCommitAndPushApproval(module, changed)) {
			monitor.beginTask("Publishing " + p.getName(), 200);
			EGitUtils.commit(p, new SubProgressMonitor(monitor, 100));
			EGitUtils.push(EGitUtils.getRepository(p), new SubProgressMonitor(monitor, 100));
			monitor.done();
			return IServer.PUBLISH_STATE_NONE;
		} else if( changed == 0 && requestPushApproval(module)) {
			monitor.beginTask("Publishing " + p.getName(), 100);
// TODO: use the following instead of the current:
//			EGitUtils.push("<remote name configured in AdapterWizardPageModel#getRemoteName>"
//					, EGitUtils.getRepository(p)
//					, new SubProgressMonitor(monitor, 100));
			EGitUtils.push(EGitUtils.getRepository(p), new SubProgressMonitor(monitor, 100));
			monitor.done();
			return IServer.PUBLISH_STATE_NONE;
		}
		return state;
	}

	private void shareProjects(final IProject[] projects) {
		Display.getDefault().asyncExec(new Runnable() { 
			public void run() {
				String msg = "There are " + projects.length + " projects that are not connected to any git repository. " +
						"Would you like to share them now?";
				String title = "Share projects?";
				boolean approved = requestApproval(msg, title);
				if( approved ) {
					ConfigureProjectWizard.shareProjects(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
							projects);
				}
			}
		});
	}
	
	private boolean requestCommitAndPushApproval(final IModule[] module, int changed) {
		String projName = module[module.length-1].getProject().getName();
		String msg = "There are " + changed + " local changes in \"" + projName + "\". " +
				"Do you want to publish to OpenShift by commiting the changes and pushing its Git repository?";
		String title = "Publish " + projName + "?";
		return requestApproval(msg, title);
	}

	private boolean requestPushApproval(final IModule[] module) {
		String projName = module[module.length-1].getProject().getName();
		String msg = "The are no local changes in \"" + projName + "\". " +
				"Do you want to publish to OpenShift by pushing its Git repository?";
		String title = "Publish " + projName + "?";
		return requestApproval(msg, title);
	}

	private boolean requestApproval(final String message, final String title) {
		final boolean[] b = new boolean[1];
		Display.getDefault().syncExec(new Runnable() { 
			public void run() {
		        b[0] = MessageDialog.openQuestion(getActiveShell(), title, message);
			}
		});
		return b[0];
	}
	
	private static Shell getActiveShell() {
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

	@Override
	public String getPublishDefaultRootFolder(IServer server) {
		// TODO Auto-generated method stub
		return null;
	}

}
