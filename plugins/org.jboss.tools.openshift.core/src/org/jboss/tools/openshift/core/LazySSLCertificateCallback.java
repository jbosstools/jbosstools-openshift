/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;

import com.openshift.restclient.ISSLCertificateCallback;

public class LazySSLCertificateCallback implements ISSLCertificateCallback {

	private ISSLCertificateCallback callback;

	@Override
	public boolean allowCertificate(X509Certificate[] certs) {
		if(!loadCallback()) return false;
		return callback.allowCertificate(certs);
	}

	@Override
	public boolean allowHostname(String hostname, SSLSession session) {
		if(!loadCallback()) return false;
		return callback.allowHostname(hostname, session);
	}
	
	private boolean loadCallback(){
		if(callback == null) {
			callback = getExtension();
			return callback != null;
		}
		return true;
	}

	// for testing purposes
	public ISSLCertificateCallback getExtension() {
		return OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback();
	}
}
