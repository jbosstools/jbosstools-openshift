/*******************************************************************************
 * Copyright (c) 2011-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.application.importoperation;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIMessages;

/**
 * @author Andre Dietisheim <adietish@redhat.com>
 * @author Jeff Maury
 * 
 */
public class GeneralProjectImportOperation extends AbstractProjectImportOperation {

	public GeneralProjectImportOperation(File projectDirectory) {
		super(projectDirectory);
	}

	public List<IProject> importToWorkspace(IProgressMonitor monitor)
			throws CoreException, InterruptedException {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(getProjectDirectory().getName());
		if (overwriteExistingProject(project, workspace, monitor)) {
	        importToWorkspace(getProjectDirectory(), workspace, monitor);
		}
		return Collections.singletonList(project);
	}

	private void importToWorkspace(File projectDirectory, IWorkspace workspace, IProgressMonitor monitor)
			throws CoreException {
		String projectName = projectDirectory.getName();
		IProjectDescription description = workspace.newProjectDescription(projectName);
		description.setLocation(Path.fromOSString(projectDirectory.getAbsolutePath()));
		IProject project = workspace.getRoot().getProject(projectName);
		project.create(description, monitor);
		project.open(IResource.BACKGROUND_REFRESH, monitor);
	}

	private boolean overwriteExistingProject(final IProject project, IWorkspace workspace, IProgressMonitor monitor)
			throws CoreException {
		if (project == null
				|| !project.exists()) {
			return true;
		}

		final boolean overwrite = displayOverwriteDialog(OpenShiftCommonUIMessages.OverwriteProjectsDialogTitle,
		                                                 NLS.bind(OpenShiftCommonUIMessages.GeneralProjectWarningMessage, project.getName()));
		if (overwrite) {
			project.delete(false, true, monitor);
		}
		return overwrite;
	}
}
