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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.junit.Test;

/**
 * @author André Dietisheim
 */
public class GitIgnoreTest {

	public static final String NL = System.getProperty("line.separator");
			
	private File gitIgnoreFile;

	public void setUp() throws IOException {
		this.gitIgnoreFile = createGitFile("");
	}

	public void tearDown() {
		gitIgnoreFile.delete();
	}

	@Test
	public void canAddEntries() throws IOException {
		GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
		assertTrue(gitIgnore.size() == 0);
		String entry = "dummy";
		gitIgnore.add(entry);
		assertTrue(gitIgnore.size() == 1);
		assertTrue(gitIgnore.contains(entry));
	}

	@Test
	public void canParseExistingEntries() throws IOException {
		File gitIgnoreFile = null;
		try {
			gitIgnoreFile = createGitFile("redhat", "jboss", "tools");
			GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
			assertTrue(gitIgnore.size() == 3);
			assertTrue(gitIgnore.contains("redhat"));
			assertTrue(gitIgnore.contains("jboss"));
			assertTrue(gitIgnore.contains("tools"));
		} finally {
			if (gitIgnoreFile != null) {
				gitIgnoreFile.delete();
			}
		}
	}

	@Test
	public void entryIsNotAddedTwice() throws IOException {
		File gitIgnoreFile = null;
		try {
			gitIgnoreFile = createGitFile("redhat");
			GitIgnore gitIgnore = new GitIgnore(gitIgnoreFile);
			assertTrue(gitIgnore.size() == 1);
			assertTrue(gitIgnore.contains("redhat"));
			gitIgnore.add("redhat");
			assertTrue(gitIgnore.size() == 1);
		} finally {
			gitIgnoreFile.delete();
		}
	}

	private File createTmpFolder() throws IOException {
		String tmpFolder = System.getProperty("java.io.tmpdir");
		String currentTime = String.valueOf(System.currentTimeMillis());
		File randomTmpFolder = new File(tmpFolder, currentTime);
		randomTmpFolder.mkdir();
		return randomTmpFolder;

	}

	private File createGitFile(String... gitIgnoreEntries) throws IOException {
		File gitFile = new File(createTmpFolder(), Constants.GITIGNORE_FILENAME);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(gitFile));
			for (String entry : gitIgnoreEntries) {
				writer.write(entry);
				writer.write(NL);
			}
			writer.flush();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		return gitFile;
	}
}
