package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

public class DeleteApplicationAction extends AbstractAction {

	/**
	 * Constructor
	 */
	public DeleteApplicationAction() {
		super(OpenShiftExpressUIMessages.DELETE_APPLICATION_ACTION);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() {
		if (selection != null && selection instanceof ITreeSelection && ((ITreeSelection)selection).getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) ((ITreeSelection)selection).getFirstElement();
			final String appName = application.getName();
			final boolean confirm = MessageDialog
					.openConfirm(
							Display.getCurrent().getActiveShell(),
							"Application deletion",
							"You are about to destroy the '" + appName + "' application.\n" +
							"This is NOT reversible, all remote data for this application will be removed.");
			if (confirm) {
				Job job = new Job("Deleting application '" + appName + "'...") {
					protected IStatus run(IProgressMonitor monitor) {
						try {
							application.destroy();
						} catch (OpenShiftException e) {
							Logger.error("Failed to delete application '" + appName + "'", e);
						} finally {
							monitor.done();
						}
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.SHORT);
				job.schedule(); // start as soon as possible
			}
		}
	}

}
