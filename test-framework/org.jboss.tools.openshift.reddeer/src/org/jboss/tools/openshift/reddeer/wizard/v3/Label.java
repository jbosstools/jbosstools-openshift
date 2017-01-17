/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v3;

/**
 * OpenShift label.
 * 
 * @author mlabuda@redhat.com
 */
public class Label {

	private String name;
	private String value;
	
	/**
	 * Creates new OpenShift label with specified name and value.
	 * 
	 * @param name name of label
	 * @param value value of label
	 */
	public Label(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
}
