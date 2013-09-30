/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.dialog.CheckboxMessageDialog;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

public class DeleteDomainAction extends AbstractOpenShiftAction {

	public DeleteDomainAction() {
		super(OpenShiftExpressUIMessages.DELETE_DOMAIN_ACTION);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE));
	}

	@Override
	public void run() {
		final Connection connection = UIUtils.getFirstElement(getSelection(), Connection.class);
		if (connection == null) {
			return;
		}
		try {
			final IDomain domain = connection.getDefaultDomain();
			if (domain == null) {
				MessageDialog.openInformation(
						Display.getCurrent().getActiveShell(), "Delete Domain", "User has no domain");
			} else {
				boolean confirm = false;
				MessageDialog dialog = new CheckboxMessageDialog(Display.getCurrent()
						.getActiveShell(), "Domain deletion", NLS.bind(
						"You are about to delete the \"{0}\" domain.\n"
								+ "Do you want to continue?",
						domain.getId()),
						"Force applications deletion (data will be lost and operation cannot be undone)");
				int result = dialog.open();
				if ((result == CheckboxMessageDialog.CHECKBOX_SELECTED) || (result == MessageDialog.OK)) {
					confirm = true;
				}
				final boolean includeApps = ((result & CheckboxMessageDialog.CHECKBOX_SELECTED) > 0);
				if (confirm) {
					Job job = new Job("Deleting OpenShift Domain...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								try {
									domain.destroy(includeApps);
									return Status.OK_STATUS;
								} catch (OpenShiftException e) {
									return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind(
											"Failed to delete domain \"{0}\"", domain.getId()), e);
								}
							} finally {
								monitor.done();
								RefreshViewerJob.refresh(viewer);
							}
						}
					};
					job.setPriority(Job.SHORT);
					job.schedule(); // start as soon as possible
				}

			}
		} catch (OpenShiftException e) {
			Logger.warn(
					"Failed to retrieve User domain, prompting for creation", e);
		}
	}
}
