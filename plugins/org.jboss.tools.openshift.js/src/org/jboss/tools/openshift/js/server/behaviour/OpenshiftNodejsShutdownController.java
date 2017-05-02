package org.jboss.tools.openshift.js.server.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftShutdownController;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

public class OpenshiftNodejsShutdownController extends OpenShiftShutdownController implements ISubsystemController {

	public OpenshiftNodejsShutdownController() {
		super();
	}
	
	@Override
	public void stop(boolean force) {
		OpenShiftServerBehaviour behavior = getBehavior();
		behavior.setServerStopping();
		try {
			NodeDebugLauncher.terminate(behavior.getServer());
			behavior.setServerStopped();
		} catch(CoreException ce) {
			log(IStatus.ERROR, "Error shutting down server", ce);
			getBehavior().setServerStarted();
		}
	}

}
