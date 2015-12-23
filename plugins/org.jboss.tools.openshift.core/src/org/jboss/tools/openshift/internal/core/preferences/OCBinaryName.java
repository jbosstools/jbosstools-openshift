/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.internal.core.preferences;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;

public enum OCBinaryName {

	WINDOWS("oc.exe", new String[] { "exe" }), 
	OTHER("oc", new String[] {});
	
	private String name;
	private String[] extensions;
	private CommandLocationBinary locationBinary;
	private OCBinaryName(String name, String[] extensions) {
		this.name = name;
		this.extensions = extensions;
	}

	public String getName() {
		return name;
	};

	public String[] getExtensions() {
		return extensions;
	};

	public String getLocation() {
		if( locationBinary == null ) {
			locationBinary = new CommandLocationBinary("oc");
			locationBinary.addPlatformLocation(Platform.OS_LINUX, "/usr/bin/oc");
			locationBinary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return locationBinary.findLocation();
	}
	
	public static OCBinaryName getInstance() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return WINDOWS;
		} else {
			return OTHER;
		}
	}
}
