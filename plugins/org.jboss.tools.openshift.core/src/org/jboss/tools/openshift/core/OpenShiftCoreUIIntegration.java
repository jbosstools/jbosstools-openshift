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

import com.openshift.restclient.ISSLCertificateCallback;

/**
 * @author Rob Stryker
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftCoreUIIntegration {

	private static OpenShiftCoreUIIntegration INSTANCE = new OpenShiftCoreUIIntegration();
	
	public static OpenShiftCoreUIIntegration getInstance(){
		return INSTANCE;
	}
	
	private ISSLCertificateCallback sslCertificateCallback;
	private ICredentialsPrompter credentialPrompter;
	
	public ISSLCertificateCallback getSSLCertificateCallback() {
		return sslCertificateCallback;
	}
	
	public void setSSLCertificateAuthorization(ISSLCertificateCallback callback) {
		this.sslCertificateCallback = callback;
	}
	
	public ICredentialsPrompter getCredentialPrompter() {
		return credentialPrompter;
	}
	
	public void setCredentialPrompter(ICredentialsPrompter prompter) {
		this.credentialPrompter = prompter;
	}
}
