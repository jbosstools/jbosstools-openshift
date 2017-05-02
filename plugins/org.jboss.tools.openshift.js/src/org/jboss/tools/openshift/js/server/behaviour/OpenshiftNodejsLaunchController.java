package org.jboss.tools.openshift.js.server.behaviour;

import static org.jboss.tools.openshift.core.server.OpenShiftServerUtils.toCoreException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.server.debug.DebuggingContext;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugUtils;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IReplicationController;

public class OpenshiftNodejsLaunchController extends OpenShiftLaunchController implements ISubsystemController {
	
	private static final String DEV_MODE = "DEV_MODE"; //$NON-NLS-1$
	private static final int DEFAULT_DEBUG_PORT = 5858;

	public OpenshiftNodejsLaunchController() {
		super();
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		OpenShiftServerBehaviour beh = launchServerProcessAndWaitForResources(configuration, launch, monitor);
		enableDevModeForNodeJsProject(OpenShiftServerUtils.getReplicationController(beh.getServer()), beh.getServer());
		try {
			toggleDebugging(mode, monitor, beh);
		} catch (CoreException e) {
			beh.setServerStopped();
			throw e;
		}
	}
	
	/**
	 * Enables the DEV_MODE environment variable in {@link IDeploymentConfig} for
	 * Node.js projects (by default)
	 *
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-22362">JBIDE-22362</a>
	 */
	@SuppressWarnings("restriction")
	private void enableDevModeForNodeJsProject(IReplicationController rc, IServer server) {

		new Job(NLS.bind("Enabling {0} for replication controller {1}",  DEV_MODE, rc.getName())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					rc.setEnvironmentVariable(DEV_MODE, Boolean.TRUE.toString());
					Connection conn = ConnectionsRegistryUtil.getConnectionFor(rc);
					conn.updateResource(rc);
					return Status.OK_STATUS;
				} catch (Exception e) {
					String message = NLS.bind("Unable to enable {0} for deployment config {1}",  DEV_MODE, rc.getName());
					OpenShiftCoreActivator.getDefault().getLogger().logError(message, e);
					return new Status(Status.ERROR, OpenShiftCoreActivator.PLUGIN_ID, message, e);
				}
			}

		}.schedule();
	}
	
	@Override
	protected void startDebugging(IServer server, IReplicationController rc, DebuggingContext debugContext,
			IProgressMonitor monitor) throws CoreException {
		int remotePort = debugContext.getDebugPort();
		if (remotePort == DebuggingContext.NO_DEBUG_PORT) {
			debugContext.setDebugPort(DEFAULT_DEBUG_PORT);//TODO get default port from server settings?
		}
		IDebugListener listener = new IDebugListener() {
			
			@Override
			public void onDebugChange(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				if (debuggingContext.getPod() == null) {
					throw toCoreException("Unable to connect to remote pod");
				}
				int localPort = mapPortForwarding(debuggingContext, monitor);
				NodeDebugLauncher.launch(server, localPort);
			}

			@Override
			public void onPodRestart(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		debugContext.setDebugListener(listener);
		OpenShiftDebugUtils.get().enableDebugMode(rc, debugContext, monitor);
	}
}
