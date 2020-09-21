/******************************************************************************* 
 * Copyright (c) 2016-2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.ocbinary;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.framework.Version;

public class OCBinaryValidator {

	public static final List<IOCBinaryRequirement> RSYNC_REQUIREMENTS = unmodifiableList(asList(
			IOCBinaryRequirement.NON_EMPTY,
			IOCBinaryRequirement.EXECUTABLE,
			IOCBinaryRequirement.RECOGNIZED_VERSION,
			IOCBinaryRequirement.MIN_VERSION_RSYNC_PATH_WITH_SPACES,
			IOCBinaryRequirement.MIN_VERSION_RSYNC,
			IOCBinaryRequirement.MIN_VERSION_RSYNC_LINUX
			));
	
	public static final List<IOCBinaryRequirement> NON_RSYNC_REQUIREMENTS = unmodifiableList(asList(
			IOCBinaryRequirement.NON_EMPTY,
			IOCBinaryRequirement.EXECUTABLE,
			IOCBinaryRequirement.RECOGNIZED_VERSION
			));

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
	private static final Pattern OC_VERSION_LINE_PATTERN = Pattern
			.compile("(oc|Client\\sVersion:)[^0-9]*("
					+ "([0-9]{1,2})" // major
					+ "(\\.[0-9]{1,2})?" // minor
					+ "(\\.[0-9]{1,2})?)" // patch
					+ "([-\\.]([^+|\\.]*)" // qualifier (group7)
					+ ")?.*");

	private String path;

	public OCBinaryValidator(String path) {
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
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Checking oc binary version...", 1);
		Optional<Version> version = Optional.empty();
		if (!StringUtils.isEmpty(path)) {
			try {
				Process process = new ProcessBuilder(path, "version").start();
				version = parseVersion(process, monitor);
				if (!version.isPresent()) {
					process = new ProcessBuilder(path, "version", "--short").start();
					version = parseVersion(process, monitor);
				}
			} catch (IOException e) {
				OpenShiftCoreActivator.logError(e.getLocalizedMessage(), e);
			} finally {
				subMonitor.done();
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
				version = Version.parseVersion(matcher.group(2));
				if ((matcher.groupCount() > 1) && version.getQualifier().isEmpty()) {
					// Since we are using the OSGi Version class to assist,
					// and an OSGi qualifier must fit (alpha|numeric|-|_)+ format,
					// remove all invalid characters in group6? alpha.1.dumb -> alpha1dumb
					String group7 = matcher.group(7);
					if (group7 != null) {
						group7 = group7.replaceAll("[^a-zA-Z0-9_-]", "_");
						version = new Version(version.getMajor(), version.getMinor(), version.getMicro(), group7);
					}
				}
			} catch (IllegalArgumentException e) {
				OpenShiftCoreActivator.logError(NLS.bind("Could not parse oc version in \"{0}\".", line), e);
			}
		}
		return Optional.ofNullable(version);
	}

	public IStatus getStatus(IProgressMonitor monitor) {
		return getStatus(getVersion(monitor), true);
	}

	public IStatus getStatus(Version version) {
		return getStatus(version, true);
	}

	public IStatus getStatus(IProgressMonitor monitor, boolean displayLinks) {
		return getStatus(getVersion(monitor), displayLinks, RSYNC_REQUIREMENTS);
	}

	public IStatus getStatus(IProgressMonitor monitor, boolean displayLinks, Collection<IOCBinaryRequirement> requirements) {
		return getStatus(getVersion(monitor), displayLinks, requirements);
	}

	public IStatus getStatus(Version version, boolean displayLinks) {
		return getStatus(version, displayLinks, RSYNC_REQUIREMENTS);
	}

	public IStatus getStatus(Version version, boolean displayLinks, Collection<IOCBinaryRequirement> requirements) {
		for (IOCBinaryRequirement requirement : requirements) {
			if (!requirement.isFulfilled(version, path)) {
				return StatusFactory.getInstance(requirement.getSeverity(), 
						OpenShiftCoreActivator.PLUGIN_ID, requirement.getMessage(path, version, displayLinks));
			}
		}
		return Status.OK_STATUS;
	}	
}
