/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * Parses a given X509 certificate and offers human readable portions of it.
 * 
 * @author Andre Dietisheim
 */
public class HumanReadableX509Certificate {

	public static final String PRINCIPAL_COMMON_NAME = "CN";
	public static final String PRINCIPAL_ORGANISATIONAL_UNIT = "OU";
	public static final String PRINCIPAL_ORGANISATION = "O";
	public static final String PRINCIPAL_LOCALITY = "L";
	public static final String PRINCIPAL_STATE = "ST";
	public static final String PRINCIPAL_COUNTRY = "C";

	public static final String LABEL_PRINCIPAL_COMMON_NAME = "Common Name (CN)";
	public static final String LABEL_PRINCIPAL_ORGANISATIONAL_UNIT = "Organisational Unit (OU)";
	public static final String LABEL_PRINCIPAL_ORGANISATION = "Organisation (O)";
	public static final String LABEL_PRINCIPAL_LOCALITY = "Locality (L)";
	public static final String LABEL_PRINCIPAL_STATE = "State (ST)";
	public static final String LABEL_PRINCIPAL_COUNTRY = "Country (C)";

	private static final String DATE_FORMAT = "E, d MMM yyyy HH:mm:ss z";
	public static final String LABEL_VALIDITY_ISSUED_ON = "Issued On ";
	public static final String LABEL_VALIDITY_EXPIRES_ON = "Expires On ";

	public static final String SEPARATOR_LABEL_VALUE = ": ";

	private static final String HEX_CHARS = "0123456789ABCDEF";

	private String validity;
	private String fingerprint;
	private X509Certificate certificate;

	public HumanReadableX509Certificate(X509Certificate certificate) {
		Assert.isLegal(certificate != null);

		this.validity = getValidity(certificate);
		this.fingerprint = getFingerprint(certificate);
		this.certificate = certificate;
	}

	public String getValidity() {
		return validity;
	}

	/**
	 * Returns all relative 
	 * @return
	 */
	public String getIssuedBy() {
		return getAllRDN(certificate.getIssuerX500Principal());
	}

	/**
	 * Returns the relative distinguished name within the issuer principal of
	 * this certificate that's identified by the given type (name). The
	 * constants PRINCIPAL_* may be used for this sake. Returns {@code null} if
	 * it cannot be found. Returns all relative distinguished names if the given
	 * name is {@code null}.<br>
	 * <br>
	 * 
	 * ex. {@code
	 * 	humandReadableX509Certificate.getIssuedBy(HumanReadableX509Certificate.PRINCIPAL_COMMON_NAME);
	 * }
	 * 
	 * @param name
	 * @return the value for the given name within the issuer, null if not
	 *         found or all relative distinguished names in the issuer.
	 * 
	 * @see X500Principal
	 * @see #PRINCIPAL_COMMON_NAME
	 * @see #PRINCIPAL_ORGANISATION
	 * @see #PRINCIPAL_COUNTRY
	 */
	public String getIssuedBy(String name) {
		return getRDNValue(name, certificate.getIssuerX500Principal());
	}

	public String getIssuedTo() {
		return getAllRDN(certificate.getSubjectX500Principal());
	}

	/**
	 * Returns the relative distinguished name within the subject principal of
	 * this certificate that's identified by the given type (name). The
	 * constants PRINCIPAL_* may be used for this sake. Returns {@code null} if
	 * it cannot be found. Returns all relative distinguished names if the given
	 * name is {@code null}.<br>
	 * <br>
	 * 
	 * ex. {@code
	 * 	humandReadableX509Certificate.getIssuedTo(HumanReadableX509Certificate.PRINCIPAL_COMMON_NAME);
	 * }
	 * 
	 * @param name
	 * @return the value for the given name within the issuer, null if not
	 *         found or all relative distinguished names in the issuer.
	 * 
	 * @see X500Principal
	 * @see #PRINCIPAL_COMMON_NAME
	 * @see #PRINCIPAL_ORGANISATION
	 * @see #PRINCIPAL_COUNTRY
	 */
	public String getIssuedTo(String name) {
		return getRDNValue(name, certificate.getSubjectX500Principal());
	}

	public String getFingerprint() {
		return this.fingerprint;
	}

	private String getRDNValue(String type, X500Principal principal) {
		if (StringUtils.isEmpty(type)) {
			return getAllRDN(principal);
		}

		try {
			LdapName ldapName = new LdapName(principal.getName());
			for (Rdn rdn : ldapName.getRdns()) {
				if (type.equals(rdn.getType())) {
					return StringUtils.toStringOrNull(rdn.getValue());
				}
			}
			return null;
		} catch (InvalidNameException e) {
			return null;
		}
	}

	private String getAllRDN(X500Principal principal) {
		StringBuilder builder = new StringBuilder();
		try {
			LdapName ldapDN = new LdapName(principal.getName());
			int i = 0;
			for (Rdn rdn : ldapDN.getRdns()) {
				String type = getTypeFullName(rdn.getType());
				if (!StringUtils.isEmpty(type)) {
					if (i++ > 0) {
						builder.append(StringUtils.getLineSeparator());
					}
					builder
						.append(type)
						.append(SEPARATOR_LABEL_VALUE)
						.append(StringUtils.toStringOrNull(rdn.getValue()));
				}
			}
			return builder.toString();
		} catch (InvalidNameException e) {
			return builder.toString();
		}
	}

	private String getValidity(X509Certificate certificate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
		return new StringBuilder()
					.append(LABEL_VALIDITY_ISSUED_ON).append(SEPARATOR_LABEL_VALUE)
						.append(dateFormat.format(certificate.getNotBefore())).append(StringUtils.getLineSeparator())
					.append(LABEL_VALIDITY_EXPIRES_ON).append(SEPARATOR_LABEL_VALUE)
						.append(dateFormat.format(certificate.getNotAfter()))
					.toString();
	}

	private String getFingerprint(X509Certificate certificate) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			sha1.update(certificate.getEncoded());
			return toHexString(sha1.digest());
		} catch (NoSuchAlgorithmException e) {
			return "<Could not determine fingerprint>";
		} catch (CertificateEncodingException e) {
			return "<Could not determine fingerprint>";
		}
	}

	private String toHexString(byte bytes[]) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2 + 0] = HEX_CHARS.charAt((v & 0xF0) >> 4);
			hexChars[j * 2 + 1] = HEX_CHARS.charAt(v & 0x0F);
		}
		return new String(hexChars);
	}

	private String getTypeFullName(String type) {
		switch (type) {
		case PRINCIPAL_COUNTRY:
			return LABEL_PRINCIPAL_COUNTRY;
		case PRINCIPAL_STATE:
			return LABEL_PRINCIPAL_STATE;
		case PRINCIPAL_LOCALITY:
			return LABEL_PRINCIPAL_LOCALITY;
		case PRINCIPAL_ORGANISATION:
			return LABEL_PRINCIPAL_ORGANISATION;
		case PRINCIPAL_ORGANISATIONAL_UNIT:
			return LABEL_PRINCIPAL_ORGANISATIONAL_UNIT;
		case PRINCIPAL_COMMON_NAME:
			return LABEL_PRINCIPAL_COMMON_NAME;
		default:
			return null;
		}
	}
}