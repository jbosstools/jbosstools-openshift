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
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.util.List;

/**
 * @author André Dietisheim
 */
public class StringUtils {
	
	private static final String LINE_SEPARATOR_KEY = "line.separator";
	private static final String SHORTENING_MARKER = "...";
	
	public static String null2emptyString(String value) {
		if (value != null) {
			return value;
		}
		return "";
	}

	public static String toStringOrNull(Object value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
	
	public static boolean isEmpty(String value) {
		return value == null
				|| value.length() == 0;
	}

	public static <T> String toString(List<T> elements) {
		return toString(elements, new ToStringConverter<T>() {

			@Override
			public String toString(T object) {
				if (object == null) {
					return null;
				}
				return String.valueOf(object);
			}
		});
	}
	
	public static <T> String toString(List<T> elements, ToStringConverter<T> converter) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < elements.size(); i++) {
			converter.toString(elements.get(i));
			builder.append(converter.toString(elements.get(i)));
			if (i + 1 < elements.size()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}
		
	public static interface ToStringConverter<T> {
		public String toString(T object);
	}
	
	public static String getLineSeparator() {
		return System.getProperty(LINE_SEPARATOR_KEY);
	}

	public static boolean isAlphaNumeric(String value) {
		for (int i = 0; i < value.length(); ++i) {
			final char c = value.charAt(i);
			if (!Character.isLetterOrDigit(c)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isEmptyOrNull(String value) {
		return value == null
				|| value.isEmpty();
	}

	public static String shorten(String text, int maxLength) {
		if (text.length() < maxLength) {
			return text;
		}
		int availableCharacters = maxLength - SHORTENING_MARKER.length();
		return new StringBuilder(text.substring(0, availableCharacters / 2))
				.append(SHORTENING_MARKER)
				.append(text.substring(text.length() - availableCharacters / 2, text.length()))
				.toString();
	}

	public static String getWithoutSuffix(String string, String suffix) {
		String stringNoSuffix = string;
		int suffixIndex = string.indexOf(suffix);
		if (suffixIndex >= 0) {
			stringNoSuffix = string.substring(0, suffixIndex);
		}
		return stringNoSuffix;
	}
	
	public static String trim(String string) {
		if (string == null) {
			return string;
		}
		return string.trim();
	}
}
