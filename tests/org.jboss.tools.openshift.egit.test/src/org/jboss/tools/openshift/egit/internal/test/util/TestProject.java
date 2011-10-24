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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.util.FileUtils;

public class TestProject {
	public IProject project;

	private String location;
	private TestUtils testUtils = new TestUtils();

	/**
	 * @throws CoreException
	 *             If project already exists
	 */
	public TestProject() throws CoreException {
		this(false);
	}

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

	public IFile createFile(String name, byte[] content) throws Exception {
		IFile file = project.getFile(name);
		InputStream inputStream = new ByteArrayInputStream(content);
		file.create(inputStream, true, null);

		return file;
	}

	public IFolder createFolder(String name) throws Exception {
		IFolder folder = project.getFolder(name);
		folder.create(true, true, null);

		IFile keep = project.getFile(name + "/keep");
		keep.create(new ByteArrayInputStream(new byte[] {0}), true, null);

		return folder;
	}

	public void dispose() throws CoreException, IOException {
		if (project.exists())
			project.delete(true, true, null);
		else {
			File f = new File(location);
			if (f.exists())
				FileUtils.delete(f, FileUtils.RECURSIVE | FileUtils.RETRY);
		}
	}

	public String getFileContent(String filepath) throws Exception {
		IFile file = project.getFile(filepath);
		InputStream stream = file.getContents();
		return testUtils.slurpAndClose(stream);
	}
}
