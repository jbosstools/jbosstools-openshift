/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.enums;

import org.apache.commons.lang.StringUtils;

/**
 * Enum represents authentication method from OpenShift 3 connection
 * 
 * @author mladbuda, odockal
 *
 */
public enum AuthenticationMethod {

	DEFAULT(""),
	BASIC("Basic"),
	OAUTH("OAuth");
	
	private final String text;
	
	private AuthenticationMethod(String text) {
		this.text = text;
	}
	
	@Override
	public String toString() {
		return text;
	}

	public static AuthenticationMethod valueOfIgnoreCase(String value) {
		return valueOf(StringUtils.upperCase(value));
	}

	public static AuthenticationMethod safeValueOf(String value) {
		try {
			return valueOfIgnoreCase(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
