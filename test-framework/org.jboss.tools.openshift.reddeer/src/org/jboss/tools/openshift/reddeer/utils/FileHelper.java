/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;

public class FileHelper {

	private static Logger logger = new Logger(FileHelper.class);
	
	public static void extractTarGz(File archive, File outputDirectory) {
		InputStream inputStream = null;
		try {
			logger.info("Opening stream to gzip archive");
			inputStream = new GzipCompressorInputStream(new FileInputStream(archive));
		} catch (IOException ex) {
			throw new OpenShiftToolsException("Exception occured while processing tar.gz file.\n" + ex.getMessage());
		}
		
		logger.info("Opening stream to tar archive");
		BufferedOutputStream outputStream = null;
		TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
		TarArchiveEntry currentEntry = null;
		try {
			while ((currentEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
				if (currentEntry.isDirectory()) {
					logger.info("Creating directory: " + currentEntry.getName());
					createDirectory(new File(outputDirectory, currentEntry.getName()));
				} else {
					File outputFile = new File(outputDirectory, currentEntry.getName());
	            	if (!outputFile.getParentFile().exists()) {
	            		logger.info("Creating directory: " + outputFile.getParentFile());
	            		createDirectory(outputFile.getParentFile());
	            	}
	            	
	            	outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
	            	
	            	logger.info("Extracting file: " + currentEntry.getName());
	            	copy(tarArchiveInputStream, outputStream, (int) currentEntry.getSize());
	            	outputStream.close();
	            	
	            	outputFile.setExecutable(true);
	        		outputFile.setReadable(true);
	        		outputFile.setWritable(true);
				}
			}
		} catch (IOException e) {
			throw new OpenShiftToolsException("Exception occured while processing tar.gz file.\n" + e.getMessage());
		} finally {
			try {
				tarArchiveInputStream.close();
			} catch (Exception ex) {}
			try {
				outputStream.close();
			} catch (Exception ex) {}
		}
	}
	
	public static void unzipFile(File zipArchive, File outputDirectory) {
		ZipFile zipfile = null;
		try {
			zipfile = new ZipFile(zipArchive);
		} catch (IOException ex) {
			throw new OpenShiftToolsException("Exception occured while processing zip file.\n" + ex.getMessage());
		}
		String extractedDirectory = StringUtils.chomp(zipArchive.getName(), ".zip");	
		Enumeration<? extends ZipEntry> entries = zipfile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			unzipEntry(zipfile, entry, new File(outputDirectory, extractedDirectory));
		}
	}
	
	private static void unzipEntry(ZipFile zipfile, ZipEntry entry,
			File outputDirectory) {

		if (entry.isDirectory()) {
			createDirectory(new File(outputDirectory, entry.getName()));
			return;
		}

		File outputFile = new File(outputDirectory, entry.getName());
		if (!outputFile.getParentFile().exists()) {
			createDirectory(outputFile.getParentFile());
		}

		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		try {
		inputStream = new BufferedInputStream(
				zipfile.getInputStream(entry));
		outputStream = new BufferedOutputStream(
				new FileOutputStream(outputFile));
		copy(inputStream, outputStream, 1024);
		} catch (IOException ex) {
		} finally {
			try {
				outputStream.close();
			} catch (Exception ex) {} 
			try {
				inputStream.close();
			} catch (Exception ex) {}
		}
		outputFile.setExecutable(true);
		outputFile.setReadable(true);
		outputFile.setWritable(true);
	}
	
	public static void createDirectory(File directory) {
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}
	
	public static void deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		directory.delete();
	}
	
	private static void copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}
	}
}
