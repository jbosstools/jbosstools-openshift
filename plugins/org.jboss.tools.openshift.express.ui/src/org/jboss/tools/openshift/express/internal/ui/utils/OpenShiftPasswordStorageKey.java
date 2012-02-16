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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStore.IStorageKey;


/**
 * Implements a key to be used to store values in the preferences store.
 * 
 * @author Andre Dietisheim
 *
 */
public class OpenShiftPasswordStorageKey implements IStorageKey {

	private static final char SEPARATOR = '/';
	
	private static final String PREFERNCES_BASEKEY = OpenShiftUIActivator.PLUGIN_ID.replace('.', SEPARATOR);
	private String platform;
	private String userName;

	public OpenShiftPasswordStorageKey(String platform, String userName) {
		this.userName = platform;
		this.platform = userName;
	}

	@Override
	public String getKey() {
		return new StringBuilder(PREFERNCES_BASEKEY)
				.append(platform)
				.append(SEPARATOR)
				.append(userName)
				.toString();
	}

	@Override
	public boolean equals(IStorageKey key) {
		if (!key.getClass().isAssignableFrom(OpenShiftPasswordStorageKey.class)) {
			return false;
		}
		OpenShiftPasswordStorageKey deltaCloudKey = (OpenShiftPasswordStorageKey) key;
		return userName.equals(deltaCloudKey.userName)
				&& platform.equals(deltaCloudKey.platform);
	}
}
