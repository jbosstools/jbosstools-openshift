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
package org.jboss.tools.openshift.common.core.connection;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * A host certificate that the user can accept or refute
 * 
 * @author Andre Dietisheim
 */
public class HostCertificate {

	private boolean accepted = false;
	private X509Certificate certificate;

	public HostCertificate(boolean accepted, X509Certificate certificate) {
		this.accepted = accepted;
		this.certificate = certificate;
	}

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean checked) {
		this.accepted = checked;
	}

	public byte[] getEncoded() throws CertificateEncodingException {
		return certificate.getEncoded();
	}

	public boolean isValid() {
		if (certificate == null) {
			return false;
		}

		try {
			certificate.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
		return true;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((certificate == null) ? 0 : certificate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostCertificate other = (HostCertificate) obj;
		if (certificate == null) {
			if (other.certificate != null)
				return false;
		} else if (!certificate.equals(other.certificate))
			return false;
		return true;
	}

	/**
	 * Returns human readable presentation to be shown in the table, with same
	 * text as in SSLCertificateDialog.
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Accepted:").append('\t').append(accepted).append('\n');
		if (certificate != null) {
			HumanReadableX509Certificate humanCertificate = new HumanReadableX509Certificate(certificate);
			sb.append("Issued To:").append('\t').append(humanCertificate.getIssuedTo()).append('\n');
			sb.append("Issued By:").append('\t').append(humanCertificate.getIssuedBy()).append('\n');
			sb.append("Validity:").append('\t').append(humanCertificate.getValidity()).append('\n');
			sb.append("SHA1 Fingerprint:").append('\t').append(humanCertificate.getFingerprint()).append('\n');
		}
		return sb.toString();
	}
}