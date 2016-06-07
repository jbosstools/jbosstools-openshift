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
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.IOpenshiftUIElement;

import com.openshift.restclient.OpenShiftException;

/**
 * @author Jeff Cantrill
 */
public class RefreshResourceHandler extends AbstractHandler {

	private static final String FAILED_TO_REFRESH_ELEMENT = "Failed to refresh element";
	private static final String LOADING_OPEN_SHIFT_INFORMATIONS = "Loading OpenShift information...";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService()
				.getSelection();
		IOpenshiftUIElement<?> element = UIUtils.getFirstElement(selection, IOpenshiftUIElement.class);
		if (element != null) {
			refresh(element);
		}
		return null;
	}


	private void refresh(IOpenshiftUIElement<?> element) {
		Job job = null;
		job = createRefreshJob(element);
		job.schedule();
	}

	private Job createRefreshJob(IOpenshiftUIElement<?> element) {
		return new AbstractDelegatingMonitorJob(LOADING_OPEN_SHIFT_INFORMATIONS) {

			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					monitor.beginTask(LOADING_OPEN_SHIFT_INFORMATIONS, IProgressMonitor.UNKNOWN);
					element.refresh();
				} catch (OpenShiftException e) {
					OpenShiftCommonUIActivator.getDefault().getLogger().logError(FAILED_TO_REFRESH_ELEMENT, e);
					return new Status(Status.ERROR, OpenShiftCommonUIActivator.PLUGIN_ID, FAILED_TO_REFRESH_ELEMENT, e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
	}
}
