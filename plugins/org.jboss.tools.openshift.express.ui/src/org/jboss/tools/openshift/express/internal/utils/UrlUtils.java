/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author André Dietisheim
 */
public class UrlUtils {

	private static final int HTTP_SCHEME_MAX_LENGTH = 8;
	private static final char HTTP_SCHEME_START_CHAR = 'h';
	private static final Pattern HTTP_SCHEME_REGEX = Pattern.compile("http[s]{0,1}://");

	/**
	 * Returns an url that was found in the given text. It starts looking
	 * backwards from the given offset within the given string. Returns
	 * <code>null</code> if none was found.
	 * 
	 * @param offset
	 *            starting point to look back in the given text.
	 * @param text
	 *            the text to search for an url
	 * @return the url that was found in the text
	 */
	public static String getUrl(int offset, String text) {
		int start = getUrlStart(offset, text);
		if (start == -1) {
			return null;
		}
		int stop = getUrlStop(offset, text);
		if (stop == -1) {
			return null;
		}
		return text.substring(start, stop);

	}

	/**
	 * Steps back in the given text until the beginning of the text or an
	 * occurrence of http(s):// is found.
	 * 
	 * @param offset the offset to start with stepping backwards 
	 * @param text the text to search
	 * @return the index at which http(s):// was found
	 */
	private static int getUrlStart(int offset, String text) {
		for (int i = offset; i > 0; --i) {
			if (text.charAt(i) == HTTP_SCHEME_START_CHAR
					&& (i + HTTP_SCHEME_MAX_LENGTH < text.length())) {
				Matcher matcher = HTTP_SCHEME_REGEX.matcher(text.substring(i, i + HTTP_SCHEME_MAX_LENGTH));
				if (matcher.find()) {
					return i;
				}
			}
		}
		return -1;
	}

	private static int getUrlStop(int offset, String text) {
		for (int i = offset; i < text.length(); ++i) {
			if (' ' == text.charAt(i)
					|| '\n' == text.charAt(i)) {
				return i;
			}
		}
		return -1;
	}

}
