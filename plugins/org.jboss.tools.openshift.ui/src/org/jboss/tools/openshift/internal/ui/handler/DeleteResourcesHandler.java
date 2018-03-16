/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.core.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.core.job.OpenShiftJobs;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.wizard.resource.DeleteResourcesWizard;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class DeleteResourcesHandler extends AbstractHandler {

	private static final Point DIALOG_SIZE = new Point(800, 600);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResourceWrapper<?, ?>[] wrappers = UIUtils.getElements(UIUtils.getCurrentSelection(event),
				IResourceWrapper.class);
		if (ArrayUtils.isEmpty(wrappers) || wrappers[0].getWrapped() == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("Could not delete resources: "
					+ "No resource selected that we can get the connection and namespace from.");
		}

		IResource selectedResource = wrappers[0].getWrapped();

		Connection connection = ConnectionsRegistryUtil.getConnectionFor(selectedResource);
		if (connection == null) {
			return OpenShiftUIActivator.statusFactory()
					.cancelStatus(NLS.bind("Could not delete resources: No connection found for selected resource {0}",
							selectedResource.getName()));
		}
		String namespace = selectedResource.getNamespaceName();
		openDialog(connection, namespace, HandlerUtil.getActiveShell(event));

		return null;
	}

	private void openDialog(Connection connection, String namespace, Shell shell) {
		WizardDialog dialog = new OkCancelButtonWizardDialog("Delete", shell,
				new DeleteResourcesWizard(connection, namespace));
		dialog.setPageSize(DIALOG_SIZE);
		dialog.open();
	}

	/**
	 * Resources are deleted with one job per resource and job are in a job group so
	 * that an error dialog will be displayed at the end of the job group.
	 * 
	 * made protected for test purposes only
	 * 
	 * @param uiResources
	 *            the UI resources to delete
	 */
	protected void deleteResources(final IResourceWrapper<?, ?>[] uiResources) {
		final JobGroup group = new JobGroup("Delete Openshift resources", 1, uiResources.length) {

			/*
			 * Overridden because job group cancel job at first job error by default
			 */
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs,
					int numberOfCanceledJobs) {
				return false;
			}

		};
		try (Stream<IResourceWrapper<?, ?>> stream = Arrays.stream(uiResources)) {
			stream.forEach(uiResource -> {
				DeleteResourceJob job = OpenShiftJobs.createDeleteResourceJob(uiResource.getWrapped());
				job.setJobGroup(group);
				job.schedule();
			});
		}
	}
}
