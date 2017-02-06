/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.core.IRouteChooser;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerExtendedProperties extends ServerExtendedProperties {

	public OpenShiftServerExtendedProperties(IAdaptable adaptable) {
		super(adaptable);
	}

	@Override
	public boolean allowConvenienceEnhancements() {
		return false;
	}

	@Override
	public boolean hasWelcomePage() {
		return true;
	}

	@Override
	public String getWelcomePageUrl() throws GetWelcomePageURLException {
		String welcomePageUrl = null;
		try {

		//Get connection explicitly to report failure. Try and connect right now to know if it fails.
		//Do not catch OpenShiftException, let it be reported. We are more concerned of NPE.
		Connection connection = OpenShiftServerUtils.getConnection(server);
		if(connection == null || !connection.connect()) {
			throw new GetWelcomePageURLException("Connection is not established.");
		}

		IResource resource = OpenShiftServerUtils.getResource(server, connection);
		if(resource == null) {
			throw new GetWelcomePageURLException("Resource is missing.");
		}

		IProject project = resource.getProject();
		if ((project != null) && (resource instanceof IService)) {
			List<IRoute> routes = ResourceUtils.getRoutesForService((IService) resource, project.getResources(ResourceKind.ROUTE));
			IRoute route = getRoute(OpenShiftServerUtils.getRouteURL(server), routes);
			if (route == null) {
				route = getRoute(routes); 
			}
			//Reporting route == null is implemented in getRoute.
			if (route != null) {
				welcomePageUrl = route.getURL();
			}
		}

		} catch (OpenShiftException e) {
			throw new GetWelcomePageURLException(e.getMessage(), e);
		}

		return welcomePageUrl;
	}

	/**
	 * Looks for a route with the given url in the list.
	 * @param url
	 * @param routes
	 * @return
	 */
	private IRoute getRoute(String url, List<IRoute> routes) {
		if(!StringUtils.isEmpty(url)) {
			for (IRoute route: routes) {
				if(url.equals(route.getURL())) {
					return route;
				}
			}
		}
		return null;
	}

	/**
	 * Opens a dialog for user to select a route from the list.
	 * @param routes
	 * @return
	 */
	private IRoute getRoute(List<IRoute> routes) {
		IRouteChooser chooser = OpenShiftCoreUIIntegration.getInstance().getRouteChooser();
		IRoute route = null;

		if (routes == null
				|| routes.isEmpty()) {
			chooser.noRouteErrorDialog();
			return null;
		}
		if (routes.size() > 1) {
			route = chooser.chooseRoute(routes);
			if(route != null && chooser.isRememberChoice()) {
				fireUpdateRoute(server, route.getURL());
			}
		} else {
			route = routes.get(0);
		}
		return route;
	}

	private void fireUpdateRoute(final IServerAttributes server, final String route) {
		new Job("Updating Route") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// Only set the route on the server, not the project
				IServerWorkingCopy wc = server.createWorkingCopy();
				wc.setAttribute(OpenShiftServerUtils.ATTR_ROUTE, route);
				wc.setHost(UrlUtils.getHost(route));
				try {
					wc.save(true, new NullProgressMonitor());
				} catch(CoreException ce) {
					return ce.getStatus();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

}
