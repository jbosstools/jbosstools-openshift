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
package org.jboss.tools.openshift.internal.ui.explorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

public class DeploymentResourceMapper {
	
	private IProject project;
	private Connection conn;

	public DeploymentResourceMapper(Connection conn, IProject project) {
		this.project = project;
		this.conn = conn;
	}
	
	public List<Deployment> getDeployments(){
		List<IService> services = conn.getResources(ResourceKind.SERVICE, project.getName());
		Map<String, Collection<IRoute>> serviceToRoutes = mapServiceToRoutes(project.getName());
		
		List<Deployment> adapters = new ArrayList<>(services.size());
		for (IService service : services) {
			adapters.add(getDeployment(service, serviceToRoutes.get(service.getName())));
		}
		
		return adapters;
	}
	
	public static Deployment getDeployment(IService service) {
		List<IRoute> routes = ConnectionsRegistryUtil.getConnectionFor(service).getResources(ResourceKind.ROUTE, service.getNamespace());
		return getDeployment(service, routes);
	}
	
	private static Deployment getDeployment(IService service, Collection<IRoute> routes) {
		List<IPod> pods = service.getPods();
		return new Deployment(service, routes, pods);
	}
	
	private Map<String, Collection<IRoute>> mapServiceToRoutes(String projectName){
		List<IRoute> routes = conn.getResources(ResourceKind.ROUTE, project.getName());
		Map<String, Collection<IRoute>> map = new HashMap<String, Collection<IRoute>>(routes.size());
		for (IRoute route : routes) {
			String service = route.getServiceName();
			if(!map.containsKey(service)) {
				map.put(service, new HashSet<>());
			}
			map.get(service).add(route);
		}
		return map;
	}
}
