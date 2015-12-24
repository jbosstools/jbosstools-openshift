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
package org.jboss.tools.openshift.common.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.eclipse.core.runtime.Assert;

/**
 * Parses a given X509 certificate and offers human readable portions of it.
 *  
 * @author Andre Dietisheim
 */
public class X509CertificateParser {
	public static final String DATE_FORMAT = "E, d MMM yyyy HH:mm:ss";
	public static final String ISSUED_ON_PREFIX = "Issued On: ";
	public static final String EXPIRES_ON_PREFIX = "Expires On: ";

		private static final String HEX_CHARS = new String("0123456789ABCDEF");
		
		private String issuer;
		private String validity;
		private String fingerprint;

		public X509CertificateParser(X509Certificate certificate) {
			Assert.isLegal(certificate != null);
			this.issuer = getIssuer(certificate);
			this.validity = getValidity(certificate);
			this.fingerprint = getFingerprint(certificate);
		}

		public String getValidity() {
			return validity;
		}

		public String getIssuer() {
			return issuer;
		}

		public String getFingerprint() {
			return this.fingerprint;
		}
		
		private String getIssuer(X509Certificate certificate) {
			try {
				StringBuilder builder = new StringBuilder();
				LdapName ldapDN = new LdapName(certificate.getSubjectX500Principal().getName());
				for (Rdn rdn : ldapDN.getRdns()) {
					String type = getTypeFullName(rdn.getType());
					if (StringUtils.isEmpty(type)) {
						builder.append("Serial Number: ").append(toHexString(rdn.getValue().toString().getBytes()));
					} else {
						builder.append(type).append(": ").append(rdn.getValue()).append('\n');
					}
				}

				return builder.toString();
			} catch (InvalidNameException e) {
				return "<Could not determine certificate issuer>";
			}
		}

		private String getValidity(X509Certificate certificate) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			return new StringBuilder()
				.append(ISSUED_ON_PREFIX).append(dateFormat.format(certificate.getNotBefore())).append('\n')
				.append(EXPIRES_ON_PREFIX).append(dateFormat.format(certificate.getNotAfter()))
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
			if ("C".equals(type)) {
				return "Country (C)";
			} else if ("ST".equals(type)) {
				return "State (ST)";
			} else if ("CN".equals(type)) {
				return "Common Name (CN)";
			} else if ("O".equals(type)) {
				return "Organization (O)";
			} else if ("OU".equals(type)) {
				return "Organizational Unit (OU)";
			}
			return null;
		}
	}