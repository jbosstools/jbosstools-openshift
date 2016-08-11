/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.openshift.restclient.model.volume.IPersistentVolumeClaim;

public class StoragePropertySource extends ResourcePropertySource<IPersistentVolumeClaim> {

	public StoragePropertySource(IPersistentVolumeClaim resource) {
		super(resource);
	}
	
	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new UneditablePropertyDescriptor(StorageIds.Modes, "Access Modes"),
				new UneditablePropertyDescriptor(StorageIds.Requested, "Requested Capacity"),
				new UneditablePropertyDescriptor(StorageIds.Status, "Status")
		};
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		if (id instanceof StorageIds) {
			StorageIds stId = (StorageIds)id;
			switch (stId) {
			case Modes:
				return String.join(", ", getResource().getAccessModes());
			case Requested:
				return getResource().getRequestedStorage();
			case Status:
				return getStatus();
			}
		}
		return super.getPropertyValue(id);
	}
	
	private String getStatus() {
		IPersistentVolumeClaim pvc = getResource();
		String status = pvc.getStatus();
		if (StringUtils.isNotBlank(pvc.getVolumeName())) {
			return String.join(" ", status, "to volume", pvc.getVolumeName());
		}
		return status;
	}

	public static enum StorageIds{
		Modes,
		Requested,
		Status
	}
	
}
