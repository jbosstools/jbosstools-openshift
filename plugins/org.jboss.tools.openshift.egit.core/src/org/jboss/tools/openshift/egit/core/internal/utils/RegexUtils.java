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
package org.jboss.tools.openshift.egit.core.internal.utils;

/**
 * @author Andre Dietisheim
 */
public class RegexUtils {

	public static String toPatternString(String string) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < string.length(); ++i) {
			char ch = string.charAt(i);
			if ("\\.^$|?*+[]{}()".indexOf(ch) != -1) {
				// reserved char char
				builder.append('\\').append(ch);
//			} else if (Character.isLetter(ch)) {
//				// letter
//				builder.append("[A-Za-z]");
//			} else if (Character.isDigit(ch)) {
//				// digit
//				builder.append("\\d");
			} else {
				builder.append(ch);
			}
		}
		return builder.toString();
	}

}
