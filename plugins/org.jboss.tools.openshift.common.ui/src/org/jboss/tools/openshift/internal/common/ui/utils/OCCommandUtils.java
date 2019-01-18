/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.regex.Pattern;

import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * Util class for parsing oc command.
 * @author Josef Kopriva
 */
public class OCCommandUtils {

	/**
	 * Validates is oc command is in correct format with required fields.
	 * 
	 * @param ocCommand
	 * @return true command has correct format
	 * @return false command has not correct format
	 */
	public static boolean isValidCommand(String ocCommand) {
		return ocCommand.trim().startsWith("oc login ") && (getAuthMethod(ocCommand) != null)
				&& (getServer(ocCommand) != null);
	}

	/**
	 * Returns authorization schema of oc command(basic/OAuth).
	 * 
	 * @param ocCommand
	 * @return auth schema
	 */
	public static String getAuthMethod(String ocCommand) {
		ocCommand = ocCommand.trim();
		if (ocCommand.contains(" -u") || ocCommand.contains(" --username")) {
			return IAuthorizationContext.AUTHSCHEME_BASIC;
		} else if (ocCommand.contains("--token=")) {
			return IAuthorizationContext.AUTHSCHEME_OAUTH;
		} else {
			return null;
		}
	}

	/**
	 * Parses server address from oc command.
	 * 
	 * @param ocCommand
	 * @return server address
	 */
	public static String getServer(String ocCommand) {
		return applyPattern(ocCommand, "(?<=\\s)https[a-zA-Z0-9:/.-]+", 0);
	}

	/**
	 * Parses token from oc command.
	 * 
	 * @param ocCommand
	 * @return token
	 */
	public static String getToken(String ocCommand) {
		return applyPattern(ocCommand, "(?<=--token=)[a-zA-Z0-9:]+", 0);
	}

	/**
	 * Parses username from oc command.
	 * 
	 * @param ocCommand
	 * @return username
	 */
	public static String getUsername(String ocCommand) {
		return applyPattern(ocCommand, "(?<=-u\\s)[a-zA-Z0-9:]+", 0);
	}

	/**
	 * Parses password from oc command.
	 * 
	 * @param ocCommand
	 * @return password
	 */
	public static String getPassword(String ocCommand) {
		return searchInStringForPattern(ocCommand, "(?<=-p\\s)(.*)(?=\\b)", "-p");
	}

	private static String searchInStringForPattern(String stringToVerify, String pattern, String shouldContain) {
		if (stringToVerify.contains(shouldContain)) {
			return applyPattern(stringToVerify, pattern);
		}
		return null;
	}

	private static String applyPattern(String stringToVerify, String pattern) {
		return applyPattern(stringToVerify, pattern, 1);
	}

	private static String applyPattern(String stringToVerify, String pattern, int group) {
		stringToVerify = stringToVerify.trim();
		Pattern patternToken = Pattern.compile(pattern);
		java.util.regex.Matcher matcherToken = patternToken.matcher(stringToVerify);
		if (matcherToken.find()) {
			return matcherToken.group(group);
		}
		return null;
	}

}
