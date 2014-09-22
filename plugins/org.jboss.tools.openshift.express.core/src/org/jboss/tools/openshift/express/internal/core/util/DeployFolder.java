/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.util;

import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public enum DeployFolder {

	JBOSSAS(IStandaloneCartridge.NAME_JBOSSAS, "deployments"), 
	JBOSSEAP(IStandaloneCartridge.NAME_JBOSSEAP, "deployments"), 
	WILDFLY8("wildfly", "deployments"),
	JBOSSEWS(IStandaloneCartridge.NAME_JBOSSEWS, "webapps");

	private String cartridgeName;
	private String deployFolder;

	DeployFolder(String cartridgeName, String deployFolder) {
		this.cartridgeName = cartridgeName;
		this.deployFolder = deployFolder;
	}
	
	public String getDeployFolder() {
		return deployFolder;
	}
	
	public static DeployFolder getByCartridgeName(String cartridgeName) {
		if (cartridgeName == null) {
			return null;
		}
		
		for (DeployFolder deployFolder : values()) {
			if (cartridgeName.startsWith(deployFolder.cartridgeName)) {
				return deployFolder;
			}
		}
		return null;
	}
}