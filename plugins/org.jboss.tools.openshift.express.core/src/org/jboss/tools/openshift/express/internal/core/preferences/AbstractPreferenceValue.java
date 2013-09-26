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
		return doGet(null);
	}
	
	public abstract TYPE get(TYPE currentValue);
	
	protected String doGet(String currentValue) {
		if( currentValue == null || currentValue.equals("")) {
			// pre-set with previously used
			Preferences prefs = getPreferences(this.pluginId);
			return prefs.get(prefsKey, "");
		} else {
			return currentValue;
		}
	}

	public void remove() {
		String prefsValue = doGet();
		if (prefsValue == null
				|| prefsValue == null) {
			return;
		}
		store(null);
	}
	
	public abstract void store(TYPE value);
	
	protected void doStore(String value) {
		Preferences prefs = getPreferences(this.pluginId);
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
