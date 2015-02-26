/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import org.jboss.tools.openshift.common.core.ICredentialsPrompter;

import com.openshift.client.IHttpClient.ISSLCertificateCallback;

public class OpenShiftCoreUIIntegration {

	private static OpenShiftCoreUIIntegration instance;
	
	public static OpenShiftCoreUIIntegration getInstance(){
		if(instance == null)
			instance = new OpenShiftCoreUIIntegration();
		return instance;
	}
	
	private ISSLCertificateCallback sslCertificateCallback;
	private ICredentialsPrompter credentialPrompter;
	
	public ISSLCertificateCallback getSSLCertificateCallback() {
		return sslCertificateCallback;
	}
	
	public void setSSLCertificateAuthorization(ISSLCertificateCallback authorization) {
		this.sslCertificateCallback = authorization;
	}
	
	public ICredentialsPrompter getCredentialPrompter() {
		return credentialPrompter;
	}
	
	public void setCredentialPrompter(ICredentialsPrompter prompter) {
		this.credentialPrompter = prompter;
	}
}
