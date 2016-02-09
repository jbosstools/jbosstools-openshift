/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IReplicationController;

public class ReplicationControllerPropertySource extends ResourcePropertySource<IReplicationController> {

	public ReplicationControllerPropertySource(IReplicationController resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new UneditablePropertyDescriptor("replicas", "Replicas"),
				new UneditablePropertyDescriptor("selector", "Selector"),
				new UneditablePropertyDescriptor("images", "Image(s)"),
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if("replicas".equals(id)){
			return String.format("%s current / %s desired", getResource().getCurrentReplicaCount(), getResource().getDesiredReplicaCount());
		}
		if("selector".equals(id)){
			return StringUtils.serialize(getResource().getReplicaSelector());
		}
		if("images".equals(id)){
			return  org.apache.commons.lang.StringUtils.join(getResource().getImages(), ", ");
		}
		return super.getPropertyValue(id);
	}
	
}
