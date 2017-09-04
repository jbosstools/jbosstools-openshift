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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.jboss.tools.openshift.common.core.connection.HumanReadableX509Certificate;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
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

	private static final String DATE_FORMAT = "E, d MMM yyyy HH:mm:ss z";

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
		assertThat(getValue(LABEL_PRINCIPAL_STATE, issuedTo)).isEqualTo("North Carolina");
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
		assertThat(getValue(LABEL_PRINCIPAL_COUNTRY, issuedBy)).isEqualTo("US");
        assertThat(getValue(LABEL_PRINCIPAL_ORGANISATION, issuedBy)).isEqualTo("Symantec Corporation");
        assertThat(getValue(LABEL_PRINCIPAL_ORGANISATIONAL_UNIT, issuedBy)).isEqualTo("Symantec Trust Network");
		assertThat(getValue(LABEL_PRINCIPAL_COMMON_NAME, issuedBy)).isEqualTo("Symantec Class 3 EV SSL CA - G3");
	}

	@Test
	public void shouldReportFingerPrint() throws CertificateException {
		// given
		// when
		String fingerprint = certificate.getFingerprint();
		// then
		assertThat(fingerprint).isEqualTo("917871FF8DC3E10638AEE136547F4E85C1404D2D");
	}

	@Test
	public void shouldReportValidity() throws CertificateException, ParseException {
		// given
		SimpleDateFormat dateParser = new SimpleDateFormat(DATE_FORMAT, Locale.US);
		Calendar expectedIssuedOn = getCalendar(dateParser.parse("Tue, 6 Jun 2017 01:00:00 CET"));
		Calendar expectedExpiresOn = getCalendar(dateParser.parse("Fri, 7 Jun 2019 00:59:59 CET"));
		// when
		String validity = certificate.getValidity();
		// then
		Calendar issuedOn = getCalendar(dateParser.parse(getValue(LABEL_VALIDITY_ISSUED_ON, validity)));
		assertThat(issuedOn).isEqualTo(expectedIssuedOn);
		Calendar expiresOn = getCalendar(dateParser.parse(getValue(LABEL_VALIDITY_EXPIRES_ON, validity)));
		assertThat(expiresOn).isEqualTo(expectedExpiresOn);
	}

	private Calendar getCalendar(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}
	
	private String getValue(String identifier, String string) {
		assertThat(string).isNotEmpty();

		int valueIndexOf = string.indexOf(identifier);
		assertThat(valueIndexOf).as("identifier %s could not be found in %s", identifier, string).isNotNegative();

		int eolIndexOf = string.indexOf(StringUtils.getLineSeparator(), valueIndexOf);
		if (eolIndexOf == -1 ) {
			eolIndexOf = string.length();
		}
		return string.substring(valueIndexOf + identifier.length() + HumanReadableX509Certificate.SEPARATOR_LABEL_VALUE.length(), eolIndexOf);
	}
}	
