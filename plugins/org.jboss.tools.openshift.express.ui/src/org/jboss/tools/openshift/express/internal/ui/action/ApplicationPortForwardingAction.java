package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.portforward.ApplicationPortForwardingWizard;
import org.jboss.tools.openshift.express.internal.core.portforward.ApplicationPortForwardingWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.job.RetrieveApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.VerifySSHSessionJob;

import com.openshift.client.IApplication;

public class ApplicationPortForwardingAction extends AbstractAction {

	public ApplicationPortForwardingAction() {
		super("Port forwarding...", DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no
	 * Console/Worker existed, a new one is created, otherwise, it is displayed.
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		if (selection != null && selection instanceof ITreeSelection) {
			Object sel = ((ITreeSelection) selection).getFirstElement();
			if (sel instanceof IApplication) {
				openPortForwardingDialogFor((IApplication) sel);
			} else if (sel instanceof IServer) {
				openPortForwardingDialogFor((IServer) sel);
			}
		}
	}

	/**
	 * Retrieves the application from the given server, then opens the dialog.
	 * Since retrieving the application can be time consuming, the task is
	 * performed in a separate job (ie, in a background thread).
	 * 
	 * @param server
	 */
	private void openPortForwardingDialogFor(final IServer server) {
		final RetrieveApplicationJob job = new RetrieveApplicationJob(server);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					final IApplication application = job.getApplication();
					openPortForwardingDialogFor(application);
				}
			}
		});
		job.setUser(true);
		job.schedule();
	}

	private void openPortForwardingDialogFor(final IApplication application) {
		final VerifySSHSessionJob job = new VerifySSHSessionJob(application);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK() && job.isValidSession()) {
					openWizardDialog(application);
				}
			}
		});

		job.setUser(true);
		job.schedule();
	}

	/**
	 * Opens the Port Forwarding dialog for good...
	 * 
	 * @param application
	 */
	private void openWizardDialog(final IApplication application) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				ApplicationPortForwardingWizard wizard = new ApplicationPortForwardingWizard(
						application);
				WizardDialog dialog = new ApplicationPortForwardingWizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(),
						wizard);
				dialog.setMinimumPageSize(700, 300);
				dialog.create();
				dialog.open();
			}
		});
	}
}
