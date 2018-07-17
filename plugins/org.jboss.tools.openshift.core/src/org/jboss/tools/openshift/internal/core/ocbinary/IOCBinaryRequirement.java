/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.ocbinary;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.osgi.framework.Version;

public interface IOCBinaryRequirement {

	public static IOCBinaryRequirement NON_EMPTY = new NonEmpty();
	public static IOCBinaryRequirement EXECUTABLE = new Executable();
	public static IOCBinaryRequirement RECOGNIZED_VERSION = new RecognizedVersion();
	public static IOCBinaryRequirement MIN_VERSION_RSYNC = new MinVersionRSync();
	public static IOCBinaryRequirement MIN_VERSION_RSYNC_LINUX = new MinVersionRSyncLinux();
	public static IOCBinaryRequirement MIN_VERSION_RSYNC_PATH_WITH_SPACES = new MinVersionRSyncPathWithSpaces();

	/**
	 * Returns {@code true} if this requirement is fullfilled, {@code false}
	 * otherwise.
	 * 
	 * @param version
	 * @param path
	 * @return
	 */
	public boolean isFulfilled(Version version, String path);

	/**
	 * Returns the severity for this requirement.
	 * @return
	 */
	public int getSeverity();

	/**
	 * Returns the message for this requirement. If the message contains links (aka
	 * <a>link</a>) using {@code false} for displayLinks will suppress the markup in
	 * the message.
	 * 
	 * @param path
	 * @param version
	 * @param displayLinks
	 * @return
	 */
	public String getMessage(String path, Version version, boolean displayLinks);

	/** for testing purposes **/
	public String getOS();

	/**
	 * Requirement that is fulfilled if the given path is not {@code null}.
	 */
	public class NonEmpty extends OCBinaryRequirement {
		private NonEmpty() {
			super(IStatus.WARNING, OpenShiftCoreMessages.NoOCBinaryLocationErrorMessage);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			return !StringUtils.isEmpty(path);
		}
	}

	/**
	 * Requirement that is fulfilled if the given path points to a file that exists
	 * and is executable.
	 */
	public class Executable extends OCBinaryRequirement {

		private Executable() {
			super(IStatus.WARNING, OpenShiftCoreMessages.OCBinaryLocationDontExistsErrorMessage);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			File oc = new File(path);
			return oc.exists() && oc.canExecute();
		}
	}

	/**
	 * Requirement that is fulfilled if the given version is not empty.
	 */
	public class RecognizedVersion extends OCBinaryRequirement {

		private RecognizedVersion() {
			super(IStatus.WARNING, OpenShiftCoreMessages.OCBinaryNotRecognized);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			return version != null
					&& !version.equals(Version.emptyVersion);
		}
	}

	/**
	 * Requirement that is fulfilled for using "oc rsync" if the path to the oc binary contains spaces
	 * and the minimum version is used.
	 * 
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-23862">JBIDE-23862</a>
	 */
	public class MinVersionRSyncPathWithSpaces extends OCBinaryRequirement {

		private static final Version OC_MINIMUM_VERSION_FOR_OC_WITH_SPACE = Version.parseVersion("3.7.0");

		private MinVersionRSyncPathWithSpaces() {
			super(IStatus.WARNING, OpenShiftCoreMessages.OCBinaryLocationRSyncWithSpaceErrorMessage);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			return isOS(Platform.OS_WIN32) 
					|| !StringUtils.contains(path, ' ')
					|| OC_MINIMUM_VERSION_FOR_OC_WITH_SPACE.compareTo(version) <= 0;
		}
	}

	/**
	 * Requirement that is fullfilled if the minimum version for using 'oc rsync' is
	 * fulfilled.
	 */
	public class MinVersionRSync extends OCBinaryRequirement {

		private static final Version OC_MINIMUM_VERSION_FOR_RSYNC = Version.parseVersion("1.1.1");

		private MinVersionRSync() {
			super(IStatus.WARNING, OpenShiftCoreMessages.OCBinaryLocationIncompatibleErrorMessage);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			return version != null 
					&& OC_MINIMUM_VERSION_FOR_RSYNC.compareTo(version) <= 0;
		}
	}

	/**
	 * Requirement that is fullfille if the minimum version for using 'oc rsync' on
	 * linux is fulfilled. RSync on Linux tries to replicate permissions on
	 * OpenShift unless --no-g, --no-o is used. This will cause permission error
	 * since the user that executes the rsync lacks those permissions.
	 * 
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-25700">JBIDE-25700</>
	 *
	 **/
	public class MinVersionRSyncLinux extends OCBinaryRequirement {

		private static final Version OC_MINIMUM_VERSION_FOR_LINUX_OC_PERMS = Version.parseVersion("3.11.0");

		private MinVersionRSyncLinux() {
			super(IStatus.WARNING, OpenShiftCoreMessages.OCBinaryLinuxRSyncPermErrorMessage);
		}

		@Override
		public boolean isFulfilled(Version version, String path) {
			return !isOS(Platform.OS_LINUX) 
					|| OC_MINIMUM_VERSION_FOR_LINUX_OC_PERMS.compareTo(version) <= 0;
		}
	}

	abstract class OCBinaryRequirement implements IOCBinaryRequirement {

		private static final Pattern LINKS_MARKUP_PATTERN = Pattern.compile("<a>([^<]*)</a>");

		private int severity;
		private String message;

		protected OCBinaryRequirement(int severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		protected boolean isOS(String os) {
			return getOS().equals(os);
		}

		/** for testing purposes */
		public String getOS() {
			return Platform.getOS();
		}

		public int getSeverity() {
			return severity;
		}

		public String getMessage(String path, Version version, boolean displayLinks) {
			String substitutedMessage = NLS.bind(message, path, version);
			if (!displayLinks) {
				substitutedMessage = removeLinks(substitutedMessage);
			}
			return substitutedMessage;
		}
		
		private String removeLinks(String message) {
			Matcher matcher = LINKS_MARKUP_PATTERN.matcher(message);
			StringBuffer buffer = new StringBuffer();
			while(matcher.find()) {
				matcher.appendReplacement(buffer, "$1");
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}
	}
}
