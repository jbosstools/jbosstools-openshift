/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

/**
 * @author adietish@redhat.com
 */
public class SystemProperties {

	public static final String KEY_SERVER = "openshift.server";
	public static final String KEY_USERNAME = "openshift.username";
	public static final String KEY_PASSWORD = "openshift.password";
	private static final String KEY_SECURE_STORAGE_PASSWORD = "securestorage.password";

	// secure storage
	public static final String SECURE_STORAGE_PASSWORD = getRequiredProperty(KEY_SECURE_STORAGE_PASSWORD, 
			"Please add '-D" + KEY_SECURE_STORAGE_PASSWORD + "=[Eclipse secure storage password]' to your launch arguments");

	public static String getRequiredProperty(String key, String errorMessage) {
		return getRequiredProperty(key, null, errorMessage);
	}

	public static String getRequiredProperty(String key, String[] validValues, String errorMessage) {
		assertTrue(StringUtils.isNotBlank(key));
		String value = System.getProperty(key);
		assertTrue(errorMessage, StringUtils.isNotBlank(value));
		if (validValues != null && validValues.length > 0) {
			assertThat(Arrays.asList(validValues), hasItem(value));
		}
		return value;
	}

}
