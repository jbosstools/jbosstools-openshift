/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Andre Dietisheim <adietish@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jboss.tools.openshift.egit.internal.test.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.util.FileUtils;

public class TestProject {
	public IProject project;

	private String location;

	public TestProject(boolean remove) throws CoreException {
		this(remove, "Project-" + System.currentTimeMillis());
	}

	/**
	 * @param remove
	 *            should project be removed if already exists
	 * @param projectName
	 * @throws CoreException
	 */
	public TestProject(final boolean remove, String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(projectName);
		if (remove)
			project.delete(true, null);
		project.create(null);
		project.open(null);
		location = project.getLocation().toOSString();
	}

	public IProject getProject() {
		return project;
	}

	public void dispose(IProgressMonitor monitor) throws CoreException, IOException {
		if (project.exists()) {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			project.close(monitor);
			project.delete(true, true, monitor);
		}
		File f = new File(location);
		if (f.exists())
			FileUtils.delete(f, FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.SKIP_MISSING);
	}

	public IFile getFile(String filepath) throws Exception {
		return project.getFile(filepath);
	}
}
