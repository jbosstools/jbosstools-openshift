package org.jboss.tools.openshift.js.server.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;
import org.jboss.tools.openshift.internal.js.storage.SessionStorage;

public class OpenhiftNodejsPublishController extends OpenShiftPublishController implements ISubsystemController {

	public OpenhiftNodejsPublishController() {
		super();
	}

	@Override
	public void publishStart(final IProgressMonitor monitor) throws CoreException {
		syncDownFailed = false;
		if (SessionStorage.get().containsKey(getServer())) {
			return;
		}
		super.publishStart(monitor);
	}

}
