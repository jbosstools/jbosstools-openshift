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

import org.eclipse.core.runtime.IAdaptable;

import com.openshift.restclient.model.IBuildConfig;
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
	
	public Deployment(IService service, IProjectAdapter parent) {
		super(parent);
		this.service = service;
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
	
	@Override
	public String toString() {
		return service.getNamespace() + "/" + service.getName();
	}

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
	
	
}

