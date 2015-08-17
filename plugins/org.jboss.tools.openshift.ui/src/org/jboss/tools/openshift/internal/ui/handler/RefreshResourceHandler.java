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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.job.IResourcesModel;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * @author Jeff Cantrill
 */
public class RefreshResourceHandler extends AbstractHandler{

	private static final String FAILED_TO_REFRESH_ELEMENT = "Failed to refresh element";
	private static final String LOADING_OPEN_SHIFT_INFORMATIONS = "Loading OpenShift information...";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Object resource = getResource(selection);
		if (resource != null) {
			refresh(resource);
		}
		return null;
	}

	private Object getResource(ISelection selection) {
		Object resource = UIUtils.getFirstElement(selection, IRefreshable.class);
		if (resource == null) {
			resource = UIUtils.getFirstElement(selection, IConnection.class);
		}
		if(resource == null) {
			resource = UIUtils.getFirstElement(selection, IResource.class);
		}

		if(resource == null
				&& UIUtils.getFirstElement(selection, OpenShiftException.class) != null
				&& selection instanceof ITreeSelection) {
			//If nothing specific is found, find first refreshable parent of selected node
			ITreeSelection treeSelection = (ITreeSelection) selection;
			TreePath path = treeSelection.getPaths()[0].getParentPath();
			while (path != null) {
				Object o = path.getLastSegment();
				if (o instanceof IRefreshable || o instanceof IConnection || o instanceof IResource) {
					resource = o;
					break;
				}
				path = path.getParentPath();
			}
		}

		return resource;
	}

	private void refresh(final Object element) {
		Job job;
		if(element instanceof IResource) {
			job = createRefreshResourceJob(element);
		}else {
			job = createRefreshRefreshableJob(element);
		}
		job.schedule();
	}
	
	private Job createRefreshRefreshableJob(final Object element) {
		final IConnection connection = getConnection(element);
		return new AbstractDelegatingMonitorJob(LOADING_OPEN_SHIFT_INFORMATIONS) {

			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					monitor.beginTask(LOADING_OPEN_SHIFT_INFORMATIONS, IProgressMonitor.UNKNOWN);
					if (element instanceof IRefreshable) {
						((IRefreshable) element).refresh();
						ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection);
					}
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
	
	private Job createRefreshResourceJob(final Object element) {
		return new RefreshResourcesJob(new IResourcesModel() {
			@Override
			public Collection<IResource> getResources() {
				return Arrays.asList(new IResource[] {(IResource) element});
			}
		}, false);
	}
	
	private IConnection getConnection(final Object resource) {
		if (resource instanceof IConnection) {
			return (IConnection) resource; 
		}else if(resource instanceof IResource) {
			return ConnectionsRegistryUtil.getConnectionFor((IResource)resource);
		}
		return null;
	}
}
