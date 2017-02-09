/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.ui.job.OpenShiftJobs;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;

import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 * @author fbricon@gmail.com
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class DeleteResourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = UIUtils.getCurrentSelection(event);
		IResourceWrapper<?, ?>[] resources = UIUtils.getElements(selection, IResourceWrapper.class);
		if (resources == null || resources.length == 0) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No resource selected that we can delete."); //$NON-NLS-1$
		}
		try (Stream<IResourceWrapper<?, ?>> stream = Arrays.stream(resources)) {
			boolean hasProject = stream.anyMatch(resource -> resource instanceof IProjectWrapper);

			String title = getDialogTitle(hasProject);
			String message = getDialogMessage(resources, hasProject);
			boolean confirm = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), title, message);
			if (confirm) {
				deleteResources(resources);
			}
			return null;
		}
	}

	private String getDialogTitle(boolean hasProject) {
		return hasProject ? 
				OpenShiftUIMessages.ProjectDeletionDialogTitle
				: OpenShiftUIMessages.ResourceDeletionDialogTitle;
	}

	private String getDialogMessage(IResourceWrapper<?, ?>[] resources, boolean hasProject) {
		if ((resources.length > 1)) {
			if ((hasProject)) {
				return OpenShiftUIMessages.ProjectDeletionConfirmationN;
			} else {
				return OpenShiftUIMessages.ResourceDeletionConfirmationN;
			}
		} else {
			IResource resource = resources[0].getWrapped();
			if (hasProject) {
				return NLS.bind(OpenShiftUIMessages.ProjectDeletionConfirmation, resource.getName());
			} else {
				return NLS.bind(OpenShiftUIMessages.ResourceDeletionConfirmation, resource.getName(), resource.getKind());
			}
		}
	}

	/**
	 * Resources are deleted with one job per resource and job are in a job
	 * group so that an error dialog will be displayed at the end of the job
	 * group.
	 * 
	 * @param uiResources
	 *            the UI resources to delete
	 */
	private void deleteResources(final IResourceWrapper<?, ?>[] uiResources) {
		final JobGroup group = new JobGroup("Delete Openshift resources", 1, uiResources.length) {

			/*
			 * Overridden because job group cancel job at first job error by
			 * default
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
