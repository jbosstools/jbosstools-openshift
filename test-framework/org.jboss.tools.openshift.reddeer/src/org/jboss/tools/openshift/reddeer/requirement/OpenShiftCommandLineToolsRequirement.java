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
package org.jboss.tools.openshift.reddeer.requirement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.utils.FileHelper;

/**
 * Requirement to download and extract OpenShift command line tools binary which is necessary 
 * for some functionality of OpenShift tools.
 *  
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 *
 */
public class OpenShiftCommandLineToolsRequirement implements Requirement<OCBinary> {

	private static final String CLIENT_TOOLS_DESTINATION = "binaries";
	private static final String SUFFIX_TAR_GZ = ".tar.gz";
	private static final String SUFFIX_ZIP = ".zip";
	
	private static final Logger LOGGER = new Logger(OpenShiftCommandLineToolsRequirement.class);
	
	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface OCBinary {
    }

	@Override
	public boolean canFulfill() {
		return true;
	}

	@Override
	public void fulfill() {
		if (!OCBinaryFile.get().getFile().exists()) {
			// symlink does not exist or exists but points to inexistent file
			File downloadedOCBinary = downloadAndExtractOpenShiftClient();
			createSymlink(downloadedOCBinary);
		} else {
			LOGGER.info("Binary is already downloaded.");
		}
	}

