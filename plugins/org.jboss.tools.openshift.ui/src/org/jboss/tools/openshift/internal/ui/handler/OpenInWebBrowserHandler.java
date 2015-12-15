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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.SelectRouteDialog;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class OpenInWebBrowserHandler extends AbstractHandler {
	private static final String NO_ROUTE_MSG = "Could not find a route that points to an url to show in a browser.";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);

		final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		final IRoute route = UIUtils.getFirstElement(currentSelection, IRoute.class);

		//Open route
		if (route != null) {
			return openBrowser(shell, route);
		}

		//Open Project
		final IProject project = UIUtils.getFirstElement(currentSelection, IProject.class);
		if (project != null) {
			new UIUpdatingJob(NLS.bind("Loading routes for project {0}", project.getName())) {

				private List<IRoute> routes;

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					this.routes = project.getResources(ResourceKind.ROUTE);
					return Status.OK_STATUS;
				}

				protected IStatus updateUI(IProgressMonitor monitor) {
					if (routes == null || routes.isEmpty()) {
						return nothingToOpenDialog(shell);
					}
					if (routes.size() == 1) {
						return openBrowser(shell, routes.get(0));
					}
					SelectRouteDialog routeDialog = new SelectRouteDialog(routes, HandlerUtil.getActiveShell(event));
					if (routeDialog.open() == Dialog.OK) {
						return openBrowser(shell, routeDialog.getSelectedRoute());
					}
					return Status.OK_STATUS;
				}
			}.schedule();
			return Status.OK_STATUS;
		}
		
		//Open Connection
		final IConnection connection = UIUtils.getFirstElement(currentSelection, IConnection.class);
		if (connection != null) {
			return openBrowser(shell, connection.getHost());
		}

		return nothingToOpenDialog(shell);
	}
	
	private IStatus nothingToOpenDialog(Shell shell) {
		MessageDialog.openWarning(shell,"No Route", NO_ROUTE_MSG);
		return OpenShiftUIActivator.statusFactory().cancelStatus(NO_ROUTE_MSG);
	}
	
	protected IStatus openBrowser(Shell shell, IRoute route) {
		if (route == null) {
			return nothingToOpenDialog(shell);
		}
		return openBrowser(shell, route.getURL());
	}

	protected IStatus openBrowser(Shell shell, String url) {
		if (StringUtils.isBlank(url)) {
			return nothingToOpenDialog(shell);
		}
		new BrowserUtility().checkedCreateInternalBrowser(url,
				"", OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
		return Status.OK_STATUS;
	}

}
