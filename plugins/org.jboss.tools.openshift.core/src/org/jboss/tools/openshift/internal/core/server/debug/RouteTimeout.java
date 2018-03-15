/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.debug;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * A class that allows to set and remove timeouts to a route. The route is
 * determined via the given resource. Route timeouts are set via an openshift
 * annotation that configures the HAProxy that backs the route.
 * 
 * @author Andre Dietisheim
 *
 * @see <a href="https://docs.openshift.com/container-platform/3.6/architecture/networking/routes.html#route-specific-annotations" />
 */
public class RouteTimeout {

	public static final String ROUTE_DEBUG_TIMEOUT = "1h";

	private IResource resource;
	private Connection connection;

	RouteTimeout(IResource resource, Connection connection) {
		this.resource = resource;
		this.connection = connection;
	}

	/**
	 * Sets the given timeout to the route that's found for the given resource.
	 * Returns the route that was modified or {@code null} otherwise.
	 * 
	 * @param timeout
	 * @param monitor
	 * @return the route if it was modified or {@code null}
	 * @throws CoreException 
	 */
	public IRoute set(DebugContext context, IProgressMonitor monitor) throws CoreException {
		IRoute route = getRoute(resource, connection, monitor);
		if (route == null) {
			OpenShiftCoreActivator.pluginLog()
					.logInfo(NLS.bind(
							"Could not increase timeout for debugging: could not find any route for resource {0}",
							resource.getName()));
			return null;
		}

		monitor.subTask(NLS.bind("Setting haproxy timeout for route {0}...", route.getName()));

		String currentTimeout = getAnnotation(route);
		if (!StringUtils.isBlank(currentTimeout)) {
			OpenShiftServerUtils.setRouteTimeout(currentTimeout, context.getServer()); // store for latter restore
			if (StringUtils.equals(ROUTE_DEBUG_TIMEOUT, currentTimeout)) {
				return null;
			}
		}

		setAnnotation(ROUTE_DEBUG_TIMEOUT, route);
		return route;
	}

	/**
	 * Removes the timeout from the route for the given resource.
	 * Restores the timeout that we backed up before setting our value if it exists.
	 * Does nothing otherwise. <br/>
	 * Returns the route if it was modified, {@code null} otherwise.
	 * 
	 * @param timeout
	 * @param monitor
	 * @return the route that was modified or {@code null}
	 * @throws CoreException 
	 */
	public IRoute reset(DebugContext context, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Getting route for resource {0}...", resource.getName()));

		IRoute route = getRoute(resource, connection, monitor);
		if (route == null) {
			OpenShiftCoreActivator.pluginLog()
					.logInfo(NLS.bind("Could not find any route for resource {0}", resource.getName()));
			return null;
		}

		monitor.subTask(NLS.bind("Removing/restoring timeout for route {0}", route.getName()));
		if (OpenShiftServerUtils.hasRouteTimeout(context.getServer())) {
			setAnnotation(OpenShiftServerUtils.getRouteTimeout(context.getServer()), route);
			OpenShiftServerUtils.setRouteTimeout(null, context.getServer()); // clear backup
		} else {
			removeAnnotation(route);
		}
		return route;
	}

	private IRoute getRoute(IResource resource, Connection connection, IProgressMonitor monitor) {
		SubMonitor routeMonitor = SubMonitor.convert(monitor);
		routeMonitor.beginTask("Determine route to set the haproxy timeout for...", 2);
		if (routeMonitor.isCanceled()) {
			return null;
		}
		List<IService> services = connection.getResources(ResourceKind.SERVICE, resource.getNamespaceName());
		Collection<IService> matchingServices = ResourceUtils.getServicesFor(resource, services);
		routeMonitor.worked(1);
		if (routeMonitor.isCanceled()) {
			return null;
		}
		List<IRoute> routes = connection.getResources(ResourceKind.ROUTE, resource.getNamespaceName());
		// TODO: support multiple matching routes, for now only get first
		Optional<IRoute> matchingRoute = matchingServices.stream()
				.flatMap(service -> ResourceUtils.getRoutesFor(service, routes).stream()).findFirst();
		routeMonitor.worked(1);
		routeMonitor.done();
		return matchingRoute.orElse(null);
	}

	private void setAnnotation(String timeout, IRoute route) {
		route.setAnnotation(OpenShiftAPIAnnotations.TIMEOUT, timeout);
	}

	private String getAnnotation(IRoute route) {
		return route.getAnnotation(OpenShiftAPIAnnotations.TIMEOUT);
	}

	private void removeAnnotation(IRoute route) {
		route.removeAnnotation(OpenShiftAPIAnnotations.TIMEOUT);
	}
}
