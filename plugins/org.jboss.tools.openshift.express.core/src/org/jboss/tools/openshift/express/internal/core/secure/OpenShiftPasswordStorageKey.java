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
package org.jboss.tools.openshift.express.internal.core.secure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.openshift.express.internal.core.secure.SecurePasswordStore.IStorageKey;


/**
 * Implements a key to be used to store values in the preferences store.
 * 
 * @author Andre Dietisheim
 *
 */
public class OpenShiftPasswordStorageKey implements IStorageKey {

	private static final char SEPARATOR = '/';
	private static final Pattern SCHEME_PATTERN = Pattern.compile(".+://(.*)"); 
	
	/*
	 * Hard-code the openshift UI activator id, due to backwards compatability issues
	 */
	private static final String PREFERNCES_BASEKEY = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$

	private String platform;
	private String userName;

	public OpenShiftPasswordStorageKey(String platform, String userName) {
		this.platform = stripScheme(platform);
		this.userName = userName;
	}

	@Override
	public String getKey() {
		return new StringBuilder(PREFERNCES_BASEKEY)
				.append(SEPARATOR)
				.append(platform)
				.append(SEPARATOR)
				.append(userName)
				.toString();
	}

	private String stripScheme(String value) {
		Matcher matcher = SCHEME_PATTERN.matcher(value);
		if (matcher.find()
			&& matcher.groupCount() == 1) {
			return matcher.group(1);
		} else {
			return value;
		}
	}
	
	@Override
	public boolean equals(IStorageKey key) {
		if (!key.getClass().isAssignableFrom(OpenShiftPasswordStorageKey.class)) {
			return false;
		}
		OpenShiftPasswordStorageKey openshiftKey = (OpenShiftPasswordStorageKey) key;
		return (userName != null && openshiftKey.userName != null && userName.equals(openshiftKey.userName)) 
				&& (platform != null && openshiftKey.platform != null && platform.equals(openshiftKey.platform));
	}
}
