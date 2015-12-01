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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;

import org.jboss.tools.openshift.core.LazySSLCertificateCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ISSLCertificateCallback;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class LazySSLCertificateCallbackTest {

	private LazySSLCertificateCallback lazyCallback;

	@Mock
	private ISSLCertificateCallback permissiveExtensionCallback;
	@Mock
	private ISSLCertificateCallback denyingExtensionCallback;
	private X509Certificate[] certs = new X509Certificate[] { mock(X509Certificate.class) };
	@Mock
	private SSLSession session;
	
	@Before
	public void setup(){
		lazyCallback = spy(new LazySSLCertificateCallback());
		when(permissiveExtensionCallback.allowCertificate(any(X509Certificate[].class))).thenReturn(true);
		when(permissiveExtensionCallback.allowHostname(any(String.class), any(SSLSession.class))).thenReturn(true);
		when(denyingExtensionCallback.allowCertificate(any(X509Certificate[].class))).thenReturn(false);
		when(denyingExtensionCallback.allowHostname(any(String.class), any(SSLSession.class))).thenReturn(false);
	}
	
	@Test
	public void testAllowCertificateWhenHasCallback() {
		when(lazyCallback.getExtension()).thenReturn(permissiveExtensionCallback);

		assertTrue("Exp. to allow certs", lazyCallback.allowCertificate(certs));
		verify(permissiveExtensionCallback, times(1)).allowCertificate(any(X509Certificate[].class));
	}

	@Test
	public void testDisallowCertificateWhenHasCallback() {
		when(lazyCallback.getExtension()).thenReturn(denyingExtensionCallback);

		assertFalse("Exp. to disallow certs", lazyCallback.allowCertificate(certs));
		verify(denyingExtensionCallback, times(1)).allowCertificate(any(X509Certificate[].class));
	}

	@Test
	public void testDisallowCertificateWhenHasNoCallback() {
		when(lazyCallback.getExtension()).thenReturn(null);

		assertFalse("Exp. to disallow certs", lazyCallback.allowCertificate(certs));
		verify(denyingExtensionCallback, never()).allowCertificate(any(X509Certificate[].class));
	}

	@Test
	public void testVerifyHostnameCertificateWhenHasCallback() {
		when(lazyCallback.getExtension()).thenReturn(permissiveExtensionCallback);

		assertTrue("Exp. to allow certs", lazyCallback.allowHostname(any((String.class)), any(SSLSession.class)));
		verify(permissiveExtensionCallback, times(1)).allowHostname(any((String.class)), any(SSLSession.class));
	}

	@Test
	public void testWontVerifyHostnameWhenHasCallback() {
		when(lazyCallback.getExtension()).thenReturn(denyingExtensionCallback);

		assertFalse("Exp. to not verify hostname", lazyCallback.allowHostname(any((String.class)), any(SSLSession.class)));
		verify(denyingExtensionCallback, times(1)).allowHostname(any((String.class)), any(SSLSession.class));
	}

	@Test
	public void testWontVerifyHostnameWhenHasNoCallback() {
		when(lazyCallback.getExtension()).thenReturn(null);

		assertFalse("Exp. to not verify hostname", lazyCallback.allowHostname(any((String.class)), any(SSLSession.class)));
		verify(denyingExtensionCallback, never()).allowHostname(any((String.class)), any(SSLSession.class));
	}
}
