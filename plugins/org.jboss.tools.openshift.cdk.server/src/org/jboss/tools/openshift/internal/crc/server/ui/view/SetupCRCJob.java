package org.jboss.tools.openshift.internal.crc.server.ui.view;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupDirector;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.ArgsUtil;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.ProcessLaunchUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.ProcessUtility;
import org.jboss.tools.openshift.internal.cdk.server.ui.view.SetupCDKJob;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

public class SetupCRCJob extends SetupCDKJob {

	private boolean useTerminal;
	
	public SetupCRCJob(IServer server, Shell shell) {
		this(server, shell, true);
	}

	public SetupCRCJob(IServer server, Shell shell, boolean useTerminal) {
		this(server, shell, useTerminal, false);
	}

	public SetupCRCJob(IServer server, Shell shell, boolean useTerminal, boolean wait) {
		super(server, shell, "Setup CRC", wait);
		this.useTerminal = useTerminal;
	}

	protected String getContainerHome() {
		return new File(System.getProperty("user.home"), ".crc").getAbsolutePath();
	}
	
	protected boolean isValid() {
		return (CRC100Server) server.loadAdapter(CRC100Server.class, new NullProgressMonitor()) != null;
	}

	protected String getBinaryLocation() {
		return  BinaryUtility.CRC_BINARY.getLocation(server);
	}
	protected String getLaunchArgs() {
		return "setup";
	}
	protected ILaunch launchSetup(IServer server) {
		if( useTerminal) {
			Process p = launchSetupViaTerminal(server);
			ILaunch l = new Launch(null, "run", null);
			ProcessUtility.addProcessToLaunch(p, l, server, true, getBinaryLocation());
			return l;
		} else {
			return super.launchSetup(server);
		}
	}
	protected Process launchSetupViaTerminal(IServer server) {
		String cmd = getBinaryLocation();
		File wd = new File(cmd).getParentFile();
		String args = getLaunchArgs();
		Process p;
		try {
			p = ProcessLaunchUtility.callProcess(cmd, 
					ArgsUtil.parse(args), wd, new HashMap<>(System.getenv()), true);
			ProcessLaunchUtility.linkTerminal(server, p);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return p;
	}
}
