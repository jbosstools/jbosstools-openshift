/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.snapshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IApplication;

/**
 * @author André Dietisheim
 */
public class SaveSnapshotWizard extends AbstractOpenShiftWizard<SaveSnapshotWizardModel> {

	public SaveSnapshotWizard(IApplication application) {
		super("Save Snapshot", new SaveSnapshotWizardModel(application));
	}

	@Override
	public boolean performFinish() {
		final String applicationName = getModel().getApplication().getName();
		try {
			IStatus status = WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob(NLS.bind("Saving snapshot for application {0}",
							applicationName)) {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							try {
								getModel().saveSnapshot(monitor);
								return Status.OK_STATUS;
							} catch (IOException e) {
								return OpenShiftUIActivator.createErrorStatus(NLS.bind("Could not save snapshot for application {0}", applicationName), e);
							}
						}
					}, getContainer());
			return status.isOK();
		} catch (InvocationTargetException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not save snapshot for application {0}", applicationName),
					status, IStatus.ERROR)
					.open();
			return false;
		} catch (InterruptedException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not save snapshot for application {0}", applicationName),
					status, IStatus.ERROR)
					.open();
			return false;
		}
	}

	@Override
	public void addPages() {
		addPage(new SaveSnapshotWizardPage(getModel(), this));
	}
}
