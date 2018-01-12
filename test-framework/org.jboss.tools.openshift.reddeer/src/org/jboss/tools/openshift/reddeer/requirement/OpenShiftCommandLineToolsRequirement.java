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
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.FileHelper;
import org.jboss.tools.openshift.reddeer.utils.SystemProperties;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;

/**
 * Requirement to download and extract OpenShift command line tools binary which is necessary 
 * for some functionality of OpenShift and CDK tools. 
 * Loads up properties file with links to different oc versions.
 *  
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 * @author odockal@redhat.com
 *
 */
public class OpenShiftCommandLineToolsRequirement implements Requirement<OCBinary> {

	private static final Logger LOGGER = new Logger(OpenShiftCommandLineToolsRequirement.class);
	private static final String CLIENT_TOOLS_DESTINATION = "binaries";
	private static final String SUFFIX_TAR_GZ = ".tar.gz";
	private static final String SUFFIX_ZIP = ".zip";
	private static final Properties properties = TestUtils.readPropertiesFile(TestUtils.getProjectAbsolutePath("resources" + File.separator + "oclinks.properties"));
	private WorkbenchPreferenceDialog dialog;
	private OpenShift3PreferencePage page;
	private String pathToOC;
	private OCBinary binary;
	
	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface OCBinary {
		/**
		 * Decides whether existing oc on pathToOC path will be used.
		 * Is in exclusion with downloadNewestOC.
		 * @return false is default value
		 */
		boolean ocExists() default false;
		
		/**
		 * Path to existing oc, ie. one that is configured within minishift setup-cdk
		 * @return path to existing oc
		 */
		String pathToOC() default StringUtils.EMPTY;
		
		/**
		 * Sets if default oc version or newest will be downloaded
		 * @return true or false
		 */
		boolean latestOC() default true;
		
		/**
		 * If set to true, given oc will be set into 
		 * Preferences -> JBoss Tools -> OpenShift 3 -> oc executable location field
		 * @return true or false
		 */
		boolean setOCInPrefs() default false;
		
		/**
		 * Will delete downloaded  and copied oc if set to true, is not used when ocExists=true.
		 * Setting cleanup to false will result in keeping extracted and copied oc in predefined folder.
		 * Note that requirement is not able to find out version of such oc and
		 * could use the one already in folder instead of downloading new/old one as required.
		 * @return default true to delete resources - oc, false otherwise
		 */
		boolean cleanup() default true;
    }
	
	/**
	 * Returns string representation of path to proper oc
	 * @return
	 */
	public String getPathToOC() {
		return this.pathToOC;
	}

	@Override
	public void fulfill() {
		if (!binary.ocExists()) {
			if (!OCBinaryFile.get().getFile().exists()) {
				String url = getDownloadLink(binary.latestOC());
				LOGGER.info("OC binary will be downloaded from " + url);
				File downloadedOCBinary = downloadAndExtractOpenShiftClient(url);
				// windows has problem with previously defined symlinks, we will used copied oc from extracted 
				// createSymLink method was used
				this.pathToOC = copyOCFromExtractedFolder(downloadedOCBinary);
			} else {
				this.pathToOC = OCBinaryFile.get().getFile().getAbsolutePath();
				LOGGER.info("Binary is already downloaded at " + getPathToOC());
			}
		} else {
			LOGGER.info("External OC binary will be used");
			File oc = new File(binary.pathToOC());
			if (oc.exists()) {
				LOGGER.info("OC binary is at " + oc.getAbsolutePath());
				this.pathToOC = oc.getAbsolutePath();
			} else {
				throw new OpenShiftToolsException("Given path to OC binary does not exist: " + binary.pathToOC());
			}
		}
		if (binary.setOCInPrefs()) {
			setOCToPreferences(getPathToOC());
		}
	}
	
