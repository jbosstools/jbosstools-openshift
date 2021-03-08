/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to handle Openshift token authentication response
 * 
 */
public class TokenExtractor {
	/**
	 * Regular expression used to check if browser page is page that displays the OAuth token
	 */
	public static final Pattern TOKEN_PAGE_PATTERN = Pattern
			.compile(".*<h2>Your API token is<\\/h2>.*<code>(.*)<\\/code>.*", Pattern.DOTALL);

	private Matcher matcher;

	public TokenExtractor(String content) {
		matcher = TOKEN_PAGE_PATTERN.matcher(content);
	}

	public boolean isTokenPage() {
		return matcher.matches();
	}

	public String getToken() {
		return matcher.group(1);
	}
}