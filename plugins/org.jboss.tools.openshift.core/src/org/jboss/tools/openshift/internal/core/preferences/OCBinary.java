/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.internal.core.preferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;

public enum OCBinary {

	WINDOWS("oc.exe"), 
	OTHER("oc");
	
	// The base name, without any suffixes applied
	private static final String CMD_BASE_NAME = "oc";
	
	// The default location on linux. 
	private static final String OC_DEFAULTLOCATION_LINUX = "/usr/bin/oc";
	
	
	public static OCBinary getInstance() {
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			return WINDOWS;
		} else {
			return OTHER;
		}
	}

	// The **LIKELY** name for the discovered binary
	// This isn't a guarantee but it is suitable for error messaging
	// When the runtime cannot be found
	private String defaultNameForPlatform;
	private CommandLocationBinary locationBinary;

	private OCBinary(String defaultNameForPlatform) {
		this.defaultNameForPlatform = defaultNameForPlatform;
	}

	public String getName() {
		return defaultNameForPlatform;
	};

	public String[] getExtensions() {
		return getLocationBinary().getPossibleSuffixes();
	};

	/**
	 * Tries to find the binary on the system path.
	 * 
	 * @return the location as found on the system path.
	 */
	public String getSystemPathLocation() {
		return getLocationBinary().findLocation();
	}

	private CommandLocationBinary getLocationBinary() {
		if( locationBinary == null ) {
			// CommandLocationBinary holds information for all platforms, 
			// so we're just setting the name and default location for all platforms 
			// where this we think we know this.  
			// We also set the default platform to linux, so if the current platform
			// has no custom logic, we can failover to linux logic. 
			locationBinary = new CommandLocationBinary(CMD_BASE_NAME);
			locationBinary.addPlatformLocation(Platform.OS_LINUX, OC_DEFAULTLOCATION_LINUX);
			locationBinary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return locationBinary;
	}
	
	/**
	 * Returns the location from preferences or looks it up on the path.
	 * 
	 * @return
	 */
	public String getLocation() {
		String location = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		if (location == null || location.trim().isEmpty()) {
			location = getSystemPathLocation();
		}
		return location;
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
	    return new OCBinaryVersionValidator(getLocation()).isCompatibleForPublishing(monitor);
	}
}
