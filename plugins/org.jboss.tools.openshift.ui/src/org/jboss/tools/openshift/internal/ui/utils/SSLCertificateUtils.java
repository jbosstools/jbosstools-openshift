/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.utils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class SSLCertificateUtils {

	public static final String BEGIN_CERTIFICATE_MARKER = "-----BEGIN CERTIFICATE-----\n";
	public static final String END_CERTIFICATE_MARKER = "-----END CERTIFICATE-----";

	public static final String CERTIFICATE_FACTORY_X509 = "X.509";

	private SSLCertificateUtils() {
	}

	/**
	 * Creates a {@link X509Certificate} for the given string. The string has to
	 * start with {@link #BEGIN_CERTIFICATE_MARKER} and end with
	 * {@link #END_CERTIFICATE_MARKER}. Otherwise a {@link CertificateException}
	 * will be thrown.
	 * 
	 * @param certificateString
	 * @return
	 * @throws CertificateException if creation fails
	 * 
	 * @see X509Certificate
	 */
	public static X509Certificate createX509Certificate(String certificateString) throws CertificateException {
		if (StringUtils.isEmpty(certificateString)) {
			return null;
		}
		CertificateFactory factory = CertificateFactory.getInstance(CERTIFICATE_FACTORY_X509);
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateString.getBytes()));
	}

	/**
	 * Returns a string representation of the given x509 certificate. The certificate is base64 encoded.
	 * 
	 * @param certificate
	 * @return
	 * @throws CertificateEncodingException
	 */
	public static String toString(X509Certificate certificate) throws CertificateEncodingException {
		StringBuilder sb = new StringBuilder()
			.append(BEGIN_CERTIFICATE_MARKER)
			.append(Base64.getEncoder().encodeToString(certificate.getEncoded()))
			.append(END_CERTIFICATE_MARKER);
		return sb.toString();
	}
	
}
