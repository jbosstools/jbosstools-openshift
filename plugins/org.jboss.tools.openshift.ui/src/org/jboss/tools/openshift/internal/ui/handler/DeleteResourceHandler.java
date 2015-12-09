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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.model.IResource;

/**
 * Handler for excuting a delete resource operation
 * 
 * @author jeff.cantrill
 */
public class DeleteResourceHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService().getSelection();
		IResource resource = UIUtils.getFirstElement(selection, IResource.class);
		if(resource == null) {
			IResourceUIModel model = UIUtils.getFirstElement(selection, IResourceUIModel.class);
			if(model != null) {
				resource = model.getResource();
			}
		}
		if(resource == null) {
			return Status.OK_STATUS;
		}
		if(MessageDialog.openQuestion(HandlerUtil.getActiveShell(event), "Delete Resource", NLS.bind("Are you certain you want to delete the {0} resource named {1}?", resource.getKind(), resource.getName()))){
			new DeleteResourceJob(resource).schedule();
			return Status.OK_STATUS;
		}
		return Status.CANCEL_STATUS;
	}


}