	private String copyOCFromExtractedFolder(File downloadedOCBinary) {
		try {
			return Files.copy(Paths.get(downloadedOCBinary.getAbsolutePath()),
					OCBinaryFile.get().getFile().toPath(),
					StandardCopyOption.REPLACE_EXISTING).toAbsolutePath().toString();
		} catch (IOException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not copy {0} to {1}:\n{2}", 
					new Object[] { OCBinaryFile.get().getFile().getAbsolutePath(), downloadedOCBinary.getAbsolutePath(), e }));
		}
	}

	@Override
	public void setDeclaration(OCBinary declaration) {
		this.binary = declaration;
	}

	@Override
	public void cleanUp() {
		setOCToPreferences("");
		if (!binary.ocExists() && binary.cleanup()) {
			deleteOnPathIfExists(getPathToOC());
		}
	}
	
	private void deleteOnPathIfExists(String path) {
		try {
			URI uri = new File(path).toURI();
			LOGGER.info("Going to delete URI resources on path: " + uri.getPath());
			Files.deleteIfExists(Paths.get(uri));
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new OpenShiftToolsException("Deleting files on path " + path + " was not successful, cause: " + e.getMessage());
		}		
	}

	@Override
	public OCBinary getDeclaration() {
		return this.binary;
	}
	
	private void setOCToPreferences(String path) {
		openDialogAndSelectPage();
		setOCLocation(path);
		closeDialog();
	}
	
	private void openDialogAndSelectPage() {
		dialog = new WorkbenchPreferenceDialog();
		page = new OpenShift3PreferencePage(dialog);
		
		dialog.open();
		dialog.select(page);
	}
	
	private void setOCLocation(String location) {
		page.setOCLocation(location);
		page.apply();
	}
	
	private void closeDialog() {
		dialog.cancel();
	}
	
	public static String getDefaultOCLocation() {
		return OCBinaryFile.get().getFile().getAbsolutePath();
	}
	
	private File downloadAndExtractOpenShiftClient(String url) {
		LOGGER.info("Creating directory binaries");
		File outputDirectory = new File(CLIENT_TOOLS_DESTINATION);
		FileHelper.createDirectory(outputDirectory);

		String fileName = downloadArchive(url);
		String extractedDirectory = extractArchive(fileName, outputDirectory);

		if (StringUtils.isEmpty(extractedDirectory)
			|| !(new File(extractedDirectory).exists())) {
				throw new OpenShiftToolsException("Cannot extract archive " + fileName + ". "
						+ "Archive does not extract into a single root folder");
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
				LOGGER.info(fileName + " already exists, it will not be downloaded");
				return fileName;
			}
			try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
					ReadableByteChannel readableByteChannel = Channels.newChannel(downloadUrl.openStream())) {
				LOGGER.info("Downloading OpenShift binary");
				fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			} catch (IOException ex) {
				// clean up after unsuccessful downloading
				deleteOnPathIfExists(fileName);
				throw new OpenShiftToolsException("Cannot download OpenShift binary\n" + ex.getMessage());
			}
		} catch (MalformedURLException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not download \"{0}\". Invalid url", downloadLink));
		}
		return fileName;
	}

	private String extractArchive(String fileName, File outputDirectory) {
		if (StringUtils.isEmpty(fileName)) {
			return null;
		}

		LOGGER.info(NLS.bind("Extracting OpenShift archive {0}", fileName));
		String extractedDirectory = null;
		if (fileName.endsWith(SUFFIX_ZIP)) {
			extractedDirectory = StringUtils.chomp(fileName, SUFFIX_ZIP);
			if (!new File(extractedDirectory + File.separator + OCBinaryFile.get().getName()).exists()) {
				FileHelper.unzipFile(new File(fileName), outputDirectory);
			} else {
				LOGGER.info("OC in path " + extractedDirectory + " already exists");
			}
		} else if (fileName.endsWith(SUFFIX_TAR_GZ)) {
			extractedDirectory = StringUtils.chomp(fileName, SUFFIX_TAR_GZ);
			if (!new File(extractedDirectory + File.separator + OCBinaryFile.get().getName()).exists()) {
				FileHelper.extractTarGz(new File(fileName), outputDirectory);
			} else {
				LOGGER.info("OC in path " + extractedDirectory + " already exists");
			}
		}

		return extractedDirectory;
	}
	
	private String getFileName(String urlPath) {
		String[] pathParts = urlPath.split("/");
		return Paths.get(CLIENT_TOOLS_DESTINATION, pathParts[pathParts.length - 1]).toString();
	}
	
	private String getDownloadLink(boolean newOC) {
		if (Platform.OS_LINUX.equals(Platform.getOS())) {
			return newOC ? ClientVersion.LINUX_LATEST.getDownloadLink() : ClientVersion.LINUX_1_1_64.getDownloadLink();
		} else if (Platform.getOS().startsWith(Platform.OS_WIN32) && Platform.getOSArch().equals(Platform.ARCH_X86_64)){
			return newOC ? ClientVersion.WINDOWS_LATEST.getDownloadLink() : ClientVersion.WINDOWS_1_1_64.getDownloadLink();
		} else if (Platform.OS_MACOSX.equals(Platform.getOS())){
			return newOC ? ClientVersion.MAC_LATEST.getDownloadLink() : ClientVersion.MAC_1_1_64.getDownloadLink();
		} else {
			return null;
		}
	}
	
	public enum ClientVersion {
		LINUX_1_1_64(DatastoreOS3.KEY_OC_LINUX_1_1_64),
		WINDOWS_1_1_64(DatastoreOS3.KEY_OC_WINDOWS_1_1_64),
		MAC_1_1_64(DatastoreOS3.KEY_OC_MAC_1_1_64),

		LINUX_LATEST(DatastoreOS3.KEY_OC_LINUX_LATEST),
		MAC_LATEST(DatastoreOS3.KEY_OC_MAC_LATEST),
		WINDOWS_LATEST(DatastoreOS3.KEY_OC_WINDOWS_LATEST);
		
		String url;
		String keyName;
		
		ClientVersion(String keyName) {
			this.keyName = keyName;
			this.url = SystemProperties.getRequiredProperty(this.keyName, properties.getProperty(this.keyName, ""), "Searched property " + keyName + " is not passed,"
					+ " please add it in form -D" + keyName + "=linkToOC");
		}
		
		public String getKeyName() {
			return keyName;
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
