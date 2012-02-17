package org.jboss.tools.openshift.express.internal.ui.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import com.openshift.express.client.IUser;
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
		final List<IApplication> appsToDelete = new ArrayList<IApplication>();
		for (@SuppressWarnings("unchecked")
		Iterator<Object> iterator = ((ITreeSelection) selection).iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			if (isApplication(element)) {
				appsToDelete.add((IApplication) element);
			}
		}
		if (appsToDelete.size() == 0) {
			return;
		}
		boolean confirm = false;
		if (appsToDelete.size() == 1) {
			confirm = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Application deletion",
					"You are about to destroy the '" + appsToDelete.get(0) + "' application.\n"
							+ "This is NOT reversible, all remote data for this application will be removed.");
		} else if (appsToDelete.size() > 1) {
			confirm = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Application deletion",
					"You are about to destroy " + appsToDelete.size() + " applications.\n"
							+ "This is NOT reversible, all remote data for those applications will be removed.");
		}
		if (confirm) {
			// rework here... loop inside the job, refresh required users only. Equals() and hashcode() in IUser ?
			Job job = new Job("Deleting OpenShift Application(s)...") {
				protected IStatus run(IProgressMonitor monitor) {
					Set<String> usersToRefresh = new HashSet<String>();
					try {
						for (final IApplication application : appsToDelete) {
							final String appName = application.getName();
							try {
								application.destroy(); 
								usersToRefresh.add(application.getUser().getRhlogin());
							} catch (OpenShiftException e) {
								Logger.error("Failed to delete application '" + appName + "'", e);
							}
						}
					} finally {
						monitor.done();
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								if (viewer != null) {
									//viewer.g
									viewer.refresh();
								}
							}
						});
					}

					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT);
			job.schedule(); // start as soon as possible
		}
	}

	private boolean isApplication(Object selection) {
		return selection instanceof IApplication;
	}

}
