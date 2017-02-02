/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
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
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.framework.Version;

public class OCBinaryVersionValidator {
	
	/**
	 * The regular expression use to match line with version of the oc executable.
	 * So should be either:
	 *   vX.Y.Z (for github releases)
	 *   vX.Y.Z-B+C
	 *   or 
	 *   vX.Y.Z.B (for enterprise releases)
	 *   
	 *   where X.Y.Z is the version (Z and Y may be omitted)
	 *   B is the qualifier (optional)
	 *   C is the short commit hash (optional)
	 *   
	 *   examples:
	 *   <ul>
	 *	 <li><strong>github versions:</strong></li>
	 *   <li>oc v1.5.0-alpha.2+e4b43ee</li>
	 *   <li>oc v1.4.1+3f9807a</li>
	 *   <li>oc v1.4.0-rc1+b4e0954</li>
	 *   <li>oc v1.4.0-alpha.1+f189ede</li>
	 *   <li>oc v1.3.3</li>
	 *   <li><strong>enterprise versions:</strong></li>
	 *   <li>oc v3.4.1.2</li>
	 *   <li>oc v3.4.0.40</li>
	 *   </ul>
	 */
	private static final Pattern OC_VERSION_LINE_PATTERN = 
			Pattern.compile("oc[^v]*v(([0-9]{1,2})(\\.[0-9]{1,2})?(\\.[0-9]{1,2})?)([-\\.]([^+]*))?.*");

	private static final Version OC_MINIMUM_VERSION_FOR_RSYNC = Version.parseVersion("1.1.1");

	private String path;
	
	public OCBinaryVersionValidator(String path) {
		this.path = path;
	}

	
	/**
	 * Returns the version of the OC binary by running the version command.
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return the OSGi version of the binary
	 */
	public Version getVersion(IProgressMonitor monitor) {
		Optional<Version> version = Optional.empty();
		if (path != null) {
			try {
				ProcessBuilder builder = new ProcessBuilder(path, "version");
				Process process = builder.start();
				version = parseVersion(process, monitor);
			} catch (IOException e) {
				OpenShiftCoreActivator.logError(e.getLocalizedMessage(), e);
			}
		}
		return version.orElse(Version.emptyVersion);
	}


	private Optional<Version> parseVersion(Process process, IProgressMonitor monitor)
			throws IOException {
        Optional<Version> version = Optional.empty();
		String line = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
		    while (!monitor.isCanceled() && (!version.isPresent()) && ((line = reader.readLine()) != null)) {
		        version = parseVersion(line);
		    }
		}
		return version;
	}
	
	public static Optional<Version> parseVersion(String line) {
		if  (StringUtils.isBlank(line)) {
			return Optional.empty();
		}
        Matcher matcher = OC_VERSION_LINE_PATTERN.matcher(line);
        Version version = null;
        if (matcher.matches()) {
        	try {
        		version = Version.parseVersion(matcher.group(1));
        		if ((matcher.groupCount() > 1) && version.getQualifier().isEmpty()) {
        			version = new Version(
        					version.getMajor(),
        					version.getMinor(),
        					version.getMicro(),
        					matcher.group(6));
        		}
        	} catch (IllegalArgumentException e) {
        		OpenShiftCoreActivator.logError(NLS.bind("Could not parse oc version {0}.",line), e);
        	}
        }
        return Optional.ofNullable(version);
	}

	/**
	 * Checks if the oc binary is compatible for rsync publishing.
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the oc binary is compatible
	 * @see https://issues.jboss.org/browse/JBIDE-21307
	 * @see https://github.com/openshift/origin/issues/6109
	 */
	public boolean isCompatibleForPublishing(IProgressMonitor monitor) {
	    return isCompatibleForPublishing(getVersion(monitor));
	}

	public static boolean isCompatibleForPublishing(Version version) {
        return version != null && version.compareTo(OC_MINIMUM_VERSION_FOR_RSYNC) >= 0;
	}
}
