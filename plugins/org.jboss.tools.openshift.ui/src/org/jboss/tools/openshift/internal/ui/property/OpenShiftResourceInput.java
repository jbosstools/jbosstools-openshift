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
				return null;
			}

			@SuppressWarnings("unchecked")
			public Object getAdapter(Class adapter) {
				return null;
			}

			public String getName() {
				StringBuilder sb = new StringBuilder(StringUtils.humanize(input.getKind()));
				return sb.append(" : ").append(input.getName()).toString();
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
}