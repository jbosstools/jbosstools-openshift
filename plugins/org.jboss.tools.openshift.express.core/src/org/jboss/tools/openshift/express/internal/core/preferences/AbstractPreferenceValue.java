/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractPreferenceValue<TYPE> {

	private String pluginId;
	private String prefsKey;

	public AbstractPreferenceValue(String prefsKey, String pluginId) {
		this.pluginId = pluginId;
		this.prefsKey = prefsKey;
	}

	public abstract TYPE get();
			
	protected String doGet() {
		Preferences prefs = getPreferences(pluginId);
		return prefs.get(prefsKey, "");
	}

	public void clear() throws BackingStoreException {
		String prefsValue = doGet();
		if (prefsValue == null
				|| prefsValue == null) {
			return;
		}
		getPreferences(pluginId).clear();
	}
		
	public void set(TYPE value) {
		doStore(String.valueOf(value));
	}
	
	protected void doStore(String value) {
		Preferences prefs = getPreferences(pluginId);
		String prefsValue = prefs.get(prefsKey, "");
		if (prefsValue == null
				|| prefsValue.equals("") 
				|| !prefsValue.equals(value)) {
			prefs.put(prefsKey, value);
			try {
				prefs.flush();
			} catch (BackingStoreException bse) {
				// intentionally ignore, non-critical
			}
		}
	}

	protected Preferences getPreferences(String pluginId) {
		return InstanceScope.INSTANCE.getNode(pluginId);
	}
}
