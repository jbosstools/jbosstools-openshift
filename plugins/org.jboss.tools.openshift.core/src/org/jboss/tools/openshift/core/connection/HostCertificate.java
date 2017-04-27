package org.jboss.tools.openshift.core.connection;
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
import java.util.Objects;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * A host certificate that the user can accept or refute
 * 
 * @author adietish
 */
public class HostCertificate {

	private final static String NL = StringUtils.getLineSeparator();

	private boolean accepted = false;
	private String issuedBy;
	private String validity;
	private String fingerprint;

	public HostCertificate(boolean accepted, String issuedBy, String validity, String fingerprint) {
		this.accepted = accepted;
		this.issuedBy = issuedBy;
		this.validity = validity;
		this.fingerprint = fingerprint;
	}

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean checked) {
		this.accepted = checked;
	}

	public String getIssuer() {
		return issuedBy;
	}

	public String getValidity() {
		return validity;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof HostCertificate)) return false;
		HostCertificate other = (HostCertificate)o;
		return Objects.equals(this.issuedBy, other.issuedBy)
			&& Objects.equals(this.validity, other.validity)
			&& Objects.equals(this.fingerprint, other.fingerprint);
	}

	/**
	 * Returns human readable presentation to be shown in the table,
	 * with same text as in SSLCertificateDialog.
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Issued By:").append(NL).append('\t').append(issuedBy).append(NL);
		sb.append("Validity:").append(NL).append('\t').append(validity).append(NL);
		sb.append("SHA1 Fingerprint:").append(NL).append('\t').append(fingerprint).append(NL);
		return sb.toString();
	}

}