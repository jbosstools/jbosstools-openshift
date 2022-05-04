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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
	 * @param path
	 * @throws CoreException
	 */
	public TestProject(final boolean remove, String path) throws CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProjectDescription description = createDescription(path, root);
		project = root.getProject(description.getName());
		if (remove) {
			TestUtils.deleteProject(project);
		}
		location = root.getRawLocation().append(path).toOSString();
		project.create(description, monitor);
		project.open(monitor);
	}

	private IProjectDescription createDescription(String path, IWorkspaceRoot root) {
		Path ppath = new Path(path);
		String projectName = ppath.lastSegment();
		IProjectDescription description = ResourcesPlugin.getWorkspace()
				.newProjectDescription(projectName);

		description.setName(projectName);
		return description;
	}

	public IProject getProject() {
		return project;
	}

	public void dispose() throws CoreException, IOException {
		try {
			if (project.exists()) {
				TestUtils.deleteProject(project);
			} else {
				File f = new File(location);
				if (f.exists()) {
					FileUtils.delete(f, FileUtils.RECURSIVE | FileUtils.RETRY);
				}
			}
		} catch (CoreException | IOException e) {
			System.err.println(e.toString());
			TestUtils.listDirectory(new File(location), true);
			throw e;
		}
	}

	public String getLocation() {
		return location;
	}
	public IFile getFile(String filepath) throws Exception {
		return project.getFile(filepath);
	}
}
