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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IAdaptable;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * A deployment is the collection of resources
 * that makes up an 'application'
 * 
 * @author jeff.cantrill
 *
 */
public class Deployment extends ResourcesUIModel implements IAdaptable {

	private final IService service;
	private final IProject project;
	private final Object grandParent; //connection
	
	public Deployment(IService service, IProjectAdapter parent) {
		super(parent);
		this.service = service;
		this.project = parent.getProject();
		this.grandParent = parent.getParent();
	}
	
	public IService getService() {
		return this.service;
	}
	
	@Override
	public Collection<IResourceUIModel> getServices() {
		return Collections.emptyList();
	}

	@Override
	public void setServices(Collection<IResourceUIModel> services) {
	}

	@Override
	public void setServiceResources(Collection<IResource> services) {
	}
	
	/**
	 * Reconcile the internal deployment references based upon the
	 * given resource and cache for cases where another relationship
	 * can only be established based on the given resource.
	 * @param resource
	 * @param cache
	 */
	public void reconcile(IResource resource, IRelationCache cache) {
		if(ResourceKind.BUILD.equals(resource.getKind())){
			Map<String, String> labels = resource.getLabels();
			if(labels.containsKey(OpenShiftAPIAnnotations.BUILD_CONFIG_NAME)) {
				List<String> names = getBuildConfigs().stream().map(bc->bc.getResource().getName()).collect(Collectors.toList());
				Collection<IResource> buildConfigs = cache.getResourcesFor(resource, ResourceKind.BUILD_CONFIG);
				for (IResource bc : buildConfigs) {
					if(!names.contains(bc.getName())) {
						add(bc);
					}
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return service.getNamespace() + "/" + service.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IBuildConfig.class.equals(adapter)) {
			Collection<IResourceUIModel> buildConfigs = getBuildConfigs();
			if (buildConfigs.size() == 1) {
				return (T) buildConfigs.iterator().next().getResource();
			}
		} else if (IService.class.equals(adapter) || IResource.class.equals(adapter)) {
			return (T) getService();
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((grandParent == null) ? 0 : grandParent.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Deployment other = (Deployment) obj;
		if (grandParent == null) {
			if (other.grandParent != null)
				return false;
		} else if (!grandParent.equals(other.grandParent))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		return true;
	}

	
}

