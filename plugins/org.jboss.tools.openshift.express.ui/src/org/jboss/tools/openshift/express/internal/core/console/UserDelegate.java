/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.console;

import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.express.internal.core.console.IPasswordPrompter.PromptResult;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IDomain;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.ISSHPublicKey;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class UserDelegate implements IUser {
	private IUser delegate;
	private boolean rememberPassword;
	
	public UserDelegate(IUser user, boolean rememberPassword) {
		this.delegate = user;
		this.rememberPassword = rememberPassword;
	}
	
	public String getRhlogin() {
		return delegate.getRhlogin();
	}
	public String getPassword() {
		return delegate.getPassword();
	}
	
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	protected void checkForPassword() {
		if( delegate.getPassword() == null || "".equals(delegate.getPassword())) {
			try {
				Map<PromptResult, Object> passwordAndSaveValues = UserModel.promptForPassword(this);
				if(passwordAndSaveValues != null) {
					final String password = (String) passwordAndSaveValues.get(PromptResult.PASSWORD_VALUE);
					delegate = UserModel.getDefault().createUser(delegate.getRhlogin(), password);
					final Boolean save = (Boolean) passwordAndSaveValues.get(PromptResult.SAVE_PASSWORD_VALUE);
					this.rememberPassword = save; 
				}
			} catch( Exception e ) {
				Logger.error("Failed to retrieve User's password", e);
			}
		}
	}
	
	public IApplication createApplication(String arg0, ICartridge arg1)
			throws OpenShiftException {
		checkForPassword();
		return delegate.createApplication(arg0, arg1);
	}

	public IDomain createDomain(String arg0, ISSHPublicKey arg1)
			throws OpenShiftException {
		checkForPassword();
		return delegate.createDomain(arg0, arg1);
	}
	public IApplication getApplicationByName(String arg0)
			throws OpenShiftException {
		checkForPassword();
		return delegate.getApplicationByName(arg0);
	}
	public List<IApplication> getApplications() throws OpenShiftException {
		checkForPassword();
		return delegate.getApplications();
	}
	public List<IApplication> getApplicationsByCartridge(ICartridge arg0)
			throws OpenShiftException {
		checkForPassword();
		return delegate.getApplicationsByCartridge(arg0);
	}
	public String getAuthIV() {
		checkForPassword();
		return delegate.getAuthIV();
	}
	public String getAuthKey() {
		checkForPassword();
		return delegate.getAuthKey();
	}
	public ICartridge getCartridgeByName(String arg0) throws OpenShiftException {
		checkForPassword();
		return delegate.getCartridgeByName(arg0);
	}
	public List<ICartridge> getCartridges() throws OpenShiftException {
		checkForPassword();
		return delegate.getCartridges();
	}
	public IDomain getDomain() throws OpenShiftException {
		checkForPassword();
		return delegate.getDomain();
	}
	public List<IEmbeddableCartridge> getEmbeddableCartridges()
			throws OpenShiftException {
		checkForPassword();
		return delegate.getEmbeddableCartridges();
	}
	public ISSHPublicKey getSshKey() throws OpenShiftException {
		checkForPassword();
		return delegate.getSshKey();
	}
	public String getUUID() throws OpenShiftException {
		checkForPassword();
		return delegate.getUUID();
	}
	public boolean hasApplication(String arg0) throws OpenShiftException {
		checkForPassword();
		return delegate.hasApplication(arg0);
	}
	public boolean hasApplication(ICartridge arg0) throws OpenShiftException {
		checkForPassword();
		return delegate.hasApplication(arg0);
	}
	public boolean hasDomain() throws OpenShiftException {
		checkForPassword();
		return delegate.hasDomain();
	}
	public boolean isValid() throws OpenShiftException {
		checkForPassword();
		return delegate.isValid();
	}
	public void refresh() throws OpenShiftException {
		checkForPassword();
		delegate.refresh();
	}
}
