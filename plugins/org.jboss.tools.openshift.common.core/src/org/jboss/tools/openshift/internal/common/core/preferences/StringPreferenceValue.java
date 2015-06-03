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
package org.jboss.tools.openshift.internal.common.core.preferences;


/**
 * @author Andre Dietisheim
 */
public class StringPreferenceValue extends AbstractPreferenceValue<String> {

	private String pluginId;
	private String prefsKey;
	public StringPreferenceValue(String prefsKey, String pluginId) {
		super(prefsKey, pluginId);
		this.pluginId = pluginId;
		this.prefsKey = prefsKey;
	}

	@Override
	public String get() {
		return doGet();
	}

	@Override
	public void set(String value) {
		doStore(value);
	}
	
	public void remove() {
		getPreferences(pluginId).remove(prefsKey);
	}
}
