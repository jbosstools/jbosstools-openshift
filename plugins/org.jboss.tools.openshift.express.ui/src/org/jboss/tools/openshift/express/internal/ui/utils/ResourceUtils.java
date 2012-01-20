/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author André Dietisheim
 */
public class ResourceUtils {

	public static boolean exists(IResource resource) {
		return resource != null
				&& resource.isAccessible();
	}

	public static boolean isDirectory(IResource resource) {
		return resource != null
				&& resource.getType() == IResource.FOLDER;
	}

	/**
	 * Copies the given source to the given destination recursively. Overwrites
	 * existing files/directory on the destination path if told so.
	 * 
	 * @param source
	 *            the source file/directory to copy
	 * @param destination
	 *            the destination to copy to
	 * @param overwrite
	 *            overwrites existing files/directories if <code>true</code>.
	 *            Does not overwrite otherwise.
	 * @throws IOException
	 * @throws CoreException
	 */
	public static void copy(IResource source, IResource destination, boolean overwrite, IProgressMonitor monitor)
			throws CoreException {
		if (!exists(source)
				|| destination == null) {
			return;
		}

		if (isDirectory(source)) {
			copyDirectory((IFolder) source, (IFolder) destination, overwrite, monitor);
		} else {
			copyFile((IFile) source, destination, overwrite, monitor);
		}
	}

	private static void copyDirectory(IFolder source, IFolder destination, final boolean overwrite,
			final IProgressMonitor monitor) throws CoreException {
		Assert.isLegal(isDirectory(source));
		Assert.isLegal(isDirectory(destination));

		final IFolder destinationFolder = getDestinationFolder(source, destination);

		if (!destinationFolder.exists()) {
			destinationFolder.create(getForceFlag(overwrite), true, monitor);
		}

		source.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (isDirectory(resource)) {
					copyDirectory((IFolder) resource, destinationFolder.getFolder(resource.getName()), overwrite,
							monitor);
				} else {
					copyFile((IFile) resource, destinationFolder.getFile(resource.getName()), overwrite, monitor);
				}
				return true;
			}
		});
	}

	private static IFolder getDestinationFolder(IFolder source, IFolder destination) {
		if (!source.getName().equals(destination.getName())) {
			destination = destination.getFolder(source.getName());
		}
		return destination;
	}

	private static void copyFile(IFile source, IResource destination, boolean overwrite, IProgressMonitor monitor)
			throws CoreException {
		Assert.isLegal(source != null);
		Assert.isLegal(destination != null);

		destination = getDestination(source, destination);

		if (exists(destination)
				&& !overwrite) {
			return;
		}

		if (isDirectory(destination)) {
			if (!overwrite) {
				return;
			}
			destination.delete(IResource.FORCE, monitor);
		}

		source.copy(destination.getFullPath(), getForceFlag(overwrite), monitor);
	}

	private static IResource getDestination(IFile source, IResource destination) {
		if (!source.getName().equals(destination.getName())) {
			if (destination.getType() == IResource.FOLDER) {
				destination = ((IFolder) destination).getFile(source.getName());
			}
		}
		return destination;
	}

	private static int getForceFlag(boolean overwrite) {
		if (overwrite) {
			return IResource.FORCE;
		} else {
			return IResource.NONE;
		}
	}

	/**
	 * Copies the given paths in the given sourceFolder (which may be located
	 * outside of the workspace - we're using java.io.File) to the given
	 * project. The copy operation is not using eclipse resource API, but it
	 * refreshes the project folder afterwards.
	 * 
	 * @param sourceFolder
	 *            the sourceFolder that contains the given path that shall be
	 *            copied
	 * @param sourcePaths
	 *            the paths within the source folder that shall be copied.
	 * @param project
	 *            the project that the sources shall be copied to.
	 * @return the freshly created resources.
	 * @throws IOException
	 * @throws CoreException 
	 */
	public static Collection<IResource> copy(File sourceFolder, String[] sourcePaths, IProject project,
			IProgressMonitor monitor) throws IOException, CoreException {
		List<IResource> resources = new ArrayList<IResource>();
		File projectFolder = project.getLocation().toFile();

		for (String sourcePath : sourcePaths) {
			File source = new File(sourceFolder, sourcePath);

			if (!FileUtils.canRead(source)) {
				continue;
			}

			FileUtils.copy(source, projectFolder, false);

			if (source.isDirectory()) {
				resources.add(project.getFolder(sourcePath));
			} else {
				resources.add(project.getFile(sourcePath));
			}
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		return resources;
	}
}
