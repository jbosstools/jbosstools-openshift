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
import org.jboss.tools.openshift.common.core.utils.ExtensionUtils;

import com.openshift.restclient.ISSLCertificateCallback;

/**
 * Allows the core plugin to call UI contributions provided by the ui-plugin
 * (org.jboss.tools.openshift.ui)
 * 
 * @author Rob Stryker
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftCoreUIIntegration {

	private static final String SSLCERTIFICATE_CALLBACK_UI_EXTENSION = "org.jboss.tools.openshift.core.sslCertificateCallbackUI";
	private static final String CREDENTIALS_PROMPTER_UI_EXTENSION = "org.jboss.tools.openshift.core.credentialsPrompterUI";
	private static final String ROUTE_CHOOSER_EXTENSION = "org.jboss.tools.openshift.core.routeChooser";
	
	private static final String ATTRIBUTE_CLASS = "class";

	private static OpenShiftCoreUIIntegration INSTANCE = new OpenShiftCoreUIIntegration();

	public static OpenShiftCoreUIIntegration getInstance(){
		return INSTANCE;
	}

	protected ISSLCertificateCallback sslCertificateCallback;
	protected ICredentialsPrompter credentialPrompter;
	private IRouteChooser browser;

	// for testing purposes
	protected OpenShiftCoreUIIntegration() {
	}

	public ISSLCertificateCallback getSSLCertificateCallback() {
		if (sslCertificateCallback == null) {
			sslCertificateCallback = ExtensionUtils.getFirstExtension(SSLCERTIFICATE_CALLBACK_UI_EXTENSION, ATTRIBUTE_CLASS);
		}
		return sslCertificateCallback;
	}
	
	public ICredentialsPrompter getCredentialPrompter() {
		if (credentialPrompter == null) {
			this.credentialPrompter = ExtensionUtils.getFirstExtension(CREDENTIALS_PROMPTER_UI_EXTENSION, ATTRIBUTE_CLASS);
		}
		return credentialPrompter;
	}

	public IRouteChooser getRouteChooser() {
		if (browser == null) {
			this.browser = ExtensionUtils.getFirstExtension(ROUTE_CHOOSER_EXTENSION, ATTRIBUTE_CLASS);
		}
		return browser;
	}

}
