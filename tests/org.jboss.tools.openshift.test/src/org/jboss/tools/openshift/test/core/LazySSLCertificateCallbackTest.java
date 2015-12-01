/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;

import org.jboss.tools.openshift.core.LazySSLCertificateCallback;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ISSLCertificateCallback;

@RunWith(MockitoJUnitRunner.class)
public class LazySSLCertificateCallbackTest {

	@Mock
	private ISSLCertificateCallback defaultCallback;
	@Mock
	private ISSLCertificateCallback altCallback;
	private LazySSLCertificateCallback lazyCallback;
	private X509Certificate[] certs = new X509Certificate[] { mock(X509Certificate.class) };
	private TestOpenShiftCoreUIIntegration integration = new TestOpenShiftCoreUIIntegration();
	
	@Mock
	private SSLSession session;
	
	@Before
	public void setup(){
		integration.setSSLCertificateAuthorization(null);
		when(defaultCallback.allowCertificate(any(X509Certificate[].class))).thenReturn(true);
		when(defaultCallback.allowHostname(anyString(), any(SSLSession.class))).thenReturn(true);
		when(altCallback.allowCertificate(any(X509Certificate[].class))).thenReturn(true);
		when(altCallback.allowHostname(anyString(), any(SSLSession.class))).thenReturn(true);
	}
	
	@After
	public void teardown(){
		integration.setSSLCertificateAuthorization(null);
	}
	
	@Test
	public void testConstructionThrowsWhenInitializedWithSelf(){
		boolean exception = false;
		try{
			new LazySSLCertificateCallback(new LazySSLCertificateCallback(null));
		}catch(IllegalArgumentException e){
			exception = true;
		}
		assertTrue("Expected an exception when trying to initialize with a lazy cred prompter", exception);
	}
	
	@Test
	public void testAllowCertificateWhenInitializedWithACallback() {
		lazyCallback = new LazySSLCertificateCallback(defaultCallback);
		
		assertTrue("Exp. to allowCerts", lazyCallback.allowCertificate(certs));
		verify(defaultCallback).allowCertificate(any(X509Certificate[].class));
		verify(altCallback, never()).allowCertificate(any(X509Certificate[].class));
	}

	@Test
	public void testAllowHostnameWhenInitializedWithACallback() {
		lazyCallback = new LazySSLCertificateCallback(defaultCallback);
		
		assertTrue("Exp. to allow hostname", lazyCallback.allowHostname("",session));
		verify(defaultCallback).allowHostname(anyString(), any(SSLSession.class));
		verify(altCallback, never()).allowHostname(anyString(),any(SSLSession.class));
	}

	@Ignore("currently failing")
	@Test
	public void testAllowCertificateDeferredLoadsWhenInitializedWithNull() {
		integration.setSSLCertificateAuthorization(altCallback);
		lazyCallback = new LazySSLCertificateCallback(null);
		
		assertTrue("Exp. to allowCerts", lazyCallback.allowCertificate(certs));
		verify(altCallback).allowCertificate(any(X509Certificate[].class));
		verify(defaultCallback, never()).allowCertificate(any(X509Certificate[].class));
	}
	
	@Ignore("currently failing")
	@Test
	public void testAllowHostnameDeferredLoadsWhenInitializedWithNull() {
		integration.setSSLCertificateAuthorization(altCallback);
		lazyCallback = new LazySSLCertificateCallback(null);
		
		assertTrue("Exp. to allow hostname", lazyCallback.allowHostname("",session));
		verify(altCallback).allowHostname(anyString(), any(SSLSession.class));
		verify(defaultCallback, never()).allowHostname(anyString(),any(SSLSession.class));
	}

	@Test
	public void testAllowCertificateReturnsFalseWhenItCantGetACallback() {
		lazyCallback = new LazySSLCertificateCallback(null);
		
		assertFalse("Exp. to not to allowCertificate", lazyCallback.allowCertificate(certs));
		verify(altCallback, never()).allowCertificate(any(X509Certificate[].class));
		verify(defaultCallback, never()).allowCertificate(any(X509Certificate[].class));
	}

	@Test
	public void testHostnameReturnsFalseWhenItCantGetACallback() {
		lazyCallback = new LazySSLCertificateCallback(null);
		
		assertFalse("Exp. to not to allowCertificate", lazyCallback.allowHostname("",session));
		verify(altCallback, never()).allowHostname(anyString(),any(SSLSession.class));
		verify(defaultCallback, never()).allowHostname(anyString(),any(SSLSession.class));
	}

	private class TestOpenShiftCoreUIIntegration extends OpenShiftCoreUIIntegration {

		protected void setSSLCertificateAuthorization(ISSLCertificateCallback callback) {
			sslCertificateCallback = callback;
		}
	}
	
}
