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
package org.jboss.tools.openshift.reddeer.enums;

public enum ResourceState {

	UNSPECIFIED(""),
	PENDING("Pending"),
	RUNNING("Running"),
	COMPLETE("Complete"),
	SUCCEEDED("Succeeded"),
	FAILED("Failed");
	
	private String state;
	
	private ResourceState(String state) {
		this.state = state;
	}
	
	public String toString() {
		return state;
	}
}
