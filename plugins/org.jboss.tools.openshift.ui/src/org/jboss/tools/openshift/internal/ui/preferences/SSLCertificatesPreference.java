/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.preferences;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.HostCertificate;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.utils.SSLCertificateUtils;

/**
 * Handles (Loads, saves, etc.) SSL certificates in the Eclipse preferences
 * 
 * @author Viacheslav Kabanovich
 * @author Rob Stryker
 * @author Andre Dietisheim
 */
public class SSLCertificatesPreference {

	private final static String SEPARATOR = ";";

	/** key for deprecated incompletely stored ssl certificates **/
	@SuppressWarnings("unused")
	private final static String ALLOWED_CERTIFICATES = "allowed_certificates";
	
	/** key for ssl certificates **/
	private final static String ALLOWED_CERTIFICATES_NEW = "allowed_certificates_new";

	private final static SSLCertificatesPreference INSTANCE = new SSLCertificatesPreference();

	private List<HostCertificate> savedItems = null;

	protected SSLCertificatesPreference() {}

	public static enum CertificateState {
		ACCEPTED, REJECTED, NOT_PRESENT;
	}
	
	public static SSLCertificatesPreference getInstance() {
		return INSTANCE;
	}

	/**
	 * Called by SSLCertificateCallback to retrieve the choice made in the dialog.
	 * 
	 * @param certificate
	 * @return
	 */
	public CertificateState isAllowed(X509Certificate certificate) {
		HostCertificate lookedUp = new HostCertificate(false, certificate);
		HostCertificate found = findCertificate(getSavedCertificates(), lookedUp);
		if (found == null) {
			return CertificateState.NOT_PRESENT;
		}
		if (found.isAccepted()) {
			return CertificateState.ACCEPTED;
		} else {
			return CertificateState.REJECTED;
		}
	}

	/**
	 * Adds a new host certificate with the given x509certificate and
	 * acceptance. Updates the certificate if it exists already.
	 * 
	 * @param certificate the x509certificate that should be added
	 * @param the acceptance given 
	 * @return
	 * 
	 * @see X509Certificate
	 */
	public void addOrReplaceCertificate(X509Certificate certificate, boolean accepted) {
		HostCertificate newCertificate = new HostCertificate(accepted, certificate);
		List<HostCertificate> savedItems = getSavedCertificates();
		HostCertificate item = findCertificate(savedItems, newCertificate);
		if (item == null) {
			savedItems.add(newCertificate);
		} else {
			item.setAccepted(accepted);
		}
		saveToPreference(savedItems);
	}

	/**
	 * Returns the existing certificates from preferences
	 * Each item is stored as  
	 * 	checked SEPARATOR issuedBy SEPARATOR validity SEPARATOR fingerprint SEPARATOR
	 * 
	 * @return the single list instance, which is never replaced, only its content is changed
	 */
	public synchronized List<HostCertificate> getSavedCertificates() {
		if (savedItems == null) {
			this.savedItems = loadSavedCertificates();
		}
		return savedItems;
	}

	private synchronized List<HostCertificate> loadSavedCertificates() {
		List<HostCertificate> savedItems = new ArrayList<>();
		String value = getPreferenceStore().getString(ALLOWED_CERTIFICATES_NEW);
		if (!StringUtils.isBlank(value)) {
			String[] tokens = value.split(SEPARATOR);
			for (int i = 0; i + 2 <= tokens.length;) {
				boolean accepted = Boolean.toString(Boolean.TRUE).equals(tokens[i++]);
				X509Certificate certificate = createCertificate(tokens[i++]);
				if (certificate == null) {
					continue;
				}
				HostCertificate hostCertificate = new HostCertificate(accepted, certificate);
				if (hostCertificate.isValid()) {
					savedItems.add(hostCertificate);
				}
			}
		}
		return savedItems;
	}

	protected X509Certificate createCertificate(String certificateString) {
		try {
			return SSLCertificateUtils.createX509Certificate(certificateString);
		} catch (CertificateException e) {
			IStatus status = StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID, 
					NLS.bind("Could not read certificate for certificate {0}", 
							StringUtils.abbreviate(certificateString, 50)));
			OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
		}
		return null;
	}

	private HostCertificate findCertificate(List<HostCertificate> existingCertificates, HostCertificate lookedUp) {
		if (existingCertificates == null 
				|| existingCertificates.isEmpty()) {
			return null;
		}
		return existingCertificates.stream()
				.filter(existing -> existing.equals(lookedUp)).findFirst().orElse(null);
	}

	/**
	 * Add the given certificates to the existing ones and save them to the preferences.
	 * 
	 * @param certificates
	 */
	public void save(Collection<HostCertificate> certificates) {
		List<HostCertificate> savedItems = getSavedCertificates();
		savedItems.clear();
		savedItems.addAll(certificates);
		saveToPreference(savedItems);
	}

	private synchronized void saveToPreference(List<HostCertificate> certificates) {
		String certificatesString = toString(certificates);
		IPreferenceStore store = getPreferenceStore();
		store.setValue(ALLOWED_CERTIFICATES_NEW, certificatesString);
		save(store);
	}

	private synchronized void save(IPreferenceStore store) {
		if (store instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
			}
		}
	}

	private String toString(List<HostCertificate> certificates) {
		StringBuilder builder = new StringBuilder();
		certificates.stream().forEach(certificate -> {
			String base64Certificate = toPreferenceValue(certificate);
			if (!StringUtils.isBlank(base64Certificate)) {
				builder
					.append(base64Certificate)
					.append(SEPARATOR);
			}
		});
		return builder.toString();
	}
	
	/**
	 * Returns the given HostCertificate as a string that can be stored in preferences. 
	 * 
	 * @param HostCertificate certificate
	 * 
	 * @returns 
	 */
	private String toPreferenceValue(HostCertificate certificate) {
		try {
			return new StringBuilder()
					.append(certificate.isAccepted()).append(SEPARATOR)
					.append(SSLCertificateUtils.toString(certificate.getCertificate()))
					.toString();
		} catch (CertificateEncodingException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(
					NLS.bind("Could not encode certificate {0}", certificate.toString()), e);
			return  null;
		}
	}
	
	protected IPreferenceStore getPreferenceStore() {
		return OpenShiftUIActivator.getDefault().getCorePreferenceStore();
	}
}
