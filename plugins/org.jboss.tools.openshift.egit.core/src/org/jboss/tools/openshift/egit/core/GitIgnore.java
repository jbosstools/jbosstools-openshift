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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Constants;

public class GitIgnore {

	private List<String> entries;
	private File baseDir;

	public GitIgnore(File baseDir) {
		this.baseDir = baseDir;
		this.entries = new ArrayList<String>();
	}

	public GitIgnore add(String entry) {
		this.entries.add(entry);
		return this;
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
		File file = getFile();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, !overwrite));
		try {
			for (String entry : entries) {
				out.write(entry.getBytes());
				out.write('\n');
			}
			out.flush();
		} finally {
			out.close();
		}
	}

	public boolean exists() {
		return getFile().exists();
	}
	
	private File getFile() {
		File file = new File(baseDir, Constants.GITIGNORE_FILENAME);
		return file;
	}
}
