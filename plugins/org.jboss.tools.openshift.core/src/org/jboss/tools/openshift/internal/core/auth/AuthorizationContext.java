/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.auth;

import org.jboss.tools.openshift.core.auth.IAuthorizationContext;

public class AuthorizationContext implements IAuthorizationContext {
	
	private String token;
	private String expires;
	private boolean authorized;
	private AuthorizationType type;

	public AuthorizationContext(String token, String expires){
		this.authorized = true;
		this.token = token;
		this.expires = expires;
	}

	public AuthorizationContext(AuthorizationType type) {
		this.type = type;
		this.authorized = false;
	}

	@Override
	public String getToken(){
		return this.token;
	}
	
	@Override
	public String getExpiresIn(){
		return expires;
	}
	
	@Override
	public AuthorizationType getType() {
		return type;
	}
	
	@Override
	public boolean isAuthorized() {
		return authorized;
	}
	
}
