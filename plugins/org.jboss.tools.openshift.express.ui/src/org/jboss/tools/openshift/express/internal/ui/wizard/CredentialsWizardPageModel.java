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
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IUser;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.User;
import com.openshift.express.client.configuration.DefaultConfiguration;
import com.openshift.express.client.configuration.SystemConfiguration;
import com.openshift.express.client.configuration.SystemProperties;
import com.openshift.express.client.configuration.UserConfiguration;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class CredentialsWizardPageModel extends ObservableUIPojo {

	private static final String CLIENT_ID = OpenShiftUIActivator.PLUGIN_ID
			+ " " + OpenShiftUIActivator.getDefault().getBundle().getVersion();

	private static final String RHLOGIN_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN";

	public static final String PROPERTY_SERVER_URL = "serverUrl";
	public static final String PROPERTY_RHLOGIN = "rhLogin";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_CREDENTIALS_STATUS = "credentialsStatus";

	private String rhLogin;
	private String password;
	private IStatus credentialsStatus;
	private IUser user;
	private StringPreferenceValue rhLoginPreferenceValue;

	private AbstractOpenShiftApplicationWizardModel wizardModel;

	public CredentialsWizardPageModel(AbstractOpenShiftApplicationWizardModel model) {
		this.wizardModel = model;
		this.rhLoginPreferenceValue = new StringPreferenceValue(RHLOGIN_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		this.rhLogin = initRhLogin();
		resetCredentialsStatus();
	}

	protected String initRhLogin() {
		String rhLogin = null;
		rhLogin = rhLoginPreferenceValue.get();
		if (rhLogin == null
				|| rhLogin.length() == 0) {
			rhLogin = getUserConfiguration();
		}
		return rhLogin;
	}

	protected String getUserConfiguration() {
		String configuredUsername = null;
		try {
			configuredUsername =
					new SystemProperties(
							new UserConfiguration(
									new SystemConfiguration(
											new DefaultConfiguration()))).getRhlogin();
		} catch (Exception e) {
			// do nothing
		}
		return configuredUsername;
	}

	public String getRhLogin() {
		return rhLogin;
	}

	public void setRhLogin(String rhLogin) {
		if (rhLogin != null
				&& !rhLogin.equals(this.rhLogin)) {
			rhLoginPreferenceValue.store(rhLogin);
			firePropertyChange(PROPERTY_RHLOGIN, this.rhLogin, this.rhLogin = rhLogin);
			resetCredentialsStatus();
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password != null
				&& !password.equals(this.password)) {
			firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
			resetCredentialsStatus();
		}
	}

	private void resetCredentialsStatus() {
		setCredentialsStatus(null);
	}

	private void setCredentialsStatus(IStatus status) {
		firePropertyChange(PROPERTY_CREDENTIALS_STATUS, this.credentialsStatus,
				this.credentialsStatus = status);
	}

	public IStatus getCredentialsStatus() {
		return credentialsStatus;
	}

	public boolean areCredentialsValid() {
		IStatus validationStatus = getCredentialsStatus();
		return validationStatus != null
				&& validationStatus.isOK();
	}

	public boolean areCredentialsValidated() {
		return credentialsStatus != null;// && credentialsValidity.isOK();
	}

	public IStatus validateCredentials() {
		IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Your credentails are not valid.");
		try {
			// reset without notifying
			//this.credentialsValidity = null;
			this.user = new User(getRhLogin(), getPassword(), CLIENT_ID);
			if (user.isValid()) {
				status = Status.OK_STATUS;
			}
		} catch (NotFoundOpenShiftException e) {
			// valid user without domain
			status = Status.OK_STATUS;
		} catch (OpenShiftException e) {
			this.user = null;
		} catch (Exception e) {
			this.user = null;
		}
		wizardModel.setUser(user);
		setCredentialsStatus(status);
		return status;
	}

	public IUser getUser() {
		return user;
	}
}
