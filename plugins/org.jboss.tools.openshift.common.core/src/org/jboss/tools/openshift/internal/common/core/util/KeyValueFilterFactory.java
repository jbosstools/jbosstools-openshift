/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * A factory that creates key-/value-filters for a given filter expression. The
 * expression consists of key=value pairs where both are regular expressions. A
 * sequence of pairs are delimited by ',' (ex. key=value, value, k.*y=val.*).
 * 
 * @author Andre Dietisheim
 */
public class KeyValueFilterFactory {

	private static final String GROUP_KEY = "KEY";
	private static final String GROUP_VALUE = "VALUE";
	private static final String KEY_VALUE_DELIMITER = "=";
	private static final Pattern filterPattern = Pattern.compile("(?<KEY>[^=]+)\\s*(=)?\\s*(?<VALUE>[^,]*)\\s*,*\\s*",
			Pattern.CASE_INSENSITIVE);

	private KeyValueFilterFactory() {
	}

	public static List<KeyValueFilter> create(String expression) {
		return parse(expression);
	}

	private static List<KeyValueFilter> parse(String expression) {
		List<KeyValueFilter> filters = new ArrayList<>();
		Matcher matcher = filterPattern.matcher(expression);
		while (matcher.find()) {
			String expression1 = matcher.group(GROUP_KEY);
			String expression2 = matcher.group(GROUP_VALUE);
			boolean hasKeyValueDelimiter = StringUtils.equals(KEY_VALUE_DELIMITER, matcher.group(2));
			filters.add(createLabelFilter(expression1, expression2, hasKeyValueDelimiter));
		}
		return filters;
	}

	private static KeyValueFilter createLabelFilter(String expression1, String expression2,
			boolean hasKeyValuedelimiter) {
		String keyExpression = null;
		String valueExpression = null;
		if (StringUtils.isEmpty(expression2) && !hasKeyValuedelimiter) {
			// value
			valueExpression = expression1;
		} else {
			// key=
			// key=value
			keyExpression = expression1;
			valueExpression = expression2;
		}
		return new KeyValueFilter(keyExpression, valueExpression);
	}

	public static class KeyValueFilter {

		private Pattern keyPattern;
		private Pattern valuePattern;

		public KeyValueFilter() {
			this(null, null);
		}

		private KeyValueFilter(String keyRegex, String valueRegex) {
			if (!StringUtils.isEmpty(keyRegex)) {
				this.keyPattern = Pattern.compile(keyRegex);
			}
			if (!StringUtils.isEmpty(valueRegex)) {
				this.valuePattern = Pattern.compile(valueRegex);
			}
		}

		public boolean matchesKey(String toMatch) {
			if (null == keyPattern) {
				return true;
			}
			return keyPattern.matcher(toMatch).find();
		}

		public boolean matchesValue(String toMatch) {
			if (null == valuePattern) {
				return true;
			}
			return valuePattern.matcher(toMatch).find();
		}

		/**
		 * Returns the character range within the given string that is matching the key
		 * filter expression.
		 * 
		 * @param value
		 * @return
		 */
		public Range getMatchingRangeForKey(String key) {
			return getMatchingRange(key, keyPattern);
		}

		/**
		 * Returns the character range within the given string that is matching the
		 * value filter expression.
		 * 
		 * @param value
		 * @return
		 */
		public Range getMatchingRangeForValue(String value) {
			return getMatchingRange(value, valuePattern);
		}

		private Range getMatchingRange(String string, Pattern pattern) {
			Range range = null;
			if (pattern != null) {
				Matcher matcher = pattern.matcher(string);
				if (matcher.find() && matcher.start() >= 0 && matcher.end() > 0) {
					range = new Range(matcher.start(), matcher.end() - matcher.start());
				}
			}
			return range;
		}

		public static class Range {

			public final int start;
			public final int length;

			public Range(int start, int length) {
				this.start = start;
				this.length = length;
			}
		}

		@Override
		public String toString() {
			return "LabelFilter [keyPattern=" + keyPattern + ", valuePattern=" + valuePattern + "]";
		}
	}
}