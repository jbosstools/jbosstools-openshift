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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftProjectUtils;

/**
 * 
 * @author Andre Dietisheim
 * 
 */
public class BaseOpenShiftMarker implements IOpenShiftMarker {

	private String name;
	private String fileName;
	private String description;

	protected BaseOpenShiftMarker(String fileName, String name, String description) {
		this.name = name;
		this.fileName = fileName;
		this.description = description;
	}

	@Override
	public IFile addTo(IProject project, IProgressMonitor monitor) throws CoreException {
		assertProjectIsOpen(project);
		IFile file = getMarkerFile(project, monitor);
		if (file.isAccessible()) {
			return null;
		}
		file.create(new ByteArrayInputStream(new byte[] {}), IResource.NONE, monitor);
		return file;
	}

	@Override
	public IFile removeFrom(IProject project, IProgressMonitor monitor) throws CoreException {
		assertProjectIsOpen(project);
		IFile file = getMarkerFile(project, monitor);
		if (!file.isAccessible()) {
			return null;
		}
		file.delete(false, monitor);
		return file;
	}

	private IFile getMarkerFile(IProject project, IProgressMonitor monitor) throws CoreException {
		IFolder markersFolder = OpenShiftProjectUtils.getMarkersFolder(project);
		OpenShiftProjectUtils.ensureMarkersFolderExists(project, monitor);
		return markersFolder.getFile(getFileName());
	}

	@Override
	public boolean existsIn(IProject project, IProgressMonitor monitor) throws CoreException {
		IFile file = getMarkerFile(project, monitor);
		return file.exists();
	}

	@Override
	public boolean matchesFilename(String fileName) {
		return this.fileName.equals(fileName);
	}

	private void assertProjectIsOpen(IProject project) throws CoreException {
		if (!project.exists()) {
			throw new CoreException(ExpressCoreActivator.statusFactory().errorStatus(NLS.bind("Project {0} does not exist",
					project.getName())));
		}
		if (!project.isOpen()) {
			throw new CoreException(ExpressCoreActivator.statusFactory().errorStatus(NLS.bind("Project {0} is not opened",
					project.getName())));
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "BaseOpenShiftMarker ["
				+ "name=" + name
				+ ", fileName=" + fileName
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
		BaseOpenShiftMarker other = (BaseOpenShiftMarker) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}

}
