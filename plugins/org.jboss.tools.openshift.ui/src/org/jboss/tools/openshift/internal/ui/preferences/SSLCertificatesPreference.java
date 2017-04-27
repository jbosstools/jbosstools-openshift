/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.openshift.common.core.utils.HumanReadableX509Certificate;
import org.jboss.tools.openshift.core.connection.HostCertificate;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

public class SSLCertificatesPreference {
	private final static String ALLOWED_CERTIFICATES = "allowed_certificates";
	private final static String SEPARATOR = ";";

	private final static SSLCertificatesPreference INSTANCE = new SSLCertificatesPreference();

	private List<HostCertificate> savedItems = null;
	private SSLCertificatesPreference() {}

	public static SSLCertificatesPreference getInstance() {
		return INSTANCE;
	}


	protected IPreferenceStore getPreferenceStore() {
		return OpenShiftUIActivator.getDefault().getCorePreferenceStore();
	}

	/**
	 * Called by SSLCertificateCallback to retrieve the choice made in the dialog.
	 * 
	 * @param certificate
	 * @return
	 */
	public Boolean getAllowedByCertificate(X509Certificate certificate) {
		HumanReadableX509Certificate p = new HumanReadableX509Certificate(certificate);
		HostCertificate i = new HostCertificate(false, p.getIssuer(), p.getValidity(), p.getFingerprint());
		HostCertificate item = findItem(getSavedItems(), i);
		return item != null ? item.isAccepted() : null;
	}

	/**
	 * Called by SSLCertificateCallback to remember the choice made in the dialog.
	 *  
	 * @param certificate
	 * @return
	 */
	public void setAllowedByCertificate(X509Certificate certificate, boolean result) {
		HumanReadableX509Certificate p = new HumanReadableX509Certificate(certificate);
		HostCertificate i = new HostCertificate(result, p.getIssuer(), p.getValidity(), p.getFingerprint());
		List<HostCertificate> savedItems = getSavedItems();
		HostCertificate item = findItem(savedItems, i);
		if (item == null) {
			savedItems.add(i);
		} else {
			item.setAccepted(result);
		}
		saveItemsToPreference(savedItems);
	}

	/**
	 * Called by preference page.
	 * Each item is stored as  
	 * 	checked SEPARATOR issuedBy SEPARATOR validity SEPARATOR fingerprint SEPARATOR
	 * 
	 * @return the single list instance, which is never replaced, only its content is changed
	 */
	List<HostCertificate> getSavedItems() {
		if(savedItems == null) {
			List<HostCertificate> savedItems = new ArrayList<>();
			String value = getPreferenceStore().getString(ALLOWED_CERTIFICATES);
			if(value != null && value.length() > 0) {
				String[] tokens = value.split(SEPARATOR);
				for (int i = 0; i + 4 <= tokens.length;) {
					boolean checked = "true".equals(tokens[i++]);
					String issuedBy = tokens[i++];
					String validity = tokens[i++];
					String fingerprint = tokens[i++];
					if(isValid(validity)) {
						savedItems.add(new HostCertificate(checked, issuedBy, validity, fingerprint));
					}
				}
			}
			this.savedItems = savedItems;
		}
		return savedItems;
	}

	private boolean isValid(String validity) {
		return HumanReadableX509Certificate.isValid(validity);
	}

	private HostCertificate findItem(List<HostCertificate> items, HostCertificate item) {
		if (items == null || items.isEmpty()) {
			return null;
		}
		return items.stream().filter(i -> i.equals(item)).findFirst().orElse(null);
	}

	/**
	 * Called by preference page
	 * @param items
	 */
	void saveWorkingCopy(List<HostCertificate> items) {
		List<HostCertificate> savedItems = getSavedItems();
		savedItems.clear();
		savedItems.addAll(items);
		saveItemsToPreference(savedItems);
	}

	private void saveItemsToPreference(List<HostCertificate> items) {
		StringBuilder sb = new StringBuilder();
		items.stream().forEach(i -> sb.append(toPreferenceValue(i)));
		getPreferenceStore().setValue(ALLOWED_CERTIFICATES, sb.toString());
		if (getPreferenceStore() instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) getPreferenceStore()).save();
			} catch (IOException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
			}
		}
	}
	
	/**
	 * Returns text to be stored in preferences.
	 * 
	 * @return
	 */
	public String toPreferenceValue(HostCertificate certificate) {
		StringBuilder sb = new StringBuilder();
		sb.append(certificate.isAccepted()).append(SEPARATOR)
		  .append(certificate.getIssuer()).append(SEPARATOR)
		  .append(certificate.getValidity()).append(SEPARATOR)
		  .append(certificate.getFingerprint()).append(SEPARATOR);
		return sb.toString();
	}
}
