/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.dialog.SelectRouteDialog;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 * @author Viacheslav Kabanovich
 */
public class EditDefaultRouteHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);

		ISelection currentSelection = UIUtils.getCurrentSelection(event);

		IServiceWrapper service = UIUtils.getFirstElement(currentSelection, IServiceWrapper.class);
		if (service != null) {
			new RouteOpenerJob(service.getWrapped().getNamespace(), shell) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					this.routes = service.getResourcesOfKind(ResourceKind.ROUTE).stream().map(r -> (IRoute)r.getWrapped()).collect(Collectors.toList());
					return Status.OK_STATUS;
				}
			}.schedule();
			return Status.OK_STATUS;
		}
		
		final IProject project = UIUtils.getFirstElement(currentSelection, IProject.class);
		if (project != null) {
			new RouteOpenerJob(project.getName(), shell) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					this.routes = project.getResources(ResourceKind.ROUTE);
					return Status.OK_STATUS;
				}
			}.schedule();
			return Status.OK_STATUS;
		}		
		return Status.OK_STATUS;
	}
	
	private abstract class RouteOpenerJob extends UIUpdatingJob {
		protected List<IRoute> routes;
		private Shell shell;

		public RouteOpenerJob(String projectName, Shell shell) {
			super(NLS.bind("Loading routes for project {0}", projectName));
			this.shell = shell;
		}

		@Override
		protected IStatus updateUI(IProgressMonitor monitor) {
			if (routes == null || routes.isEmpty()) {
				return OpenInWebBrowserHandler.nothingToOpenDialog(shell);
			}

			IRoute selectedRoute = SelectedRoutePreference.instance.getSelectedRoute(routes);

			SelectRouteDialog routeDialog = new SelectRouteDialog(routes, shell, selectedRoute != null, selectedRoute);
			if (routeDialog.open() == Dialog.OK) {
				selectedRoute = routeDialog.getSelectedRoute();
				if(routeDialog.isRememberChoice()) {
					SelectedRoutePreference.instance.setSelectedRoute(routes, selectedRoute);
				} else {
					SelectedRoutePreference.instance.removeSelectedRoute(routes);
				}
			}
			return Status.OK_STATUS;
		}
	}

}
