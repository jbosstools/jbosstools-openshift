package org.jboss.tools.openshift.core.connection;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;

import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;

import com.openshift.restclient.ISSLCertificateCallback;

public class LazySSLCertificateCallback implements ISSLCertificateCallback {

	private ISSLCertificateCallback callback;

	public LazySSLCertificateCallback(ISSLCertificateCallback callback) {
		if(callback instanceof LazySSLCertificateCallback){
			throw new IllegalArgumentException("Unable to initialize a LazySSLCertificateCallback with instance of the same type");
		}
		this.callback = callback;
	}

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
		if(callback == null){
			callback = OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback();
			if(callback == null) return false;
		}
		return true;
	}
}
