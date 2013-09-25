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

/**
 * @author Andre Dietisheim
 */
public class StringPreferenceValue extends AbstractPreferenceValue<String> {

	public StringPreferenceValue(String prefsKey, String pluginId) {
		super(prefsKey, pluginId);
	}

	public String get() {
		return get(null);
	}
	
	public String get(String currentValue) {
		return doGet(currentValue);
	}

	public void store(String value) {
		doStore(value);
	}
}
