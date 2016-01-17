/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.WordUtils;

/**
 * @author André Dietisheim
 */
public class StringUtils {
	
	private static final String LINE_SEPARATOR_KEY = "line.separator";
	private static final String SHORTENING_MARKER = "...";
	
	public static String pluralize(String value) {
		return value + "s";
	}
	
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

	public static boolean isEmpty(Object value) {
		return (value instanceof String)
				&& isEmpty((String) value);
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
	
	public static <T> String toString(Collection<T> elements, ToStringConverter<T> converter) {
		if (elements == null
				|| elements.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for(T element : elements) {
			builder.append(converter.toString(element));
			if (++i < elements.size()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}
		
	public static <T> String toString(Map<String, String> map) {
		if (map == null
				|| map.isEmpty()) {
			return null;
		}
		return toString(map.entrySet(), new ToStringConverter<Entry<String, String>>() {

			@Override
			public String toString(Entry<String, String> entry) {
				return new StringBuilder(entry.getKey())
					.append('=')
					.append(entry.getValue())
					.toString();
			}
		});
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
	
	public static boolean isAlphaNumericOrUnderscore(String value) {
		for (int i = 0; i < value.length(); ++i) {
			final char c = value.charAt(i);
			if (c != '_') {
				if (!Character.isLetterOrDigit(c)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean startsWithLetterOrUnderscore(String value) {
		if (isEmpty(value)) {
			return false;
		}
		char character = value.charAt(0);
		return character == '_' 
				|| Character.isLetter(character);
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
	
	public static String toLowerCase(String string) {
		if (isEmpty(string)) {
			return string;
		}
		
		return string.toLowerCase();
	}

	public static boolean areEqual(String thisString, String thatString) {
		if (thisString == null) {
			return thatString == null;
		} else {
			return thisString.equals(thatString);
		}
	}

	/**
	 * Rudimentary implementation of humanizing a String
	 * to a human readable form (e.g. Build Configs from buildConfigs)
	 * @param string
	 * @return
	 */
	public static String humanize(String value) {
		String[] parts = org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(value);
		String split = org.apache.commons.lang.StringUtils.join(parts, " ");
		return  WordUtils.capitalize(split);
	}
	
	/**
	 * Serialize Map<String, String> to a string
	 * of key value pairs
	 * @param map
	 * @return String
	 */
	public static String serialize(Map<String, String> map){
		List<String> out = new ArrayList<String>(map.size());
		for (Map.Entry<String, String> entry : map.entrySet()) {
			out.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}
		Collections.sort(out);
		return org.apache.commons.lang.StringUtils.join(out.toArray(), ",");
	}

	/**
	 * Remove all trailing <code>/</code> from a {@link String}.
	 * @param value
	 * @return the value without trailing <code>/</code>
	 */
	public static String removeTrailingSlashes(String value) {
		if (value == null){
			return null;
		}
		return value.replaceAll("/*$", "");
	}
}
