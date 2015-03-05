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

import org.jboss.tools.openshift.internal.common.core.preferences.StringsPreferenceValue;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferences {
	
	private static final OpenShiftPreferences INSTANCE = new OpenShiftPreferences();

	/** available connections */
	private static final String CONNECTIONS = "org.jboss.tools.openshift.core.connection.CONNECTION_NAMES";

	private final StringsPreferenceValue connectionsPreferenceValue = 
			new StringsPreferenceValue('|', CONNECTIONS, OpenShiftCoreActivator.PLUGIN_ID);
	
	public static OpenShiftPreferences getInstance(){
		return INSTANCE;
	}

	private OpenShiftPreferences() {
	}

	public String[] loadConnections() {
		return connectionsPreferenceValue.get();
	}

	public void saveConnections(String[] connections) {
		connectionsPreferenceValue.set(connections);
	}
}
