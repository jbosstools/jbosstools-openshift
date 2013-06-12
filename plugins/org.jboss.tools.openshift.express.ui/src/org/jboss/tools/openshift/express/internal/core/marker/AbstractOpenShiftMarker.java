/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.marker;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractOpenShiftMarker implements IOpenShiftMarker {

	private static final String MARKERS_DIRECTORY_NAME = ".openshift/markers/"; 
	
	public final IFile addTo(IProject project, IProgressMonitor monitor) throws CoreException {
		assertProjectIsOpen(project);
		IFile file = project.getFile(new Path(MARKERS_DIRECTORY_NAME).append(getMarkerName()));
		if (file.exists()) {
			return null;
		}
		file.create(new ByteArrayInputStream(getMarkerContent()), IResource.NONE, monitor);
		return file;
	}

	private void assertProjectIsOpen(IProject project) throws CoreException {
		if (!project.exists()) {
			throw new CoreException(OpenShiftUIActivator.createErrorStatus(NLS.bind("Project {0} does not exist", project.getName())));
		}
		if (!project.isOpen()) {
			throw new CoreException(OpenShiftUIActivator.createErrorStatus(NLS.bind("Project {0} is not opened", project.getName())));
		}
	}

	protected abstract String getMarkerName();
	
	protected abstract byte[] getMarkerContent();
	
}
