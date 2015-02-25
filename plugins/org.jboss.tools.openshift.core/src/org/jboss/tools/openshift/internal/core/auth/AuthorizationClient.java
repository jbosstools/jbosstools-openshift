/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.auth;

import static org.jboss.tools.openshift.common.core.utils.URIUtils.splitFragment;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.tools.openshift.core.auth.IAuthorizationClient;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.client.NoopSSLCertificateCallback;
import com.openshift.client.IHttpClient.ISSLCertificateCallback;
import com.openshift.client.OpenShiftException;

@SuppressWarnings("restriction")
public class AuthorizationClient  implements IAuthorizationClient{

	public static final String ACCESS_TOKEN = "access_token";
	private SSLContext sslContext;
	private X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
	
	public AuthorizationClient(){
		setSSLCertificateCallback(new NoopSSLCertificateCallback());
	}
	
	public String requestToken(final String baseURL, final String username, final String password){
		CloseableHttpResponse response = null;
		CloseableHttpClient client = null; 
		try {
			client = HttpClients.custom()
					.setRedirectStrategy(new OpenShiftAuthorizationRedirectStrategy())
					.setDefaultCredentialsProvider(buildCredentialsProvider(username, password))
					.setHostnameVerifier(hostnameVerifier)
					.setSslcontext(sslContext)
					.build();
			HttpGet request = new HttpGet(
					new URIBuilder(String.format("%s/oauth/authorize", baseURL))
						.addParameter("response_type", "token")
						.addParameter("client_id", "openshift-challenging-client")
						.build()
						);
			response = client.execute(request);
			return getAccessToken(response);
		} catch (URISyntaxException e) {
			logWarn("", e);
			throw new OpenShiftException(e,"");
		} catch (ClientProtocolException e) {
			logWarn("", e);
			throw new OpenShiftException(e,"");
		} catch (IOException e) {
			logWarn("", e);
			throw new OpenShiftException(e,"");
		}finally{
			close(response);
			close(client);
		}
	}
	
	private String getAccessToken(CloseableHttpResponse response) {
		Header header = response.getFirstHeader("Location");
		return splitFragment(header.getValue()).get(ACCESS_TOKEN);
	}

	private void close(Closeable closer){
		if(closer == null) return;
		try {
			closer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private CredentialsProvider buildCredentialsProvider(final String username, final String password){
		CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(
				//TODO: limit scope on host?
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(username, password));
		return provider;
	}
	
	@Override
	public void setSSLCertificateCallback(ISSLCertificateCallback callback) {
		X509TrustManager trustManager = null;
		if(callback != null){
			trustManager = createCallbackTrustManager(callback);
		}
		try {
			this.sslContext = SSLContext.getInstance("TLS");
			this.sslContext.init(null, new TrustManager [] {trustManager},null);
		} catch (NoSuchAlgorithmException e){
			logWarn("Could not install trust manager callback", e);
			this.sslContext = null;
		}catch(KeyManagementException e) {
			logWarn("Could not install trust manager callback", e);
			this.sslContext = null;
		}
	}
	private void logWarn(String message){
		logWarn(message, null);
	}
	private void logWarn(String message, Throwable e){
		OpenShiftCoreActivator.pluginLog().logWarning(message,e);
	}
	//TODO REPLACE me with osjc impl
	private X509TrustManager createCallbackTrustManager(ISSLCertificateCallback sslAuthorizationCallback) {
		X509TrustManager trustManager = null;
		try {
			trustManager = getCurrentTrustManager();
			if (trustManager == null) {
				logWarn("Could not install trust manager callback, no trustmanager was found.");
			} else {
				trustManager = new CallbackTrustManager(trustManager, sslAuthorizationCallback);
			}
		} catch (GeneralSecurityException e) {
			logWarn("Could not install trust manager callback.", e);
		}
		return trustManager;
	}
	
	//TODO replace me with OSJC implementation
	private X509TrustManager getCurrentTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustManagerFactory = 
				TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init((KeyStore) null);

		X509TrustManager x509TrustManager = null;
		for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
			if (trustManager instanceof X509TrustManager) {
				x509TrustManager = (X509TrustManager) trustManager;
				break;
			}
		}
		return x509TrustManager;
	}

	//TODO - Replace me with instance in OSJC
	private static class CallbackTrustManager implements X509TrustManager {

		private X509TrustManager trustManager;
		private ISSLCertificateCallback callback;

		private CallbackTrustManager(X509TrustManager currentTrustManager, ISSLCertificateCallback callback) 
				throws NoSuchAlgorithmException, KeyStoreException {
			this.trustManager = currentTrustManager; 
			this.callback = callback;
		}
		
		public X509Certificate[] getAcceptedIssuers() {
			return trustManager.getAcceptedIssuers();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				trustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				if (!callback.allowCertificate(chain)) {
					throw e;
				}
			}
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			trustManager.checkServerTrusted(chain, authType);
		}
	}
	
	
}
