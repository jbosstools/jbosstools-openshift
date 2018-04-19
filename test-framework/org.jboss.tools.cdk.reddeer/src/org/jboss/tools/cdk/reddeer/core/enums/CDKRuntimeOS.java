/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.enums;

/**
 * CDK runtime OS enum types used with minishift, ie. in url, folder names, etc.
 * 
 * @author odockal
 *
 */
public enum CDKRuntimeOS {

	LINUX("linux", "linux", ""), 
	WINDOWS("win", "windows", ".exe"), 
	MAC("darwin", "darwin", ""), 
	OTHER;

	String runtimeName;
	String runtimeFullName;
	String suffix;

	CDKRuntimeOS() {
		this.runtimeName = System.getProperty("os.name");
		this.suffix = "";
	}

	CDKRuntimeOS(String name, String runtimeFullName, String suffix) {
		this.runtimeName = name;
		this.runtimeFullName = runtimeFullName;
		this.suffix = suffix;
	}

	public String getRuntimeName() {
		return this.runtimeName;
	}

	public String getRuntimeFullName() {
		return this.runtimeFullName;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public static CDKRuntimeOS get() {
		String baseOS = System.getProperty("os.name").toLowerCase();
		if (baseOS.contains("linux")) {
			return CDKRuntimeOS.LINUX;
		} else if (baseOS.contains("windows")) {
			return WINDOWS;
		} else if (baseOS.contains("mac")) {
			return CDKRuntimeOS.MAC;
		} else {
			return CDKRuntimeOS.OTHER;
		}
	}
}
