/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class CreateStorageModel extends ComponentModel {
	public static final String PROPERTY_STORAGE_NAME = "storageName";
	public static final String PROPERTY_MOUNT_PATH = "mountPath";
	public static final String PROPERTY_SIZE = "size";
	
	private String storageName;
	private String mountPath;
	private String size;
	private List<String> sizes;
	
	public CreateStorageModel(Odo odo, String projectName, String applicationName, String componentName, List<String> sizes) {
		super(odo, projectName, applicationName, componentName);
		this.sizes = sizes;
		if (!sizes.isEmpty()) {
			setSize(sizes.get(0));
		}
	}
	
	/**
	 * @return the storageName
	 */
	public String getStorageName() {
		return storageName;
	}

	/**
	 * @param storageName the storageName to set
	 */
	public void setStorageName(String storageName) {
		firePropertyChange(PROPERTY_STORAGE_NAME, this.storageName, this.storageName = storageName);
	}


	/**
	 * @return the mountPath
	 */
	public String getMountPath() {
		return mountPath;
	}

	/**
	 * @param mountPath the mountPath to set
	 */
	public void setMountPath(String mountPath) {
		this.mountPath = mountPath;
	}

	/**
	 * @return the size
	 */
	public String getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(String size) {
		firePropertyChange(PROPERTY_SIZE, this.size, this.size = size);
	}

	/**
	 * @return the ports
	 */
	public List<String> getSizes() {
		return sizes;
	}
}
