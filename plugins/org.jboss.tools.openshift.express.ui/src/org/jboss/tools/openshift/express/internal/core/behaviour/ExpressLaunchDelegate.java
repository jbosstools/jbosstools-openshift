package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.Trace;
import org.jboss.ide.eclipse.as.core.server.internal.DelegatingServerBehavior;
import org.jboss.ide.eclipse.as.core.server.internal.launch.DelegatingStartLaunchConfiguration;
import org.jboss.ide.eclipse.as.core.server.internal.launch.IJBossLaunchDelegate;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.core.util.LaunchCommandPreferences;

public class ExpressLaunchDelegate implements IJBossLaunchDelegate {

	public ExpressLaunchDelegate() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actualLaunch(DelegatingStartLaunchConfiguration launchConfig,
			ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		DelegatingServerBehavior jbsBehavior = JBossServerBehaviorUtils.getServerBehavior(configuration);
		IStatus s = jbsBehavior.canStart(mode);

		Trace.trace(Trace.STRING_FINEST, "Ensuring Server can start: " + s.getMessage()); //$NON-NLS-1$
		if (!s.isOK())
			throw new CoreException(jbsBehavior.canStart(mode));
		if (LaunchCommandPreferences.isIgnoreLaunchCommand(jbsBehavior.getServer())) {
			Trace.trace(Trace.STRING_FINEST, "Server is marked as ignore Launch. Marking as started."); //$NON-NLS-1$
			jbsBehavior.setServerStarting();
			jbsBehavior.setServerStarted();
			return false;
		}
		return true;
	}

	@Override
	public void preLaunch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public void postLaunch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setupLaunchConfiguration(
			ILaunchConfigurationWorkingCopy workingCopy, IServer server)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
