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
package org.jboss.tools.openshift.internal.common.core.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.openshift.internal.common.core.security.SecureStore.IStoreKey;


/**
 * Implements a key to be used to store values in the preferences store.
 * 
 * @author Andre Dietisheim
 *
 */
public class OpenShiftSecureStorageKey implements IStoreKey {

	private static final String SEPARATOR = "/";
	private static final Pattern SCHEME_PATTERN = Pattern.compile(".+://(.*)"); 
	
	private String baseKey;
	private String host;
	private String userName;

	public OpenShiftSecureStorageKey(String baseKey, String host, String userName) {
		this.baseKey = baseKey;
		this.host = sanitizeHost(host);
		this.userName = userName;
	}

	@Override
	public String getKey() {
		return new StringBuilder(baseKey)
				.append(SEPARATOR)
				.append(host)
				.append(SEPARATOR)
				.append(userName)
				.toString();
	}

	private String sanitizeHost(String value) {
		Matcher matcher = SCHEME_PATTERN.matcher(value);
		String host;
		if (matcher.find()
			&& matcher.groupCount() == 1) {
			host = matcher.group(1);
		} else {
			host = value;
		}
		if(host.endsWith(SEPARATOR)) {
			host = host.substring(0, host.length()-1);
		}
		return host;
	}
	
	@Override
	public boolean equals(Object key) {
		if (!key.getClass().isAssignableFrom(OpenShiftSecureStorageKey.class)) {
			return false;
		}
		OpenShiftSecureStorageKey openshiftKey = (OpenShiftSecureStorageKey) key;
		return (userName != null && openshiftKey.userName != null && userName.equals(openshiftKey.userName)) 
				&& (host != null && openshiftKey.host != null && host.equals(openshiftKey.host));
	}
}
