/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core;

public final class OSIOCoreConstants {
	
	public static final String TOKEN_PROVIDER_EXTENSION_POINT = "org.jboss.tools.openshift.io.core.tokenProvider";
	
	public static final String LOGIN_PROVIDER_EXTENSION_POINT = "org.jboss.tools.openshift.io.core.loginProvider";

	public static final String DEVSTUDIO_OSIO_LANDING_PAGE_SUFFIX = "/devstudio";
	
	public static final String OSIO_ENDPOINT = "https://auth.openshift.io/api/";
	
	public static final String LOGIN_SUFFIX = "login?api_token=jbosstools&redirect=";
	
	public static final String REFRESH_SUFFIX = "token/refresh";
	
	public static final String ACCOUNT_BASE_KEY = "accounts";
	
	public static final long DURATION_24_HOURS = 24 * 3600 * 1000L; 
	
}
