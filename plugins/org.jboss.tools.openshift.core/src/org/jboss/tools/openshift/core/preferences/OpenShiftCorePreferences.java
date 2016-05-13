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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.common.core.preferences.StringPreferenceValue;
import org.jboss.tools.openshift.internal.common.core.preferences.StringsPreferenceValue;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCorePreferences implements IOpenShiftCoreConstants, IOpenShiftCorePreferences {
	
	public static final OpenShiftCorePreferences INSTANCE = new OpenShiftCorePreferences();

	/** available connections */
	private static final String CONNECTIONS = "org.jboss.tools.openshift.core.connection.CONNECTION_NAMES";
	private static final String CONNECTION_AUTH_PREFIX = "org.jboss.tools.openshift.core.connection.auth";
	private static final String CONNECTION_EXT_PROPERTY_PREFIX = "org.jboss.tools.openshift.core.connection.extproperties";

	private final StringsPreferenceValue connectionsPreferenceValue = 
			new StringsPreferenceValue('|', CONNECTIONS, OpenShiftCoreActivator.PLUGIN_ID);

	private final StringPreferenceValue ocBinaryLocation = 
			new StringPreferenceValue(OPENSHIFT_CLI_LOC, OpenShiftCoreActivator.PLUGIN_ID);
	
	private OpenShiftCorePreferences() {
	}

	@Override
	public String[] loadConnections() {
		return connectionsPreferenceValue.get();
	}

	@Override
	public void saveConnections(String[] connections) {
		connectionsPreferenceValue.set(connections);
	}
	
	@Override
	public void saveAuthScheme(String connectionURL, String scheme) {
		createAuthSchemePreferenceValue(connectionURL).set(scheme);
	}

	@Override
	public void removeAuthScheme(String connectionURL) {
		createAuthSchemePreferenceValue(connectionURL).remove();
	}
	
	@Override
	public String loadScheme(String connectionURL) {
		return createAuthSchemePreferenceValue(connectionURL).get();
	}

	private StringPreferenceValue createAuthSchemePreferenceValue(String connectionURL) {
		return createPreferenceValue(CONNECTION_AUTH_PREFIX,connectionURL);
	}
	
	@Override
	public String getOCBinaryLocation() {
		return ocBinaryLocation.get();
	}
	
	@Override
	public void saveOCBinaryLocation(String location) {
		ocBinaryLocation.set(location);
	}

	@Override
	public void saveExtProperties(String connectionURL, Map<String, Object> ext) {
		if(connectionURL == null || ext == null) return;
		try{
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(ext);
			createPreferenceValue(CONNECTION_EXT_PROPERTY_PREFIX, connectionURL).set(json);
		} catch (IOException e) {
			OpenShiftCoreActivator.logError("Unable to persist ext properties for " + connectionURL, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> loadExtProperties(String connectionURL) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = createPreferenceValue(CONNECTION_EXT_PROPERTY_PREFIX, connectionURL).get();
			if(StringUtils.isNotBlank(json)) {
				return mapper.readValue(new StringReader(json), Map.class);
			}
		} catch (IOException e) {
			OpenShiftCoreActivator.logError("Unable to load ext properties for " + connectionURL, e);
		}
		return new HashMap<>();
	}
	
	private StringPreferenceValue createPreferenceValue(String prefix, String connectionURL) {
		return new StringPreferenceValue(NLS.bind("{0}.{1}",prefix,connectionURL), OpenShiftCoreActivator.PLUGIN_ID);
	}
}
