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
package org.jboss.tools.openshift.express.internal.core.util;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

public class OpenShiftProjectUtils {

	private static final String FOLDER_DOT_OPENSHIFT = ".openshift";
	private static final String FOLDER_MARKERS = FOLDER_DOT_OPENSHIFT + File.separatorChar + "markers";

	public static boolean isOpenShiftProject(IProject project) {
		IFolder folder = getOpenShiftFolder(project);
		return folder != null
				&& folder.isAccessible();
	}
	
	public static IFolder getOpenShiftFolder(IProject project) {
		return project.getFolder(FOLDER_DOT_OPENSHIFT);

	}

	public static IFolder getMarkersFolder(IProject project) {
		return project.getFolder(new Path(FOLDER_MARKERS));
	}

	public static IFolder ensureMarkersFolderExists(IProject project, IProgressMonitor monitor) throws CoreException {
		ensureExists(getOpenShiftFolder(project), monitor);
		return ensureExists(getMarkersFolder(project), monitor);
	}

	private static IFolder ensureExists(IFolder folder, IProgressMonitor monitor) throws CoreException {
		if (!folder.isAccessible()) {
			folder.create(false, true, monitor);
		}
		return folder;

	}
	
}
