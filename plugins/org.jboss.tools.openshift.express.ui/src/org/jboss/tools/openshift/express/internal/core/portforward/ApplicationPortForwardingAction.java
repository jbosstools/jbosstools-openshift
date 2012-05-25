package org.jboss.tools.openshift.express.internal.core.portforward;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractAction;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

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
				openPortForwarding((IApplication) sel);
			} else if (sel instanceof IServer) {
				openPortForwarding((IServer) sel);
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
	private void openPortForwarding(final IServer server) {
		Job job = new Job("Retrieving application's forwardable ports...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final IApplication application = ExpressServerUtils.getApplication(server);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						openPortForwarding(application);
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	/**
	 * @param application
	 */
	private void openPortForwarding(IApplication application) {
		try {
			ApplicationPortForwardingWizard wizard = new ApplicationPortForwardingWizard(application);
			WizardDialog dialog = new ApplicationPortForwardingWizardDialog(Display.getCurrent().getActiveShell(),
					wizard);
			dialog.setMinimumPageSize(700, 300);
			dialog.create();
			dialog.open();
		} catch (Exception e) {
			Logger.error("Failed to perform 'port-forwarding' for application '" + application.getName() + "'", e);
		}
	}

}
