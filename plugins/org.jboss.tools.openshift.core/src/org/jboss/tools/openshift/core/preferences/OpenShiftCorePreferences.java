/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.core.preferences.StringPreferenceValue;
import org.jboss.tools.openshift.internal.common.core.preferences.StringsPreferenceValue;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinaryName;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCorePreferences implements IOpenShiftCoreConstants {
	
	public static final OpenShiftCorePreferences INSTANCE = new OpenShiftCorePreferences();

	/** available connections */
	private static final String CONNECTIONS = "org.jboss.tools.openshift.core.connection.CONNECTION_NAMES";
	private static final String CONNECTION_AUTH_PREFIX = "org.jboss.tools.openshift.core.connection.auth";

	private final StringsPreferenceValue connectionsPreferenceValue = 
			new StringsPreferenceValue('|', CONNECTIONS, OpenShiftCoreActivator.PLUGIN_ID);

	private final StringPreferenceValue ocBinaryLocation = 
			new StringPreferenceValue(OPENSHIFT_CLI_LOC, OpenShiftCoreActivator.PLUGIN_ID);
	
	private OpenShiftCorePreferences() {
	}

	public String[] loadConnections() {
		return connectionsPreferenceValue.get();
	}

	public void saveConnections(String[] connections) {
		connectionsPreferenceValue.set(connections);
	}
	
	public void saveAuthScheme(String connectionURL, String scheme) {
		createPreferenceValue(connectionURL).set(scheme);
	}

	public void removeAuthScheme(String connectionURL) {
		createPreferenceValue(connectionURL).remove();
	}
	
	public String loadScheme(String connectionURL) {
		return createPreferenceValue(connectionURL).get();
	}

	private StringPreferenceValue createPreferenceValue(String connectionURL) {
		return new StringPreferenceValue(NLS.bind("{0}.{1}",CONNECTION_AUTH_PREFIX,connectionURL), OpenShiftCoreActivator.PLUGIN_ID);
	}
	
	public String getOCBinaryLocation() {
		String result = ocBinaryLocation.get();
		if(StringUtils.isEmpty(result)) {
			result = OCBinaryName.getInstance().getLocation();
		}
		return result;
	}
	
	public void saveOCBinaryLocation(String location) {
		ocBinaryLocation.set(location);
	}
}
