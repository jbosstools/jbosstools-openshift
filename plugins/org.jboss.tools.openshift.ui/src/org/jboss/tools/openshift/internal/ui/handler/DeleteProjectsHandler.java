/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.ui.job.OpenShiftJobs;

import com.openshift.restclient.model.IProject;

/**
 * @author jeff.cantrill
 * @author fbricon@gmail.com
 */
public class DeleteProjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IProject project = UIUtils.getFirstElement(selection, IProject.class);
		if(project == null || project.getProject() == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No project selected that we can delete."); //$NON-NLS-1$
		}
		deleteProject(project, HandlerUtil.getActiveShell(event));
		return null;
	}
	
	private void deleteProject(final IProject adapter, Shell shell) {
		IProject project = adapter.getProject();
		boolean confirm = MessageDialog.openConfirm(shell, 
				OpenShiftUIMessages.ProjectDeletionDialogTitle, 
				NLS.bind(OpenShiftUIMessages.ProjectDeletionConfirmation, project.getName()));
		if (!confirm) {
			return;
		}
		DeleteResourceJob job = OpenShiftJobs.createDeleteProjectJob(project);
		job.schedule();
	}

}
