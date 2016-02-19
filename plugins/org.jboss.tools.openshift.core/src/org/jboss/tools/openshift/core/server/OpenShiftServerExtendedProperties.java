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

import org.eclipse.core.runtime.IAdaptable;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.IRouteChooser;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
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
	public String getWelcomePageUrl() {
		String welcomePageUrl = null;
		IService service = OpenShiftServerUtils.getService(server);
		IProject project = service.getProject();
		if (project != null) {
			List<IRoute> routes = project.getResources(ResourceKind.ROUTE);
			IRoute route = getRoute(OpenShiftServerUtils.getRouteURL(server), routes);
			if (route == null) {
				route = getRoute(routes); 
			}
			if (route != null) {
				welcomePageUrl = route.getURL();
			}
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
		} else {
			route = routes.get(0);
		}
		return route;
	}

}
