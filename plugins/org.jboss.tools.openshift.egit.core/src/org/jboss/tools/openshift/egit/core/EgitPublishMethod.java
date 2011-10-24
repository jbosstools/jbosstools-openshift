package org.jboss.tools.openshift.egit.core;


public class EgitPublishMethod { /*
	implements IJBossServerPublishMethod {

	public String getPublishMethodId() {
		return EgitBehaviourDelegate.ID;
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
		// TODO Auto-generated method stub
		IProject project = module[0].getProject();
		EGitUtils.commit(project, monitor);
		EGitUtils.push(EGitUtils.getRepository(project), monitor);
		return IServer.PUBLISH_STATE_NONE;
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
  */
}
