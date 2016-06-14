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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.dialog.CheckboxMessageDialog;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.utils.ResourceProcessor;

import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 * @author fbricon@gmail.com
 * @author Jeff Maury
 */
public class DeleteResourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IResourceUIModel[] resources = UIUtils.getElements(selection, IResourceUIModel.class);
		if(resources == null || resources.length == 0) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No resource selected that we can delete."); //$NON-NLS-1$
		}
		try (Stream<IResourceUIModel> stream = Arrays.stream(resources)) {
	        boolean willDeleteSubResources = stream.anyMatch(resource -> Platform.getAdapterManager().getAdapter(resource.getResource(), ResourceProcessor.class).willCascadeDeleteLinkedResources());
	        boolean confirm;
	        
	        String message = (resources.length > 1)?(willDeleteSubResources)?OpenShiftUIMessages.ResourceWithLinkedResourcesDeletionConfirmationN:OpenShiftUIMessages.ResourceDeletionConfirmationN
                    :(willDeleteSubResources)?NLS.bind(OpenShiftUIMessages.ResourceWithLinkedResourcesDeletionConfirmation, resources[0].getResource().getName(), resources[0].getResource().getKind()):
                                  NLS.bind(OpenShiftUIMessages.ResourceDeletionConfirmation, resources[0].getResource().getName(), resources[0].getResource().getKind());
            if (willDeleteSubResources) {
	            int status = new CheckboxMessageDialog(HandlerUtil.getActiveShell(event), OpenShiftUIMessages.ResourceDeletionDialogTitle, message, "Cascade delete").open();
	            confirm = ((status & Window.CANCEL) == 0);
	            willDeleteSubResources = (status & CheckboxMessageDialog.CHECKBOX_SELECTED) == CheckboxMessageDialog.CHECKBOX_SELECTED;
	        } else {
	            confirm = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), 
                                                    OpenShiftUIMessages.ResourceDeletionDialogTitle, 
                                                    message);
	        }
	        
	        if (confirm) {
	            deleteResources(resources, willDeleteSubResources);
	        }
	        return null;
		}
	}
	
	/**
	 * Resources are deleted with one job per resource and job are in a job group so
	 * that an error dialog will be displayed at the end of the job group.
	 * 
	 * @param uiResources the UI resources to delete
	 * @param willDeleteSubResources 
	 */
	private void deleteResources(final IResourceUIModel[] uiResources, boolean willDeleteSubResources) {
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
	    DeleteResourceJob<IResourceUIModel> job = new DeleteResourceJob<>(Arrays.asList(uiResources), willDeleteSubResources, new DeleteResourceJob.Callback<IResourceUIModel>() {
            @Override
            public IResource getResource(IResourceUIModel wrapper) {
                return wrapper.getResource();
            }

            @Override
            public void preProcess(IResourceUIModel wrapper) {
                wrapper.setDeleting(true);
            }

            @Override
            public void postProcess(IResourceUIModel wrapper) {
                wrapper.setDeleting(false);
            }
        });
        job.schedule();
	}
}
