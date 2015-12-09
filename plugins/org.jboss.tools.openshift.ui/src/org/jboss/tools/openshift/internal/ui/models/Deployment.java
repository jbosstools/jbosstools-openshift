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
package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IPod;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * A deployment is the collection of resources
 * that makes up an 'application'
 * 
 * @author jeff.cantrill
 *
 */
public class Deployment extends ObservablePojo {

	public static final String PROP_BUILDS = getProperty(ResourceKind.BUILD);
	public static final String PROP_PODS = getProperty(ResourceKind.BUILD);
	public static final String PROP_ROUTES = getProperty(ResourceKind.ROUTE);
	public static final String PROP_REPLICATION_CONTROLLERS = getProperty(ResourceKind.REPLICATION_CONTROLLER);
	public static final String PROP_BUILD_CONFIGS = getProperty(ResourceKind.BUILD_CONFIG);

	private IService service;
	private Map<String, List<IResourceUIModel>> resources = new ConcurrentHashMap<>();
	
	public Deployment(IService service) {
		this.service = service;
		for (String kind : new String [] {ResourceKind.BUILD, ResourceKind.POD, ResourceKind.ROUTE, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.BUILD_CONFIG}) {
			resources.put(kind, new ArrayList<>());
		}
	}
	
	public Deployment(IService service, Collection<IRoute> routes, Collection<IBuild> builds, Collection<IPod> pods, Collection<IReplicationController> rcs) {
		this.service = service;
		resources.put(ResourceKind.BUILD, init(builds));
		resources.put(ResourceKind.POD, init(pods));
		resources.put(ResourceKind.ROUTE, init(routes));
		resources.put(ResourceKind.REPLICATION_CONTROLLER, init(rcs));
	}
	private <T extends IResource> List<IResourceUIModel> init(Collection<T> resources) {
		if(resources != null) {
			return resources.stream().map(r->new OpenShiftResourceUIModel(r)).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public IService getService() {
		return this.service;
	}
	
	public Collection<IResourceUIModel> getBuilds() {
		return resources.get(ResourceKind.BUILD);	
	}
	
	public void setBuilds(Collection<IResourceUIModel> builds) {
		firePropertyChange(PROP_BUILDS, resources.get(ResourceKind.BUILD), resources.put(ResourceKind.BUILD, new ArrayList<>(builds)));
	}
	
	public void setBuildResources(Collection<IBuild> builds) {
		firePropertyChange(PROP_BUILDS, resources.get(ResourceKind.BUILD), resources.put(ResourceKind.BUILD, init(builds)));
	}
	
	public Collection<IResourceUIModel> getPods() {
		return resources.get(ResourceKind.POD);
	}
	
	public void setPods(Collection<IResourceUIModel> pods) {
		firePropertyChange(PROP_PODS, resources.get(ResourceKind.POD), resources.put(ResourceKind.POD, new ArrayList<>(pods)));
	}

	public void setPodResources(Collection<IPod> pods) {
		firePropertyChange(PROP_PODS, resources.get(ResourceKind.POD), resources.put(ResourceKind.POD, init(pods)));
	}
	
	public Collection<IResourceUIModel> getRoutes() {
		return resources.get(ResourceKind.ROUTE);
	}

	public void setRoutes(Collection<IResourceUIModel> routes) {
		firePropertyChange(PROP_ROUTES, resources.get(ResourceKind.ROUTE), resources.put(ResourceKind.ROUTE, new ArrayList<>(routes)));
	}
	
	public void setRouteResources(Collection<IRoute> routes) {
		firePropertyChange(PROP_ROUTES, resources.get(ResourceKind.ROUTE), resources.put(ResourceKind.ROUTE, init(routes)));
	}

	public Collection<IResourceUIModel> getReplicationControllers() {
		return resources.get(ResourceKind.REPLICATION_CONTROLLER);
	}
	
	public void setReplicationControllers(Collection<IResourceUIModel> rcs) {
		firePropertyChange(PROP_REPLICATION_CONTROLLERS, resources.get(ResourceKind.REPLICATION_CONTROLLER), resources.put(ResourceKind.REPLICATION_CONTROLLER, new ArrayList<>(rcs)));
	}

	public void setReplicationControllerResources(Collection<IReplicationController> rcs) {
		firePropertyChange(PROP_REPLICATION_CONTROLLERS, resources.get(ResourceKind.REPLICATION_CONTROLLER), resources.put(ResourceKind.REPLICATION_CONTROLLER, init(rcs)));
	}
	public Collection<IResourceUIModel> getBuildConfigs(){
		return resources.get(ResourceKind.BUILD_CONFIG);
	}
	
	public void setBuildConfigs(Collection<IResourceUIModel> buildConfigs) {
		firePropertyChange(PROP_BUILD_CONFIGS, resources.get(ResourceKind.BUILD_CONFIG), resources.put(ResourceKind.BUILD_CONFIG, new ArrayList<>(buildConfigs)));
	}
	
	public void setBuildConfigResources(Collection<IResource> buildConfigs) {
		firePropertyChange(PROP_BUILD_CONFIGS, resources.get(ResourceKind.BUILD_CONFIG), resources.put(ResourceKind.BUILD_CONFIG, init(buildConfigs)));
	}

	public void add(IResource resource) {
		final String property = getProperty(resource.getKind());
		List<IResourceUIModel> models = resources.get(resource.getKind());
		if(models != null) {
			models.add(new OpenShiftResourceUIModel(resource));
			int index = models.size();
			fireIndexedPropertyChange(property, index, null, Collections.unmodifiableList(models));
		}
	}

	public void remove(IResource resource) {
		final String property = getProperty(resource.getKind());
		List<IResourceUIModel> models = resources.get(resource.getKind());
		if(models != null) {
			int index = indexOf(models, resource);
			if(index > -1) {
				models.remove(index);
				fireIndexedPropertyChange(property, index, Collections.unmodifiableList(models), null);
			}
		}
	}

	public void update(IResource resource) {
		final String property = getProperty(resource.getKind());
		List<IResourceUIModel> models = resources.get(resource.getKind());
		if(models != null) {
			int index = indexOf(models, resource);
			if(index > -1) {
				List<IResourceUIModel> old = new ArrayList<>(models);
				models.set(index, new OpenShiftResourceUIModel(resource));
				fireIndexedPropertyChange(property, index, old, Collections.unmodifiableList(models));
			}
		}
	}
	
	private int indexOf(List<IResourceUIModel> models, IResource resource) {
		for (int i = 0; i < models.size(); i++) {
			IResourceUIModel model = models.get(i);
			if(model != null) {
				IResource old = model.getResource();
				if(old.equals(resource)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private static String getProperty(String kind) {
		return StringUtils.pluralize(kind.toLowerCase());
	}

}
