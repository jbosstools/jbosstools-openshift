/******************************************************************************* 
 * Copyright (c) 2015-2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.preferences;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public enum OCBinary {

	WINDOWS("oc.exe"), OTHER("oc");

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
	}

	public String[] getExtensions() {
		return getLocationBinary().getPossibleSuffixes();
	}

	/**
	 * Tries to find the binary on the system path.
	 * 
	 * @return the location as found on the system path.
	 */
	public String getSystemPathLocation() {
		return getLocationBinary().findLocation();
	}

	private CommandLocationBinary getLocationBinary() {
		if (locationBinary == null) {
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
	 * Get the location of the workspace preference pointing to an oc install.
	 * 
	 * @return
	 */
	private String getWorkspaceLocation() {
		String location = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		if (location == null || location.trim().isEmpty()) {
			location = getSystemPathLocation();
		}
		return location;
	}

	/**
	 * Returns the location of the oc binary. It gets it from the given connection
	 * if it holds such a setting and is set to override the global location. As a
	 * fallback the location is retrieved from preferences or is looked up on the
	 * path.
	 * 
	 * @param connection
	 * @return returns the location from the given connection, workspace preferences
	 *         or from the path.
	 */
	public String getLocation(IConnection connection) {
		if (connection instanceof IOpenShiftConnection) {
			IOpenShiftConnection c = (IOpenShiftConnection) connection;
			Boolean override = (Boolean) c.getExtendedProperties().get(ICommonAttributes.OC_OVERRIDE_KEY);
			if (Boolean.TRUE.equals(override)) {
				String loc = (String) c.getExtendedProperties().get(ICommonAttributes.OC_LOCATION_KEY);
				if (!StringUtils.isEmpty(loc)) {
					return loc;
				}
			}
		}

		return getWorkspaceLocation();
	}

	/**
	 * Compute the error message for the OCBinary state and path.
	 * 
	 * @param valid
	 *            if the oc binary is valid or not
	 * @param location
	 *            the location of the oc binary
	 * @return the error message (may be null)
	 */
	public IStatus getStatus(IConnection connection, IProgressMonitor monitor) {
		String location = getLocation(connection);
		IStatus status = null;
		if (location == null) {
			status = OpenShiftCoreActivator.statusFactory()
					.errorStatus(OpenShiftCoreMessages.NoOCBinaryLocationErrorMessage);
		} else if (!new File(location).exists()) {
			status = OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind(OpenShiftCoreMessages.OCBinaryLocationDontExistsErrorMessage, location));
		} else if (!new File(location).canExecute()) {
			status = OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("{0} does not have execute permissions.", location));
		} else {
			status = new OCBinaryVersionValidator(location).getValidationStatus(monitor);
		}
		return status;
	}
}
