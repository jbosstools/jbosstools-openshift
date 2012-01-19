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
package org.jboss.tools.openshift.egit.internal.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.lib.Constants;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.junit.Test;

/**
 * @author André Dietisheim
 */
public class GitIgnoreTest {

	public static final String NL = System.getProperty("line.separator");

	private IFile gitIgnoreFile;

	private IProject project;

	public void setUp() throws CoreException {
		this.project = createRandomProject();
		this.gitIgnoreFile = createGitFile(project, "");
	}

	public void tearDown() {
		silentlyDelete(project);
	}

	@Test
	public void canAddEntries() throws CoreException, IOException {
		GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
		assertTrue(gitIgnore.size() == 0);
		String entry = "dummy";
		gitIgnore.add(entry);
		assertTrue(gitIgnore.size() == 1);
		assertTrue(gitIgnore.contains(entry));
	}

	@Test
	public void canParseExistingEntries() throws CoreException, IOException {
		IFile gitIgnoreFile = null;
		IProject project = null;
		try {
			project = createRandomProject();
			gitIgnoreFile = createGitFile(project, "redhat", "jboss", "tools");
			GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
			assertTrue(gitIgnore.size() == 3);
			assertTrue(gitIgnore.contains("redhat"));
			assertTrue(gitIgnore.contains("jboss"));
			assertTrue(gitIgnore.contains("tools"));
		} finally {
			silentlyDelete(project);
		}
	}

	@Test
	public void entryIsNotAddedTwice() throws CoreException, IOException {
		IFile gitIgnoreFile = null;
		IProject project = null;
		try {
			project = createRandomProject();
			gitIgnoreFile = createGitFile(project, "redhat");
			GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
			assertTrue(gitIgnore.size() == 1);
			assertTrue(gitIgnore.contains("redhat"));
			gitIgnore.add("redhat");
			assertTrue(gitIgnore.size() == 1);
		} finally {
			silentlyDelete(project);
		}
	}

	private IFile createGitFile(IProject project, String... gitIgnoreEntries) throws CoreException {
		IFile gitFile = project.getFile(Constants.GITIGNORE_FILENAME);
		StringBuilder builder = new StringBuilder();
		for (String entry : gitIgnoreEntries) {
			builder.append(entry);
			builder.append(NL);
		}
		gitFile.create(new ByteArrayInputStream(builder.toString().getBytes()), IResource.FORCE, null);
		return gitFile;
	}

	private IProject createRandomProject() throws CoreException {
		String projectName = String.valueOf(System.currentTimeMillis());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.create(null);
		project.open(null);
		return project;
	}

	private void silentlyDelete(IProject project) {
		if (project == null
				|| !project.isAccessible()) {
			return;
		}
		try {
			project.close(null);
			project.delete(true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
