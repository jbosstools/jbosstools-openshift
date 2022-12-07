/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

public class ComponentDescriptor {
	private final String managedByVersion;
	private String path;
	private final String name;

	private final String managedBy;

	public ComponentDescriptor(String name, String path, String managedBy, String managedByVersion) {
		this.name = name;
		this.path = path;
		this.managedBy = managedBy;
		this.managedByVersion = managedByVersion;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getManagedBy() {
		return managedBy;
	}

	public boolean isPreOdo3() {
		return managedBy != null && "odo".equals(managedBy) && managedByVersion != null && managedByVersion.length() > 1
				&& managedByVersion.charAt(1) - '0' < 3;
	}
}