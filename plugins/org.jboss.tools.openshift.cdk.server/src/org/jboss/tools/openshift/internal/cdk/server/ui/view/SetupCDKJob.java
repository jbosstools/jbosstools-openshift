package org.jboss.tools.openshift.internal.cdk.server.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.MinishiftBinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK3Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchUtility;

public class SetupCDKJob extends Job {

	private IServer server;
	private Shell shell;
	private boolean wait;

	public SetupCDKJob(IServer server) {
		this(server, null);
	}

	public SetupCDKJob(IServer server, Shell shell) {
		this(server, shell, false);
	}

	public SetupCDKJob(IServer server, Shell shell, boolean wait) {
		super("Setup CDK");
		this.server = server;
		this.shell = shell;
		this.wait = wait;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		CDK3Server cdk3 = (CDK3Server) server.loadAdapter(CDK3Server.class, new NullProgressMonitor());
		if (cdk3 != null) {
			if (!promptRun(cdk3)) {
				return Status.CANCEL_STATUS;
			}

			if (!wait) {
				launchSetup(server);
			} else {
				Object waiting = new Object();
				WaitingLaunchListener listener = new WaitingLaunchListener(waiting);
				DebugPlugin.getDefault().addDebugEventListener(listener);
				ILaunch launch = launchSetup(server);
				listener.setLaunchAndWait(launch);
				DebugPlugin.getDefault().removeDebugEventListener(listener);
			}
		}
		return Status.OK_STATUS;
	}

	private boolean promptRun(CDK3Server cdk3) {
		String home = cdk3.getMinishiftHome();
		final int[] retmain = new int[1];
		if (new File(home).exists()) {
			Display.getDefault().syncExec(() -> {
				if( shell == null )
					shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				
				String title = "Warning: Folder already exists!";
				String msgText = "Setup CDK will delete all existing contents of {0}. Are you sure you want to continue?";
				String msg = NLS.bind(msgText, home);
				MessageDialog messageDialog = new MessageDialog(shell, title, null, msg, MessageDialog.WARNING, 
						new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				retmain[0] = messageDialog.open();
			});
			if (retmain[0] != IDialogConstants.OK_ID) {
				return false;
			}
		}
		return true;
	}

	private ILaunch launchSetup(IServer server) {
		String cmd = MinishiftBinaryUtility.getMinishiftLocation(server);
		String args = "setup-cdk --force";
		try {
			ILaunchConfiguration lc = server.getLaunchConfiguration(true, new NullProgressMonitor());
			ILaunchConfigurationWorkingCopy lc2 = new CDKLaunchUtility().createExternalToolsLaunch(server, args,
					new Path(cmd).lastSegment(), lc, cmd, true);
			return lc2.launch("run", new NullProgressMonitor());
		} catch (CoreException ce) {
			CDKCoreActivator.pluginLog().logError(ce);
		}
		return null;
	}

	private static class WaitingLaunchListener implements IDebugEventSetListener {
		private Object waiting;
		private ArrayList<DebugEvent> cachedDebugEvents;
		private ILaunch launch;
		private IProcess myProcess;

		
		public WaitingLaunchListener(Object waiting) {
			this.waiting = waiting;
			cachedDebugEvents = new ArrayList<DebugEvent>();
		}

		public void setLaunchAndWait(ILaunch launch) {
			this.launch = launch;
			this.myProcess = this.launch.getProcesses()[0];
			synchronized (waiting) {
				while( !checkAllCachedEvents()) {
					try {
						waiting.wait();
					} catch (InterruptedException ie) {
						// ignore
					}
				}
			}
		}

		private boolean checkAllCachedEvents() {
			synchronized(waiting) {
				for(DebugEvent e : cachedDebugEvents) {
					if (myProcess.equals(e.getSource()) && e.getKind() == DebugEvent.TERMINATE) {
						return true;
					}
				}
			}
			return false;
		}
		
		@Override
		public void handleDebugEvents(DebugEvent[] events) {
			if (events != null) {
				int size = events.length;
				synchronized(waiting) {
					if( myProcess != null ) {
						for (int i = 0; i < size; i++) {
							if (myProcess.equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
								waiting.notifyAll();
							}
						}
					} 
					cachedDebugEvents.addAll(Arrays.asList(events));
				}
			}
		}

	}

}
