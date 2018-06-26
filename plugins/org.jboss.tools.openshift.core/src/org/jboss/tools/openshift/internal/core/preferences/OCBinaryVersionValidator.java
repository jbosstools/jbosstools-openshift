/******************************************************************************* 
 * Copyright (c) 2016-2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.framework.Version;

public class OCBinaryVersionValidator {

	/**
	 * The regular expression use to match line with version of the oc executable.
	 * So should be either: vX.Y.Z (for github releases) vX.Y.Z-B+C or vX.Y.Z.B (for
	 * enterprise releases)
	 * 
	 * where X.Y.Z is the version (Z and Y may be omitted) B is the qualifier
	 * (optional) C is the short commit hash (optional)
	 * 
	 * examples:
	 * <ul>
	 * <li><strong>github versions:</strong></li>
	 * <li>oc v1.5.0-alpha.2+e4b43ee</li>
	 * <li>oc v1.4.1+3f9807a</li>
	 * <li>oc v1.4.0-rc1+b4e0954</li>
	 * <li>oc v1.4.0-alpha.1+f189ede</li>
	 * <li>oc v1.3.3</li>
	 * <li><strong>enterprise versions:</strong></li>
	 * <li>oc v3.4.1.2</li>
	 * <li>oc v3.4.0.40</li>
	 * </ul>
	 */

	public static enum OCBinaryStatus {

		OK(null),
		OC_INCOMPATIBLE_FOR_RSYNC(OpenShiftCoreMessages.OCBinaryLocationIncompatibleErrorMessage),
		/** @see <a href="https://issues.jboss.org/browse/JBIDE-25700">JBIDE-25700</> **/
		OC_LINUX_RSYNC_PERM_ERRORS(OpenShiftCoreMessages.OCBinaryLinuxRSyncPermErrorMessage),
		OC_PATH_INCOMPATIBLE(OpenShiftCoreMessages.OCBinaryLocationWithSpaceErrorMessage);

		private String message;

		private OCBinaryStatus(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

	}

	private static final Pattern OC_VERSION_LINE_PATTERN = Pattern
			.compile("oc[^v]*v(([0-9]{1,2})(\\.[0-9]{1,2})?(\\.[0-9]{1,2})?)([-\\.]([^+]*))?.*");

	private static final Version OC_MINIMUM_VERSION_FOR_RSYNC = Version.parseVersion("1.1.1");

	private static final Version OC_MINIMUM_VERSION_FOR_OC_WITH_SPACE = Version.parseVersion("3.7.0");

	/** @see <a href="https://issues.jboss.org/browse/JBIDE-25700">JBIDE-25700</> **/
	private static final Version OC_MINIMUM_VERSION_FOR_LINUX_OC_PERMS = Version.parseVersion("3.11.0");

	private String path;

	public OCBinaryVersionValidator(String path) {
		this.path = path;
	}

	/**
	 * Returns the version of the OC binary by running the version command.
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return the OSGi version of the binary
	 */
	public Version getVersion(IProgressMonitor monitor) {
		Optional<Version> version = Optional.empty();
		if (path != null) {
			try {
				Process process = new ProcessBuilder(path, "version").start();
				version = parseVersion(process, monitor);
			} catch (IOException e) {
				OpenShiftCoreActivator.logError(e.getLocalizedMessage(), e);
			}
		}
		return version.orElse(Version.emptyVersion);
	}

	private Optional<Version> parseVersion(Process process, IProgressMonitor monitor) throws IOException {
		Optional<Version> version = Optional.empty();
		String line = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			while (!monitor.isCanceled() 
					&& (!version.isPresent()) 
					&& ((line = reader.readLine()) != null)) {
				version = parseVersion(line);
			}
		}
		return version;
	}

	protected Optional<Version> parseVersion(String line) {
		if (StringUtils.isBlank(line)) {
			return Optional.empty();
		}
		Matcher matcher = OC_VERSION_LINE_PATTERN.matcher(line);
		Version version = null;
		if (matcher.matches()) {
			try {
				version = Version.parseVersion(matcher.group(1));
				if ((matcher.groupCount() > 1) && version.getQualifier().isEmpty()) {
					// Since we are using the OSGi Version class to assist,
					// and an OSGi qualifier must fit (alpha|numeric|-|_)+ format,
					// remove all invalid characters in group6? alpha.1.dumb -> alpha1dumb
					String group6 = matcher.group(6);
					if (group6 != null) {
						group6 = group6.replaceAll("[^a-zA-Z0-9_-]", "_");
						version = new Version(version.getMajor(), version.getMinor(), version.getMicro(), group6);
					}
				}
			} catch (IllegalArgumentException e) {
				OpenShiftCoreActivator.logError(NLS.bind("Could not parse oc version {0}.", line), e);
			}
		}
		return Optional.ofNullable(version);
	}

	public IStatus getValidationStatus(IProgressMonitor monitor) {
		return getValidationStatus(getVersion(monitor), monitor);
	}

	public IStatus getValidationStatus(Version version, IProgressMonitor monitor) {
		OCBinaryStatus status = getStatus(version, monitor);
		String message = NLS.bind(status.getMessage(), path);
		if (status == OCBinaryStatus.OK) {
			return Status.OK_STATUS;
		} else {
			return StatusFactory.warningStatus(OpenShiftCoreActivator.PLUGIN_ID, message);
		}
	}

	public OCBinaryStatus getStatus(IProgressMonitor monitor) {
		return getStatus(getVersion(monitor), monitor);
	}

	public OCBinaryStatus getStatus(Version version, IProgressMonitor monitor) {
		OCBinaryStatus status = OCBinaryStatus.OK;
		if (!isCompatibleForPublishing(version)) {
			status = OCBinaryStatus.OC_INCOMPATIBLE_FOR_RSYNC;
		} else if (!isOcPathValid(version, path)) {
			status = OCBinaryStatus.OC_PATH_INCOMPATIBLE;
		} else if (!isCompatibleForLinuxRSyncPermissions(version)) {
			status = OCBinaryStatus.OC_LINUX_RSYNC_PERM_ERRORS;
		}
		return status;
	}

	private boolean isCompatibleForPublishing(Version version) {
		return version!= null
				&& OC_MINIMUM_VERSION_FOR_RSYNC.compareTo(version) <= 0;
	}

	private boolean isOcPathValid(Version version, String path) {
			return isPlatform(Platform.OS_WIN32)
					|| !StringUtils.contains(path, ' ')
					|| OC_MINIMUM_VERSION_FOR_OC_WITH_SPACE.compareTo(version) <= 0;
	}

	private boolean isCompatibleForLinuxRSyncPermissions(Version version) {
		return !isPlatform(Platform.OS_LINUX)
				|| OC_MINIMUM_VERSION_FOR_LINUX_OC_PERMS.compareTo(version) <= 0;
	}

	protected boolean isPlatform(String platform) {
		return Platform.getOS().equals(platform);
	}
}
