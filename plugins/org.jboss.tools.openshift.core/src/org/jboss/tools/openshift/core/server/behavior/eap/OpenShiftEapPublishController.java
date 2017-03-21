package org.jboss.tools.openshift.core.server.behavior.eap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

public class OpenShiftEapPublishController extends OpenShiftPublishController implements ISubsystemController {

	public OpenShiftEapPublishController() {
		super();
	}
	
	@Override
	public int publishModule(int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		return super.publishModule(IServer.PUBLISH_CLEAN, deltaKind, module, monitor);
	}

}
