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
package org.jboss.tools.cdk.reddeer.core.enums;

/**
 * Server adapters types
 * @author odockal
 *
 */
public enum CDKServerAdapterType {

	CDK2 	("CDK", "org.jboss.tools.openshift.cdk.server.type", "CDK"),
	CDK3 	("CDK 3.0+", "org.jboss.tools.openshift.cdk.server.type.v3", "CDK"),
	CDK32 	("CDK 3.2+", "org.jboss.tools.openshift.cdk.server.type.v32", "CDK"),
	MINISHIFT17 ("Minishift", "org.jboss.tools.openshift.cdk.server.type.minishift.v17", "Minishift"),
	NO_CDK  ("", "", "");
	
	private final String runtimeTypeName;
	private final String serverType;
	private final String serverTypeName;
	
	CDKServerAdapterType(String runtimeTypeName, String type,String name) {
		this.runtimeTypeName = runtimeTypeName;
		this.serverType = type;
		this.serverTypeName = name;
	}
	
	public String serverType() { 
		return this.serverType; 
	}
	
	public String serverTypeName() {
		return this.serverTypeName;
	}
	
	public String runtimeTypeName() {
		return this.runtimeTypeName;
	}
}
