package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethod;
import org.jboss.ide.eclipse.as.core.server.internal.DeployableServerBehavior;
import org.jboss.ide.eclipse.as.core.server.xpl.PublishCopyUtil.IPublishCopyCallbackHandler;
import org.jboss.ide.eclipse.as.egit.core.EGitUtils;

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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int publishModule(DeployableServerBehavior behaviour, int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		int state = behaviour.getServer().getModulePublishState(module);
		IProject p = module[module.length-1].getProject();
		int changed = EGitUtils.countCommitableChanges(p, new NullProgressMonitor() );
		if( changed == 0 || (kind == IServer.PUBLISH_FULL || state == IServer.PUBLISH_STATE_FULL)) {
			if( changed != 0 && requestCommitAndPushApproval(module, changed)) {
				monitor.beginTask("Publishing " + p.getName(), 200);
				EGitUtils.commit(p, new SubProgressMonitor(monitor, 100));
				EGitUtils.push(EGitUtils.getRepository(p), new SubProgressMonitor(monitor, 100));
				monitor.done();
				return IServer.PUBLISH_STATE_NONE;
			} else if( changed == 0 && requestPushApproval(module)) {
				monitor.beginTask("Publishing " + p.getName(), 100);
				EGitUtils.push(EGitUtils.getRepository(p), new SubProgressMonitor(monitor, 100));
				monitor.done();
				return IServer.PUBLISH_STATE_NONE;
			}
		}
		return IServer.PUBLISH_STATE_INCREMENTAL;
	}

	private boolean requestCommitAndPushApproval(final IModule[] module, int changed) {
		String projName = module[module.length-1].getProject().getName();
		String msg = "There are " + changed + " local changes in \"" + projName + "\". " +
				"Do you want to publish to OpenShift by commiting the changes and pushing its Git repository?";
		String title = "Publish " + projName + "?";
		return requestApproval(module, msg, title);
	}

	private boolean requestPushApproval(final IModule[] module) {
		String projName = module[module.length-1].getProject().getName();
		String msg = "The are no local changes in \"" + projName + "\". " +
				"Do you want to publish to OpenShift by pushing its Git repository?";
		String title = "Publish " + projName + "?";
		return requestApproval(module, msg, title);
	}

	private boolean requestApproval(final IModule[] module, final String message, final String title) {
		final boolean[] b = new boolean[1];
		Display.getDefault().syncExec(new Runnable() { 
			public void run() {
				MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		        messageBox.setMessage(message);
		        messageBox.setText(title);
		        int response = messageBox.open();
		        if (response == SWT.YES)
		        	b[0] = true;
		        b[1] = false;
			}
		});
		return b[0];
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
