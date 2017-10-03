/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represent the data stored in the JSON payload returned when OSIO refresh has been
 * completed.
 */
public class RefreshResponse {

	@JsonProperty("token")
	private LoginResponse loginResponse;

	/**
	 * @return the loginResponse
	 */
	public LoginResponse getLoginResponse() {
		return loginResponse;
	}


	@JsonAnySetter
	public void setOther(String name, Object value) {
	}
}
