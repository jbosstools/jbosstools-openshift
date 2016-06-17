/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.jmx;

/**
 * This Enum is used to specifiy a particular server version.
 * @author Thomas Mäder
 *
 */
public enum ServerType {
	EAP("eap"), WILDFLY("wildfly"), JBOSS("jboss");
	
	private String idValue;

	private ServerType(String idValue) {
		this.idValue = idValue;
	};
	
	String getIdValue() {
		return idValue;
	}

	public static ServerType fromString(String serverType) {
		for (ServerType type : ServerType.values()) {
			if (type.idValue.equals(serverType)) {
				return type;
			}
		}
		throw new IllegalArgumentException("unknown server type: "+serverType);
	}

}
