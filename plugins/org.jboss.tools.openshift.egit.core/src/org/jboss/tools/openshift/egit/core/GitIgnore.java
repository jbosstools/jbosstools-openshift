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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.Constants;

/**
 * @author André Dietisheim
 */
public class GitIgnore {

	public static final String NL = System.getProperty("line.separator");
			
	private Set<String> entries;
	private File file;

	public GitIgnore(IProject project) throws IOException {
		this(new File(project.getLocation().toFile(), Constants.GITIGNORE_FILENAME));
	}

	public GitIgnore(File gitIgnoreFile) throws IOException {
		this.file = gitIgnoreFile;
		initEntries(gitIgnoreFile);
	}

	private void initEntries(File gitIgnore) throws IOException {
		this.entries = new HashSet<String>();
		if (gitIgnore == null
				|| !gitIgnore.canRead()) {
			return;
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(gitIgnore));
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
	 * and existing file if the given overwrite is set to <code>true</code>,
	 * appends otherwise.
	 * 
	 * @param overwrite
	 *            overwrites an existing file if <code>true</code>, appends
	 *            otherwise
	 * @throws IOException
	 */
	public void write(boolean overwrite) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, !overwrite));
		try {
			for (String entry : entries) {
				writer.write(entry);
				writer.write(NL);
			}
			writer.flush();
		} finally {
			writer.close();
		}
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
