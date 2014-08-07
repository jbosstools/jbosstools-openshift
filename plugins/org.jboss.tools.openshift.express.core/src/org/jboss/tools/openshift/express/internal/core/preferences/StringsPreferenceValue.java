/*******************************************************************************
 * Copyright (c) 2010-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.preferences;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * A class that offers access to a collection of values that is stored in the
 * preferences under a single key.
 * 
 * @author Andre Dietisheim
 */
public class StringsPreferenceValue extends AbstractPreferenceValue<String[]> {

	private static final int NO_MAXSIZE = -1;
	private String delimiter;
	private String escapedDelimiter;
	private int maxSize;

	public StringsPreferenceValue(char delimiter, String prefsKey, String pluginId) {
		this(delimiter, NO_MAXSIZE, prefsKey, pluginId);
	}
	
	public StringsPreferenceValue(char delimiter, int maxSize, String prefsKey, String pluginId) {
		super(prefsKey, pluginId);
		this.delimiter = String.valueOf(delimiter);
		this.maxSize = maxSize;
		try {
			this.escapedDelimiter = URLEncoder.encode(String.valueOf(delimiter), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// cannot happen since we use a fixed, known encoding
		}
	}

	@Override
	public String[] get() {
		return split(doGet());
	}

	private String[] split(String string) {
		ArrayList<String> values = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(string, delimiter);
		while (tokenizer.hasMoreTokens()) {
			String value = tokenizer.nextToken();
			String unescapedValue = unescapeDelimiterCharacter(value);
			values.add(unescapedValue);
		}
		return values.toArray(new String[values.size()]);
	}

	/**
	 * Adds the given string value to this preference value(s) and stores it. 
	 * Duplicate values are not added. 
	 * 
	 * @param value
	 *            the value to add
	 */
	public void add(String value) {
		String currentValues = doGet();
		value = escapeDelimiterCharacter(value);
		if (!isContained(value, currentValues)) {
			StringBuilder builder = new StringBuilder(currentValues);
			if (!isEmpty(currentValues)) {
				String[] values = split(currentValues);
				if (maxSize != NO_MAXSIZE
						&& values.length >= maxSize) {
					values = shiftLeft(value, values);
					values[maxSize - 1] = value;
					builder = new StringBuilder(concatenate(values));
				} else {
					builder.append(delimiter).append(value);
				}
			} else {
				builder.append(value);
			}
			doStore(builder.toString());
		}
	}

	protected String[] shiftLeft(String value, String[] values) {
		String[] copy = new String[maxSize];
		System.arraycopy(values, 1, copy, 0, maxSize - 1);
		return copy;
	}

	protected String escapeDelimiterCharacter(String value) {
		if (value == null || value.length() == 0) {
			return value;
		}

		int index = value.indexOf(delimiter);
		if (index < 0) {
			return value;
		}
		StringBuilder builder = new StringBuilder(value.substring(0, index));
		builder.append(escapedDelimiter);
		builder.append(value.substring(index + 1));
		return builder.toString();
	}

	protected String unescapeDelimiterCharacter(String value) {
		if (value == null || value.length() == 0) {
			return value;
		}

		int index = value.indexOf(escapedDelimiter);
		if (index < 0) {
			return value;
		}
		StringBuilder builder = new StringBuilder(value.substring(0, index));
		builder.append(delimiter);
		builder.append(value.substring(index + 1));
		return builder.toString();
	}

	protected boolean isContained(String value, String currentValues) {
		return currentValues != null
				&& currentValues.length() > 0
				&& currentValues.indexOf(value) >= 0;
	}

	protected boolean isEmpty(String currentValues) {
		return currentValues == null 
					|| currentValues.length() == 0;
	}

	/**
	 * Removes the given values and
	 * stores the preferences.
	 * 
	 * @param values
	 *            the values
	 */
	public void remove(String... valuesToRemove) {
		boolean removed = false;
		String[] currentValues = get();
		if (valuesToRemove != null) {
			for (int i = 0; i < currentValues.length; i++) {
				for (String valueToRemove : valuesToRemove) {
					if (valueToRemove.equals(currentValues[i])) {
						currentValues[i] = null;
						removed = true;
					}
				}
			}
		}
		if (removed) {
			doStore(concatenate(currentValues));
		}
	}

	protected String concatenate(String[] values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				if (builder.length() > 0) {
					builder.append(delimiter);
				}
				builder.append(values[i]);
			}
		}
		return builder.toString();
	}
	
	@Override
	public void set(String[] values) {
		doStore(concatenate(values));
	}
	
	public int size() {
		return get().length;
	}
}
