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
package org.jboss.tools.openshift.internal.ui.property;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftResourceInput implements IStorageEditorInput {

	private IStorage storage;
	private Connection connection;
	private IResource input;

	public OpenShiftResourceInput(Connection connection, IResource resource) {
		this.connection = connection;
		this.input = resource;
		this.storage = new IStorage() {
			public InputStream getContents() throws CoreException {
				try {
					return IOUtils.toBufferedInputStream(IOUtils.toInputStream(input.toJson(), "UTF-8"));
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to edit input", e));
				}
			}

			public IPath getFullPath() {
				return new Path(input.getNamespace())
						.append(input.getKind())
						.append(input.getName()+".json");
			}

			@SuppressWarnings("unchecked")
			public Object getAdapter(Class adapter) {
				return null;
			}

			public String getName() {
				StringBuilder sb = new StringBuilder()
						.append(StringUtils.humanize(input.getKind()))
						.append(":")
						.append(input.getName())
						.append(".json");
				return sb.toString();
			}

			public boolean isReadOnly() {
				return false;
			}
		};
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_ICON;
	}

	public String getName() {
		return storage.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public IStorage getStorage() {
		return storage;
	}

	public String getToolTipText() {
		return storage.getName();
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public IResource getResource() {
		return input;
	}
	
	public void setResource(IResource newResource) {
		input = newResource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connection == null) ? 0 : connection.hashCode());
		result = prime * result + ((input == null) ? 0 : input.hashCode());
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
		OpenShiftResourceInput other = (OpenShiftResourceInput) obj;
		if (connection == null) {
			if (other.connection != null)
				return false;
		} else if (!connection.equals(other.connection))
			return false;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		return true;
	}
}