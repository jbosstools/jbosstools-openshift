package org.jboss.tools.openshift.internal.crc.server.core.adapter.controllers;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.AbstractCDKShutdownController;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Poller;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

public class CRC100ShutdownController extends AbstractCDKShutdownController {

	@Override
	protected String getShutdownArgs() {
		return "stop";
	}

	@Override
	protected Process call(IServer s, String args, String launchConfigName) throws CoreException, IOException {
		return new CDKLaunchUtility().callCRCConsole(getServer(), args, getServer().getName());
	}

	@Override
	protected boolean useTerminal() {
		return false;
	}

	@Override
	protected String getCommandLocation() {
		return BinaryUtility.CRC_BINARY.getLocation(getServer());
	}
	
	@Override
	protected IServerStatePoller2 getPoller(IServer server) {
		return new CRC100Poller();
	}

	@Override
	protected ILaunchConfigurationWorkingCopy createShutdownLaunchConfiguration(ILaunchConfiguration lc, String cmd,
			String args) throws CoreException {
		CDKLaunchUtility util = new CDKLaunchUtility();
		ILaunchConfigurationWorkingCopy lc2 = util.createExternalToolsLaunch(getServer(), args,
				new Path(cmd).lastSegment(), lc, cmd, util.envFromLaunchConfig(getServer()), true);
		return lc2;
	}
}
