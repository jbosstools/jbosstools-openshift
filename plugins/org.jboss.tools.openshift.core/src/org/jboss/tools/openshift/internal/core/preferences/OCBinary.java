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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;

public enum OCBinary {

	WINDOWS("oc.exe", new String[] { "*.exe"}), 
	OTHER("oc", new String[] {});
	
	private static final String OC_DEFAULTLOCATION_LINUX = "/usr/bin/oc";
	
	public static OCBinary getInstance() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return WINDOWS;
		} else {
			return OTHER;
		}
	}

	private String name;
	private String[] extensions;
	private CommandLocationBinary locationBinary;

	private OCBinary(String name, String[] extensions) {
		this.name = name;
		this.extensions = extensions;
	}

	public String getName() {
		return name;
	};

	public String[] getExtensions() {
		return extensions;
	};

	/**
	 * Tries to find the binary on the system path.
	 * 
	 * @return the location as found on the system path.
	 */
	public String getSystemPathLocation() {
		if( locationBinary == null ) {
			locationBinary = new CommandLocationBinary("oc");
			locationBinary.addPlatformLocation(Platform.OS_LINUX, OC_DEFAULTLOCATION_LINUX);
			locationBinary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return locationBinary.findLocation();
	}

	/**
	 * Returns the location from preferences or looks it up on the path.
	 * 
	 * @return
	 */
	public String getLocation() {
		String location = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		if (StringUtils.isBlank(location)) {
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
	    return new OCBinaryValidator(getLocation()).isCompatibleForPublishing(monitor);
	}
}
