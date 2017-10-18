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
package org.jboss.tools.cdk.reddeer.server.adapter;

public enum CDKServerAdapterType {

	CDK2 	("org.jboss.tools.openshift.cdk.server.type"),
	CDK3 	("org.jboss.tools.openshift.cdk.server.type.v3"),
	CDK32 	("org.jboss.tools.openshift.cdk.server.type.v32"),
	NO_CDK  ("");
	
	private final String serverType;
	
	CDKServerAdapterType(String type) {
		this.serverType = type;
	}
	
	public String serverType() { 
		return this.serverType; 
	}
}
