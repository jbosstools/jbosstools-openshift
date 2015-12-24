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

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.X509CertificateParser;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

public class SSLCertificatesPreference {
	final static String ALLOWED_CERTIFICATES = "allowed_certificates";
	final static String SEPARATOR = ";";
	final static String NL = StringUtils.getLineSeparator();

	final static SSLCertificatesPreference instance = new SSLCertificatesPreference();

	public static SSLCertificatesPreference getInstance() {
		return instance;
	}

	private List<Item> savedItems = null;

	private SSLCertificatesPreference() {}

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
		X509CertificateParser p = new X509CertificateParser(certificate);
		Item i = new Item(false, p.getIssuer(), p.getValidity(), p.getFingerprint());
		Item item = findItem(getSavedItems(), i);
		return item != null ? item.checked : null;
	}

	/**
	 * Called by SSLCertificateCallback to remember the choice made in the dialog.
	 *  
	 * @param certificate
	 * @return
	 */
	public void setAllowedByCertificate(X509Certificate certificate, boolean result) {
		X509CertificateParser p = new X509CertificateParser(certificate);
		Item i = new Item(result, p.getIssuer(), p.getValidity(), p.getFingerprint());
		List<Item> savedItems = getSavedItems();
		Item item = findItem(savedItems, i);
		if(item == null) {
			savedItems.add(i);
		} else {
			item.checked = result;
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
	List<Item> getSavedItems() {
		if(savedItems == null) {
			List<Item> savedItems = new ArrayList<>();
			String value = getPreferenceStore().getString(ALLOWED_CERTIFICATES);
			if(value != null && value.length() > 0) {
				String[] tokens = value.split(SEPARATOR);
				for (int i = 0; i + 4 <= tokens.length;) {
					boolean checked = "true".equals(tokens[i++]);
					String issuedBy = tokens[i++];
					String validity = tokens[i++];
					String fingerprint = tokens[i++];
					if(isValid(validity)) {
						savedItems.add(new Item(checked, issuedBy, validity, fingerprint));
					}
				}
			}
			this.savedItems = savedItems;
		}
		return savedItems;
	}

	private boolean isValid(String validity) {
		int expiresOnIndex = validity.indexOf(X509CertificateParser.EXPIRES_ON_PREFIX);
		if(expiresOnIndex >= 0) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(X509CertificateParser.DATE_FORMAT);
			String expiresOn = validity.substring(expiresOnIndex + X509CertificateParser.EXPIRES_ON_PREFIX.length()).trim();
			try {
				Date date = dateFormat.parse(expiresOn);
				return date.getTime() > System.currentTimeMillis();
			} catch (ParseException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError("SSLCertificatesPreference.isValid(String): Could not parse '" + expiresOn + "' in format " + X509CertificateParser.DATE_FORMAT);
			}
		}
		//In case of any failure, just assume that certificate is valid.
		return true;
	}

	private Item findItem(List<Item> items, Item item) {
		if (items == null || items.isEmpty()) {
			return null;
		}
		return items.stream().filter(i -> i.equals(item)).findFirst().orElse(null);
	}

	/**
	 * Called by preference page
	 * @param items
	 */
	void saveWorkingCopy(List<Item> items) {
		List<Item> savedItems = getSavedItems();
		savedItems.clear();
		savedItems.addAll(items);
		saveItemsToPreference(savedItems);
	}

	private void saveItemsToPreference(List<Item> items) {
		StringBuilder sb = new StringBuilder();
		items.stream().forEach(i->sb.append(i.toPreferenceValue()));
		getPreferenceStore().setValue(ALLOWED_CERTIFICATES, sb.toString());
	}

	/**
	 * Used by preference page to show in the checkbox table viewer.
	 * @param items
	 */
	static class Item {
		boolean checked = false;
		String issuedBy;
		String validity;
		String fingerprint;

		Item(boolean checked, String issuedBy, String validity, String fingerprint) {
			this.checked = checked;
			this.issuedBy = issuedBy;
			this.validity = validity;
			this.fingerprint = fingerprint;
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
	
		public String getIssuer() {
			return issuedBy;
		}

		public String getValidity() {
			return validity;
		}

		public String getFingerprint() {
			return fingerprint;
		}

		/**
		 * Returns text to be stored in preferences.
		 * 
		 * @return
		 */
		public String toPreferenceValue() {
			StringBuilder sb = new StringBuilder();
			sb.append(checked).append(SEPARATOR)
			  .append(issuedBy).append(SEPARATOR)
			  .append(validity).append(SEPARATOR)
			  .append(fingerprint).append(SEPARATOR);
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if(!(o instanceof Item)) return false;
			Item other = (Item)o;
			return Objects.equals(this.issuedBy, other.issuedBy)
				&& Objects.equals(this.validity, other.validity)
				&& Objects.equals(this.fingerprint, other.fingerprint);
		}
	}

}
