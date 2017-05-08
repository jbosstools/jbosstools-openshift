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
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_COMMON_NAME;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_COUNTRY;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_LOCALITY;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_ORGANISATION;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_ORGANISATIONAL_UNIT;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_PRINCIPAL_STATE;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_VALIDITY_EXPIRES_ON;
import static org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate.LABEL_VALIDITY_ISSUED_ON;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_REDHAT_COM;

import java.security.cert.CertificateException;

import org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate;
import org.jboss.tools.openshift.internal.ui.utils.SSLCertificateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class HumanReadableX509CertificateTest {

	private HumanReadableX509Certificate certificate;

	@Before
	public void setup() throws Exception {
		this.certificate = new HumanReadableX509Certificate(SSLCertificateUtils.createX509Certificate(CERTIFICATE_REDHAT_COM));
	}

	@Test
	public void shouldReportIssuedTo() throws CertificateException {
		// given
		// when
		String issuedTo = certificate.getIssuedTo();
		// then
		assertThat(getValue(LABEL_PRINCIPAL_COUNTRY, issuedTo)).isEqualTo("US");
		assertThat(getValue(LABEL_PRINCIPAL_STATE, issuedTo)).isEqualTo("NORTH CAROLINA");
		assertThat(getValue(LABEL_PRINCIPAL_LOCALITY, issuedTo)).isEqualTo("Raleigh");
		assertThat(getValue(LABEL_PRINCIPAL_ORGANISATION, issuedTo)).isEqualTo("Red Hat, Inc.");
		assertThat(getValue(LABEL_PRINCIPAL_ORGANISATIONAL_UNIT, issuedTo)).isEqualTo("IT");
		assertThat(getValue(LABEL_PRINCIPAL_COMMON_NAME, issuedTo)).isEqualTo("www.redhat.com");
	}

	@Test
	public void shouldReportIssuedToCommonName() {
		// given
		// when
		String issuedToCommonName = certificate.getIssuedTo(HumanReadableX509Certificate.PRINCIPAL_COMMON_NAME);
		// then
		assertThat(issuedToCommonName).isEqualTo("www.redhat.com");
	}

	@Test
	public void shouldReportIssuedBy() throws CertificateException {
		// given
		// when
		String issuedBy = certificate.getIssuedBy();
		// then
		assertThat(getValue(LABEL_PRINCIPAL_COUNTRY, issuedBy)).isEqualTo("GB");
		assertThat(getValue(LABEL_PRINCIPAL_STATE, issuedBy)).isEqualTo("Greater Manchester");
		assertThat(getValue(LABEL_PRINCIPAL_LOCALITY, issuedBy)).isEqualTo("Salford");
		assertThat(getValue(LABEL_PRINCIPAL_ORGANISATION, issuedBy)).isEqualTo("COMODO CA Limited");
		assertThat(getValue(LABEL_PRINCIPAL_COMMON_NAME, issuedBy)).isEqualTo("COMODO RSA Extended Validation Secure Server CA 2");
	}

	@Test
	public void shouldReportIssuedByState() {
		// given
		// when
		String issuedToCommonName = certificate.getIssuedBy(HumanReadableX509Certificate.PRINCIPAL_STATE);
		// then
		assertThat(issuedToCommonName).isEqualTo("Greater Manchester");
	}

	@Test
	public void shouldReportFingerPrint() throws CertificateException {
		// given
		// when
		String fingerprint = certificate.getFingerprint();
		// then
		assertThat(fingerprint).isEqualTo("A1C3587B794993BCFD0AFD023BAA07680F20B3F2");
	}

	@Test
	public void shouldReportValidity() throws CertificateException {
		// given
		// when
		String validity = certificate.getValidity();
		// then
		assertThat(getValue(LABEL_VALIDITY_ISSUED_ON, validity)).isEqualTo("Sat, 5 Sep 2015 02:00:00");
		assertThat(getValue(LABEL_VALIDITY_EXPIRES_ON, validity)).isEqualTo("Sun, 3 Sep 2017 01:59:59");
	}

	private String getValue(String identifier, String string) {
		assertThat(string).isNotEmpty();

		int valueIndexOf = string.indexOf(identifier);
		assertThat(valueIndexOf).as("identifier %s could not be found in %s", identifier, string).isNotNegative();

		int eolIndexOf = string.indexOf('\n', valueIndexOf);
		if (eolIndexOf == -1 ) {
			eolIndexOf = string.length();
		}
		return string.substring(valueIndexOf + identifier.length() + HumanReadableX509Certificate.SEPARATOR_LABEL_VALUE.length(), eolIndexOf);
	}
}	
