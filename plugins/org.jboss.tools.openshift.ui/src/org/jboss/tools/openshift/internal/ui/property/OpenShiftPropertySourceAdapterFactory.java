/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

public class OpenShiftPropertySourceAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if (adapterType == IPropertySource.class) {
			if(adaptableObject instanceof Connection){
				return new ConnectionPropertySource((Connection) adaptableObject);
			}
			if(adaptableObject instanceof IResource){
				IResource resource = (IResource) adaptableObject;
				switch(resource.getKind()){
				case ResourceKind.BUILD:
					return new BuildPropertySource((IBuild)resource);
				case ResourceKind.BUILD_CONFIG: 
					return new BuildConfigPropertySource((IBuildConfig)resource);
				case ResourceKind.IMAGE_STREAM:
					return new ImageStreamPropertySource((IImageStream) resource);
				case ResourceKind.POD:
					return new PodPropertySource((IPod)resource);
				case ResourceKind.REPLICATION_CONTROLLER: 
					return new ReplicationControllerPropertySource((IReplicationController) resource);
				case ResourceKind.ROUTE:
					return new RoutePropertySource((IRoute) resource);
				case ResourceKind.SERVICE: 
					return new ServicePropertySource((IService) resource);
				default:
					return new ResourcePropertySource<IResource>(resource);
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
