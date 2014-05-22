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
package org.jboss.tools.openshift.express.internal.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;

/**
 * @author André Dietisheim
 */
public class FileUtils {

	private static final char SUFFIX_DELIMITER = '.';
	private static final String NUMERIC_SUFFIX_PATTERN = "{0}({1}){2}";
	
	private static final byte[] buffer = new byte[1024];


	public static boolean canRead(String path) {
		if (path == null) {
			return false;
		}
		return canRead(new File(path));
	}

	public static boolean canRead(File file) {
		if (file == null) {
			return false;
		}
		return file.canRead();
	}

	public static boolean exists(File file) {
		return file != null
				&& file.exists();
	}

	public static boolean isDirectory(File file) {
		return file != null
				&& file.isDirectory();
	}

	public static File getSystemTmpFolder() {
		String tmpFolder = System.getProperty("java.io.tmpdir");
		return new File(tmpFolder);
	}
	
	public static File getRandomTmpFolder() {
		String randomName = String.valueOf(System.currentTimeMillis());
		return new File(getSystemTmpFolder(), randomName);
	}
	
	/**
	 * Copies the ginve source to the given destination recursively. Overwrites
	 * existing files/directory on the destination path if told so.
	 * 
	 * @param source
	 *            the source file/directory to copy
	 * @param destination
	 *            the destination to copy to
	 * @param overwrite
	 *            overwrites existing files/directories if <code>true</code>.
	 *            Does not overwrite otherwise.
	 * @throws IOException
	 */
	public static void copy(File source, File destination, boolean overwrite) throws IOException {
		if (!exists(source)
				|| destination == null) {
			return;
		}

		if (source.isDirectory()) {
			copyDirectory(source, destination, overwrite);
		} else {
			copyFile(source, destination, overwrite);
		}
	}

	private static void copyDirectory(File source, File destination, boolean overwrite) throws IOException {
		Assert.isLegal(source != null);
		Assert.isLegal(source.isDirectory());
		Assert.isLegal(destination != null);

		destination = getDestinationDirectory(source, destination);
		
		if (!destination.exists()) {
			destination.mkdir();
			copyPermissions(source, destination);
		}

		for (File content : source.listFiles()) {
			if (content.isDirectory()) {
				copyDirectory(content, new File(destination, content.getName()), overwrite);
			} else {
				copyFile(content, new File(destination, content.getName()), overwrite);
			}
		}
	}

	private static File getDestinationDirectory(File source, File destination) {
		if (!source.getName().equals(destination.getName())) {
			destination = new File(destination, source.getName());
		}
		return destination;
	}

	private static void copyFile(File source, File destination, boolean overwrite) throws IOException {
		Assert.isLegal(source != null);
		Assert.isLegal(source.isFile());
		Assert.isLegal(destination != null);

		destination = getDestinationFile(source, destination);
		
		if (exists(destination)
				&& !overwrite) {
			return;
		}

		if (isDirectory(destination)) {
			if (!overwrite) {
				return;
			}
			destination.delete();
		}

		writeTo(source, destination);
	}

	private static File getDestinationFile(File source, File destination) {
		if (!source.getName().equals(destination.getName())) {
			destination = new File(destination, source.getName());
		}
		return destination;
	}

	private static final void writeTo(File source, File destination) throws IOException {
		Assert.isLegal(source != null);
		Assert.isLegal(destination != null);

		writeTo(new BufferedInputStream(new FileInputStream(source)), destination);
		copyPermissions(source, destination);
	}

	public static final void writeTo(String content, File destination) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(destination);
		writer.write(content);
		writer.flush();
		writer.close();
	}

	private static final void writeTo(InputStream in, File destination) throws IOException {
		Assert.isLegal(in != null);
		Assert.isLegal(destination != null);

		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(destination));
			for (int read = -1; (read = in.read(buffer)) != -1; ) {
				out.write(buffer, 0, read);
			}
			out.flush();
		} finally {
			silentlyClose(in);
			silentlyClose(out);
		}
	}
	
	/**
	 * Replicates the owner permissions from the source to the destination. Due
	 * to limitation in java6 this is the best we can do (there's no way in
	 * java6 to know if rights are due to owner or group)
	 * 
	 * @param source
	 * @param destination
	 * 
	 * @see File#canRead()
	 * @see File#setReadable(boolean)
	 * @see File#canWrite()
	 * @see File#setWritable(boolean)
	 * @see File#canExecute()
	 * @see File#setExecutable(boolean)
	 */
	private static void copyPermissions(File source, File destination) {
		Assert.isLegal(source != null);
		Assert.isLegal(destination != null);

		destination.setReadable(source.canRead());
		destination.setWritable(source.canWrite());
		destination.setExecutable(source.canExecute());
	}

	private static void silentlyClose(InputStream in) {
		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private static void silentlyClose(OutputStream out) {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}
	
	public static String createValidNumericSuffix(String filename) {
		String suffix = getSuffix(filename);
		String filenameNoSuffix = getNoSuffixFilename(filename);
		
		String newFilename = filename;
		int i = 1;
		while(new File(newFilename).exists()) {
			newFilename = MessageFormat.format(NUMERIC_SUFFIX_PATTERN, filenameNoSuffix, i++, suffix);
		}
		return newFilename;
	}

	public static String getSuffix(String filename) {
		if (StringUtils.isEmpty(filename)) {
			return filename;
		}
		int suffixStart = filename.indexOf(SUFFIX_DELIMITER);
		if ( suffixStart < 0) {
			return filename;
		}
		return filename.substring(suffixStart);
	}

	/**
	 * Returns the filename without the suffix
	 * 
	 * @param filename
	 * @return
	 */
	public static String getNoSuffixFilename(String filename) {
		if (StringUtils.isEmpty(filename)) {
			return filename;
		}
		int suffixStart = filename.indexOf(SUFFIX_DELIMITER);
		if ( suffixStart < 0) {
			return filename;
		}
		return filename.substring(0, suffixStart);
	}
}
