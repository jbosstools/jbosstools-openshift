/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.certificate;

/**
 * Class represents basic SSL certificate information
 * @author odockal
 *
 */
public class UntrustedSSLCertificate {

	private String issuedTo;
	private String issuedBy;
	private String sha1Fingerprint;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((issuedBy == null) ? 0 : issuedBy.hashCode());
		result = prime * result + ((issuedTo == null) ? 0 : issuedTo.hashCode());
		result = prime * result + ((sha1Fingerprint == null) ? 0 : sha1Fingerprint.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UntrustedSSLCertificate))
			return false;
		UntrustedSSLCertificate other = (UntrustedSSLCertificate) obj;
		if (issuedBy == null) {
			if (other.issuedBy != null)
				return false;
		} else if (!issuedBy.equals(other.issuedBy))
			return false;
		if (sha1Fingerprint == null) {
			if (other.sha1Fingerprint != null)
				return false;
		} else if (!sha1Fingerprint.equals(other.sha1Fingerprint))
			return false;
		return true;
	}

	public UntrustedSSLCertificate(String issuedTo, String issuedBy, String sha1Fingerprint) {
		this.issuedTo = issuedTo;
		this.issuedBy = issuedBy;
		this.sha1Fingerprint = sha1Fingerprint;
	}
	
	public String getIssuedTo() {
		return issuedTo;
	}

	public void setIssuedTo(String issuedTo) {
		this.issuedTo = issuedTo;
	}

	public String getIssuedBy() {
		return issuedBy;
	}

	public void setIssuedBy(String issuedBy) {
		this.issuedBy = issuedBy;
	}

	public String getFingerprint() {
		return sha1Fingerprint;
	}

	public void setFingerprint(String sha1Fingerprint) {
		this.sha1Fingerprint = sha1Fingerprint;
	}
	
	

}
