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
package org.jboss.tools.openshift.egit.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.Constants;

/**
 * @author André Dietisheim
 */
public class GitIgnore {

	public static final String NL = System.getProperty("line.separator");

	private Set<String> entries;
	private IFile file;

	public GitIgnore(IProject project) throws IOException, CoreException {
		this(project.getFile(Constants.GITIGNORE_FILENAME));
	}

	public GitIgnore(IFile gitIgnoreFile) throws IOException, CoreException {
		this.file = gitIgnoreFile;
		initEntries(gitIgnoreFile);
	}

	private void initEntries(IFile gitIgnore) throws IOException, CoreException {
		this.entries = new HashSet<String>();
		if (gitIgnore == null
				|| !gitIgnore.isAccessible()) {
			return;
		}
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new InputStreamReader(gitIgnore.getContents()));
			for (String line = null; (line = reader.readLine()) != null;) {
				if (line != null) {
					entries.add(line);
				}
			}
		} finally {
			safeClose(reader);
		}
	}

	public GitIgnore add(String entry) {
		this.entries.add(entry);
		return this;
	}

	public boolean contains(String entry) {
		return entries.contains(entry);
	}

	public int size() {
		return entries.size();
	}

	/**
	 * Writes the entries in this instance to the .gitignore file. Overwrites
	 * and existing file
	 * 
	 * @throws IOException
	 * @throws CoreException
	 */
	public IFile write(IProgressMonitor monitor) throws CoreException {
		StringBuilder builder = new StringBuilder();
		for (String entry : entries) {
			builder.append(entry);
			builder.append(NL);
		}
		file.setContents(new ByteArrayInputStream(builder.toString().getBytes()), IResource.FORCE, monitor);
		return file;
	}

	public boolean exists() {
		return file != null
				&& file.exists();
	}

	private void safeClose(Reader reader) {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		} catch (IOException e) {
			// swallow
		}
	}
}
