/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftPasswordStorageKey;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStore;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStoreException;

import com.openshift.express.client.IUser;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class CredentialsWizardPageModel extends ObservableUIPojo {

	private static final String RHLOGIN_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN";

	public static final String PROPERTY_SERVER_URL = "serverUrl";
	public static final String PROPERTY_RHLOGIN = "rhLogin";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_CREDENTIALS_STATUS = "credentialsStatus";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";

	private String rhLogin;
	private String password;
	private boolean rememberPassword;
	private IStatus credentialsStatus;
	private StringPreferenceValue rhLoginPreferenceValue;
	private final String libraServer;

	private SecurePasswordStore store;

	public CredentialsWizardPageModel() {
		this.rhLoginPreferenceValue = new StringPreferenceValue(RHLOGIN_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		this.libraServer = initLibraServer();
		this.rhLogin = initRhLogin();
		initSecureStore(libraServer, rhLogin);
		initPassword();
		resetCredentialsStatus();
	}

	private String initLibraServer() {
		try {
			return new OpenShiftConfiguration().getLibraServer();
		} catch (Exception e) {
			Logger.error("Failed to load OpenShift configuration from client library", e);
		}
		return null;
	}

	private void initSecureStore(final String platform, final String username) {
		final OpenShiftPasswordStorageKey key = new OpenShiftPasswordStorageKey(platform, username);
		if (key != null) {
			this.store = new SecurePasswordStore(key);
		}
	}

	protected String initRhLogin() {
		String rhLogin = null;
		rhLogin = rhLoginPreferenceValue.get();
		if (rhLogin == null || rhLogin.length() == 0) {
			rhLogin = getUserConfiguration();
		}
		return rhLogin;
	}

	protected void initPassword() {
		if (libraServer != null && rhLogin != null && !rhLogin.isEmpty() && store != null) {
			try {
				this.password = store.getPassword();
				setRememberPassword(this.password != null && !this.password.isEmpty());
			} catch (SecurePasswordStoreException e) {
				Logger.error("Failed to retrieve OpenShift user's password from Secured Store", e);
			}
		}
	}

	private void storePassword(String password) {
		try {
			if (store != null) {
				store.setPassword(password);
			}
		} catch (SecurePasswordStoreException e) {
			Logger.error(e.getMessage(), e);
		}
	}
	
	private void erasePasswordStore() {
		try {
			if (store != null) {
				store.remove();	
			}
		} catch (SecurePasswordStoreException e) {
			Logger.error(e.getMessage(), e);
		}
	}

	protected String getUserConfiguration() {
		String configuredUsername = null;
		try {
			configuredUsername = new OpenShiftConfiguration().getRhlogin();
		} catch (Exception e) {
			// do nothing
		}
		return configuredUsername;
	}

	public String getRhLogin() {
		return rhLogin;
	}

	public void setRhLogin(String rhLogin) {
		if (rhLogin != null && !rhLogin.equals(this.rhLogin)) {
			rhLoginPreferenceValue.store(rhLogin);
			firePropertyChange(PROPERTY_RHLOGIN, this.rhLogin, this.rhLogin = rhLogin);
			resetCredentialsStatus();
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password != null && !password.equals(this.password)) {
			firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
			resetCredentialsStatus();
		}
	}

	/**
	 * @return the rememberPassword
	 */
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	/**
	 * @param rememberPassword
	 *            the rememberPassword to set
	 */
	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD, this.rememberPassword, this.rememberPassword = rememberPassword);
	}

	private void resetCredentialsStatus() {
		setCredentialsStatus(null);
	}

	private void setCredentialsStatus(IStatus status) {
		firePropertyChange(PROPERTY_CREDENTIALS_STATUS, this.credentialsStatus, this.credentialsStatus = status);
	}

	public IStatus getCredentialsStatus() {
		return credentialsStatus;
	}

	public boolean areCredentialsValid() {
		IStatus validationStatus = getCredentialsStatus();
		return validationStatus != null && validationStatus.isOK();
	}

	public boolean areCredentialsValidated() {
		return credentialsStatus != null;// && credentialsValidity.isOK();
	}

	public IStatus validateCredentials() {
		IStatus status = Status.OK_STATUS;
		try {
			// reset without notifying
			// this.credentialsValidity = null;
			IUser user = OpenShiftUIActivator.getDefault().createUser(getRhLogin(), getPassword());
			if (user.isValid() && rememberPassword) {
				storePassword(password);
			} else if(user.isValid() && !rememberPassword) {
				erasePasswordStore();
			}
		} catch (NotFoundOpenShiftException e) {
			// valid user without domain
		} catch (OpenShiftException e) {
			status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Your credentails are not valid.");
		} catch (Exception e) {
			status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind(
					"Could not check user credentials: {0}.", e.getMessage()));
		}

		setCredentialsStatus(status);
		return status;
	}

}
