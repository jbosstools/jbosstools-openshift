/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.core.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.internal.ui.utils.SSLCertificateUtils.createX509Certificate;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_OPENSHIFT_REDHAT_COM;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_REDHAT_COM;

import java.security.cert.CertificateException;

import org.jboss.tools.openshift.common.core.connection.HostCertificate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class HostCertificateTest {

	private HostCertificate certificate;

	@Before
	public void setup() throws Exception {
		this.certificate = new HostCertificate(true, createX509Certificate(CERTIFICATE_REDHAT_COM));
	}

	@Test
	public void shouldEqualIdenticalCertificate() throws CertificateException {
		// given
		HostCertificate identicalCertificate = new HostCertificate(true, createX509Certificate(CERTIFICATE_REDHAT_COM));
		// when
		boolean equal = certificate.equals(identicalCertificate);
		// then
		assertThat(equal).isTrue();
	}

	@Test
	public void shouldEqualIdenticalCertificateThatDiffersInAcceptance() throws CertificateException {
		// given
		HostCertificate identicalCertificate = new HostCertificate(false, createX509Certificate(CERTIFICATE_REDHAT_COM));
		// when
		boolean equal = certificate.equals(identicalCertificate);
		// then
		assertThat(equal).isTrue();
	}

	@Test
	public void shouldNotEqualOtherCertificate() throws CertificateException {
		// given
		HostCertificate identicalCertificate = new HostCertificate(false, createX509Certificate(CERTIFICATE_OPENSHIFT_REDHAT_COM));
		// when
		boolean equal = certificate.equals(identicalCertificate);
		// then
		assertThat(equal).isFalse();
	}

	@Test
	public void shouldReturnIsAccepted() throws CertificateException {
		// given
		// when
		boolean accepted = certificate.isAccepted();
		// then
		assertThat(accepted).isTrue();
	}

	@Test
	public void shouldSetAccepted() throws CertificateException {
		// given
		boolean accepted = certificate.isAccepted();
		// when
		certificate.setAccepted(!accepted);
		// then
		assertThat(certificate.isAccepted()).isEqualTo(!accepted);
	}

	@Test
	public void shouldReturnIsValid() throws CertificateException {
		// given
		// when
		boolean valid = certificate.isValid();
		// then
		assertThat(valid).isTrue();
	}

}	