	private void createSymlink(File downloadedOCBinary) {
		try {
			Files.deleteIfExists(Paths.get(OCBinaryFile.get().getFile().toURI()));
			Files.createSymbolicLink(OCBinaryFile.get().getFile().toPath(), Paths.get(downloadedOCBinary.getAbsolutePath()));
		} catch (IOException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not symlink {0} to {1}:\n{2}", 
					new Object[] { OCBinaryFile.get().getFile().getAbsolutePath(), downloadedOCBinary.getAbsolutePath(), e }));
		}
	}

	@Override
	public void setDeclaration(OCBinary declaration) {}

	@Override
	public void cleanUp() {}
	
	public static String getOCLocation() {
		return OCBinaryFile.get().getFile().getAbsolutePath();
	}
	
	private File downloadAndExtractOpenShiftClient() {
		LOGGER.info("Creating directory binaries");
		File outputDirectory = new File(CLIENT_TOOLS_DESTINATION);
		FileHelper.createDirectory(outputDirectory);

		String fileName = downloadArchive(getDownloadLink());
		String extractedDirectory = extractArchive(fileName, outputDirectory);

		if (StringUtils.isEmpty(extractedDirectory)
			|| !(new File(extractedDirectory).exists())) {
				throw new OpenShiftToolsException("Cannot extract archive " + fileName + ". "
						+ "Archive does not extract into a single root folder.");
		}

		return new File(extractedDirectory, OCBinaryFile.get().getName());
	}

	private String downloadArchive(String downloadLink) {
		if (StringUtils.isEmpty(downloadLink)) {
			throw new OpenShiftToolsException("Cannot download OpenShift binary. No download known\n");
		}

		String fileName = null;
		try {
			URL downloadUrl = new URL(downloadLink);
			fileName = getFileName(downloadUrl.getPath());
			if (new File(fileName).exists()) {
				return fileName;
			}
			try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
					ReadableByteChannel readableByteChannel = Channels.newChannel(downloadUrl.openStream())) {
				LOGGER.info("Downloading OpenShift binary.");
				fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			} catch (IOException ex) {
				throw new OpenShiftToolsException("Cannot download OpenShift binary.\n" + ex.getMessage());
			}
		} catch (MalformedURLException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not download \"{0}\". Invalid url.", downloadLink));
		}
		return fileName;
	}

	private String extractArchive(String fileName, File outputDirectory) {
		if (StringUtils.isEmpty(fileName)) {
			return null;
		}

		LOGGER.info(NLS.bind("Extracting OpenShift archive {0}.", fileName));
		String extractedDirectory = null;
		if (fileName.endsWith(SUFFIX_ZIP)) {
			extractedDirectory = StringUtils.chomp(fileName, SUFFIX_ZIP);
			FileHelper.unzipFile(new File(fileName), outputDirectory);
		} else if (fileName.endsWith(SUFFIX_TAR_GZ)) {
			extractedDirectory = StringUtils.chomp(fileName, SUFFIX_TAR_GZ);
			FileHelper.extractTarGz(new File(fileName), outputDirectory);
		}

		return extractedDirectory;
	}
	
	private String getFileName(String urlPath) {
		String[] pathParts = urlPath.split("/");
		return Paths.get(CLIENT_TOOLS_DESTINATION, pathParts[pathParts.length - 1]).toString();
	}
	
	private String getDownloadLink() {
		if (Platform.OS_LINUX.equals(Platform.getOS())) {
			if (Platform.getOSArch().equals(Platform.ARCH_X86)) {
				return ClientVersion.LINUX_1_3_32.getDownloadLink();
			} else { 
				return ClientVersion.LINUX_1_3_64.getDownloadLink();
			}
		} else if (Platform.OS_WIN32.equals(Platform.getOSArch())){
			return ClientVersion.WINDOWS_1_3_64.getDownloadLink();
		} else if (Platform.OS_MACOSX.equals(Platform.getOS())){
			return ClientVersion.MAC_1_3.getDownloadLink();
		} else {
			return null;
		}
	}
	
	public enum ClientVersion {
		LINUX_1_1_32("https://github.com/openshift/origin/releases/download/"
				+ "v1.1/openshift-origin-v1.1-ac7a99a-linux-386.tar.gz"),
		LINUX_1_1_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.1/openshift-origin-v1.1-ac7a99a-linux-amd64.tar.gz"),
		WINDOWS_1_1_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.1/openshift-origin-v1.1-ac7a99a-windows-amd64.zip"),
		
		LINUX_1_2_32("https://github.com/openshift/origin/releases/download/"
				+ "v1.2.0/openshift-origin-client-tools-v1.2.0-2e62fab-linux-32bit.tar.gz"),
		LINUX_1_2_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.2.0/openshift-origin-client-tools-v1.2.0-2e62fab-linux-64bit.tar.gz"),
		WINDOWS_1_2_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.2.0/openshift-origin-client-tools-v1.2.0-2e62fab-windows.zip"),
		MAC_1_2("https://github.com/openshift/origin/releases/download/" 
				+ "v1.2.0/openshift-origin-client-tools-v1.2.0-2e62fab-mac.zip"),
		
		LINUX_1_3_32("https://github.com/openshift/origin/releases/download/"
				+ "v1.3.2/openshift-origin-client-tools-v1.3.2-ac1d579-linux-32bit.tar.gz"),
		LINUX_1_3_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.3.2/openshift-origin-client-tools-v1.3.2-ac1d579-linux-64bit.tar.gz"),
		WINDOWS_1_3_64("https://github.com/openshift/origin/releases/download/"
				+ "v1.3.2/openshift-origin-client-tools-v1.3.2-ac1d579-windows.zip"),
		MAC_1_3("https://github.com/openshift/origin/releases/download/"
				+ "v1.3.2/openshift-origin-client-tools-v1.3.2-ac1d579-mac.zip");
		
		String url;
		
		private ClientVersion(String url) {
			this.url = url;
		}
		
		public String getDownloadLink() {
			return url;
		}		
	}

	public enum OCBinaryFile {
		LINUX("oc"), 
		MAC("oc"),
		WINDOWS("oc.exe");

		private String name;

		private OCBinaryFile(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public File getFile() {
			return new File(CLIENT_TOOLS_DESTINATION, getName());
		}
		
		public static OCBinaryFile get() {
			if (Platform.OS_LINUX.equals(Platform.getOS())) {
				return LINUX;
			} else if (Platform.OS_MACOSX.equals(Platform.getOS())) {
				return MAC;
			} else if (Platform.OS_WIN32.equals(Platform.getOS())) {
				return WINDOWS;
			} else {
				throw new OpenShiftToolsException("Could not determine oc binary name. Unknown operating system.");
			}

		}
	}
}
