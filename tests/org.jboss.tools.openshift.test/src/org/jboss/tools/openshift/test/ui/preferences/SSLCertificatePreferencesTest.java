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
package org.jboss.tools.openshift.test.ui.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_OPENSHIFT_REDHAT_COM;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_OPEN_PAAS_REDHAT_COM;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.CERTIFICATE_REDHAT_COM;
import static org.jboss.tools.openshift.test.util.SSLCertificateMocks.createHostCertificate;
import static org.jboss.tools.openshift.internal.ui.utils.SSLCertificateUtils.createX509Certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.jboss.tools.openshift.common.core.connection.HostCertificate;
import org.jboss.tools.openshift.internal.ui.preferences.SSLCertificatesPreference;
import org.jboss.tools.openshift.internal.ui.preferences.SSLCertificatesPreference.CertificateState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class SSLCertificatePreferencesTest {
	
	private TestableSSLCertificatesPreference preference;

	@Before
	public void setUp() throws Exception {
		this.preference = new TestableSSLCertificatesPreference();
		List<HostCertificate> certificates = Arrays.asList(
				createHostCertificate(true, CERTIFICATE_REDHAT_COM), 
				createHostCertificate(false, CERTIFICATE_OPEN_PAAS_REDHAT_COM));
		preference.save(certificates);
	}

	@Test
	public void shouldSaveAndLoad2Certificates() throws Exception {
		// given
		// when
		// then
		assertThat(preference.getSavedCertificates()).hasSize(2);
	}

	@Test
	public void shouldReplaceExistingCertificatesWhenSaving() throws Exception {
		// given
		// when
		preference.save(Collections.singletonList(createHostCertificate(true, CERTIFICATE_OPENSHIFT_REDHAT_COM)));
		// then
		assertThat(preference.getSavedCertificates()).hasSize(1);
	}

	@Test
	public void shouldAddCertificate() throws CertificateException {
		// given
		assertThat(preference.getSavedCertificates()).hasSize(2);
		X509Certificate x509 = createX509Certificate(CERTIFICATE_OPENSHIFT_REDHAT_COM);
		// when
		preference.addOrReplaceCertificate(x509, true);
		// then
		assertThat(preference.getSavedCertificates()).hasSize(3);
	}

	@Test
	public void shouldReplaceCertificate() throws CertificateException {
		// given
		assertThat(preference.getSavedCertificates()).hasSize(2);
		X509Certificate x509 = createX509Certificate(CERTIFICATE_REDHAT_COM);
		assertThat(preference.isAllowed(x509)).isSameAs(CertificateState.ACCEPTED);
		// when
		preference.addOrReplaceCertificate(x509, false);
		// then
		assertThat(preference.getSavedCertificates()).hasSize(2);
		assertThat(preference.isAllowed(x509)).isSameAs(CertificateState.REJECTED);
	}

	@Test
	public void shouldReturnAcceptanceForExistingCertificate() throws CertificateException {
		// given
		X509Certificate x509 = createX509Certificate(CERTIFICATE_REDHAT_COM);
		// when
		CertificateState allowed = preference.isAllowed(x509);
		// then
		assertThat(allowed).isSameAs(CertificateState.ACCEPTED);
	}

	@Test
	public void shouldReturnNotPresentForNonExistingCertificate() throws CertificateException {
		// given
		X509Certificate x509 = createX509Certificate(CERTIFICATE_OPENSHIFT_REDHAT_COM);
		// when
		CertificateState allowed = preference.isAllowed(x509);
		// then
		assertThat(allowed).isSameAs(CertificateState.NOT_PRESENT);
	}

	private class TestableSSLCertificatesPreference extends SSLCertificatesPreference {

		private IPreferenceStore store = new PreferenceStoreFake();

		@Override
		protected IPreferenceStore getPreferenceStore() {
			return store;
		}

		private class PreferenceStoreFake implements IPreferenceStore {

			private Map<String, String> store = new HashMap<>();
			
			@Override
			public void addPropertyChangeListener(IPropertyChangeListener listener) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean contains(String name) {
				return store.containsKey(name);
			}

			@Override
			public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
			}

			@Override
			public boolean getBoolean(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean getDefaultBoolean(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public double getDefaultDouble(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public float getDefaultFloat(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int getDefaultInt(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public long getDefaultLong(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getDefaultString(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public double getDouble(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public float getFloat(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int getInt(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public long getLong(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getString(String name) {
				return store.get(name);
			}

			@Override
			public boolean isDefault(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean needsSaving() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void putValue(String name, String value) {
				store.put(name, value);
			}

			@Override
			public void removePropertyChangeListener(IPropertyChangeListener listener) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, double value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, float value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, int value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, long value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, String defaultObject) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDefault(String name, boolean value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setToDefault(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setValue(String name, double value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setValue(String name, float value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setValue(String name, int value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setValue(String name, long value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setValue(String name, String value) {
				putValue(name, value);
			}

			@Override
			public void setValue(String name, boolean value) {
				throw new UnsupportedOperationException();
			}
		}

	}
}
