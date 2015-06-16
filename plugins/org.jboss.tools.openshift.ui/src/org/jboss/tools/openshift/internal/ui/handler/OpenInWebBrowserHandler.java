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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
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

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IRoute route = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IRoute.class);
		if (route != null) {
			openBrowser(route);
		} else {
			final IProject project = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IProject.class);
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
							return OpenShiftUIActivator.statusFactory()
									.cancelStatus("Could not find a route that points to an url to show in a browser.");
						} else if (routes.size() > 1) {
							SelectRouteDialog routeDialog = new SelectRouteDialog(routes, HandlerUtil.getActiveShell(event));
							if (routeDialog.open() == Dialog.OK) {
								openBrowser(routeDialog.getSelectedRoute());
							}
						} else {
							openBrowser(routes.get(0));
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			} else {
				return OpenShiftUIActivator.statusFactory()
						.cancelStatus("Could not find a route that points to an url to show in a browser.");
			}
		}
		return Status.OK_STATUS;
	}

	private void openBrowser(IRoute route) {
		if (route == null
				|| route.getURL() == null
				|| route.getURL().isEmpty()) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Could not find a route that points to an url to show in a browser.");
			return;
		}
		new BrowserUtility().checkedCreateInternalBrowser(route.getURL(), 
				"", OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
	}

}
