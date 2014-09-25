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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftProjectUtils;
import org.jboss.tools.openshift.express.internal.core.util.ResourceUtils;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftMarkers {

	private IProject project;
	private Collection<IOpenShiftMarker> allKnownMarkers;

	public OpenShiftMarkers(IProject project) {
		this.project = project;
		this.allKnownMarkers = new ArrayList<IOpenShiftMarker>();
		allKnownMarkers.add(IOpenShiftMarker.DISABLE_AUTO_SCALING);
		allKnownMarkers.add(IOpenShiftMarker.ENABLE_JPA);
		allKnownMarkers.add(IOpenShiftMarker.FORCE_CLEAN_BUILD);
		allKnownMarkers.add(IOpenShiftMarker.HOT_DEPLOY);
		allKnownMarkers.add(IOpenShiftMarker.JAVA_7);
		allKnownMarkers.add(IOpenShiftMarker.SKIP_MAVEN_BUILD);

	}

	/**
	 * Returns all possible markers for the given project. The method returns
	 * the markers it knows about and the unknown ones found in the given
	 * project.
	 * 
	 * @return all possible markers for the given project.
	 * @throws CoreException
	 * 
	 * @see IProject
	 * @see IOpenShiftMarker
	 * 
	 */
	public List<IOpenShiftMarker> getAll() throws CoreException {
		final List<IOpenShiftMarker> allMarkers = new ArrayList<IOpenShiftMarker>();
		allMarkers.addAll(getAllKnownMarkers());
		final IFolder folder = OpenShiftProjectUtils.getMarkersFolder(project);
		if (folder != null
				&& folder.isAccessible()) {
			folder.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					// visit markers folder
					if (resource == folder) {
						return true;
					}
					// dont visit markers within markers folder
					if (resource.getType() != IResource.FILE) {
						return false;
					}
					if (startsWithDot(resource)) {
						return false;
					}
					if (isReadme(resource)) {
						return false;
					}
					if (!isKnownMarker(resource.getName())) {
						allMarkers.add(createUnknownMarker(resource));
					}
					return false;
				}

			}, IResource.DEPTH_ONE, false);
		}

		return allMarkers;
	}

	/**
	 * Returns all markers found in the given project.
	 * 
	 * @return
	 * @throws CoreException
	 */
	public List<IOpenShiftMarker> getPresent() throws CoreException {
		final List<IOpenShiftMarker> allMarkers = new ArrayList<IOpenShiftMarker>();
		final IFolder folder = OpenShiftProjectUtils.getMarkersFolder(project);
		if (ResourceUtils.exists(folder)) {
			folder.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					// visit markers folder
					if (resource == folder) {
						return true;
					}
					// dont visit markers within markers folder
					if (resource.getType() != IResource.FILE) {
						return false;
					}
					// exclude dot-files (.gitignore etc.)
					if (startsWithDot(resource)) {
						return false;
					} 
					// exclude README
					if (isReadme(resource)) {
						return false;
					}
					allMarkers.add(getMarker(resource));
					return false;
				}
			}, IResource.DEPTH_ONE, false);
		}

		return allMarkers;
	}

	private boolean startsWithDot(IResource resource) {
		if (!ResourceUtils.exists(resource)) {
			return false;
		}
		return resource.getName().startsWith(".");
	}
	
	private boolean isReadme(IResource resource) {
		if (!ResourceUtils.exists(resource)) {
			return false;
		}
		return "README.MD".equalsIgnoreCase(resource.getName());
	}
	
	private IOpenShiftMarker getMarker(IResource resource) {
		IOpenShiftMarker marker = getKnownMarker(resource.getName());
		if (marker == null) {
			marker = createUnknownMarker(resource);
		}
		return marker;
	}

	private IOpenShiftMarker createUnknownMarker(IResource resource) {
		return new BaseOpenShiftMarker(resource.getName(), resource.getName(), null);
	}

	private boolean isKnownMarker(String fileName) {
		return getKnownMarker(fileName) != null;
	}

	private IOpenShiftMarker getKnownMarker(String fileName) {
		for (IOpenShiftMarker marker : getAllKnownMarkers()) {
			if (marker.getFileName().equals(fileName)) {
				return marker;
			}
		}
		return null;
	}

	private Collection<IOpenShiftMarker> getAllKnownMarkers() {
		return allKnownMarkers;
	}

}
