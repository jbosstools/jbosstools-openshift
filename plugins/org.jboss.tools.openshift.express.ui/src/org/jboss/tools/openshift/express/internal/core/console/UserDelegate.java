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

import java.net.SocketTimeoutException;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectToOpenShiftWizard;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftUnknonwSSHKeyTypeException;

public class UserDelegate {
	private IUser delegate;
	private boolean rememberPassword;
	private boolean connected;
	private boolean alreadyPromptedForPassword;
	
	public UserDelegate(IUser user, boolean rememberPassword, boolean connected) {
		this.delegate = user;
		this.rememberPassword = rememberPassword;
		this.setConnected(connected);
	}
	
	/**
	 * @return the delegate
	 */
	protected final IUser getDelegate() {
		return delegate;
	}

	/**
	 * @param delegate the delegate to set
	 */
	protected final void setDelegate(IUser delegate) {
		this.delegate = delegate;
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

	/**
	 * @param rememberPassword the rememberPassword to set
	 */
	protected final void setRememberPassword(boolean rememberPassword) {
		this.rememberPassword = rememberPassword;
	}

	public boolean canPromptForPassword() {
		return this.alreadyPromptedForPassword == false;
	}
	
	/**
	 * Prompts user for password if it was not given or retrieved from secure storage before.
	 * @return true if user entered credentials, false otherwise.
	 */
	public boolean checkForPassword() {
		if( delegate.getPassword() == null || "".equals(delegate.getPassword())) {
			try {
				this.alreadyPromptedForPassword = true;
				Display.getDefault().syncExec(new Runnable() { public void run() {
					final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					final Shell shell = activeWorkbenchWindow != null ? activeWorkbenchWindow.getShell() : null;
					if(shell == null) {
						Logger.error("Could not open Credentials Wizard: no shell available");
						return;
					}
					final IWizard connectToOpenShiftWizard = new ConnectToOpenShiftWizard();
					int returnCode = WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
					if (returnCode == Window.OK) {
						Logger.debug("OpenShift Auth succeeded.");
						setDelegate(UserModel.getDefault().getRecentUser().getDelegate());
						setConnected(true);
						setRememberPassword(UserModel.getDefault().getRecentUser().isRememberPassword());
					} else {
						setConnected(false);
					}
				}});
			} catch( Exception e ) {
				Logger.error("Failed to retrieve User's password", e);
			}
		}
		return (delegate.getPassword() != null  && !"".equals(delegate.getPassword()));
	}
	
	public IApplication createApplication(final String applicationName, final ICartridge applicationType, final ApplicationScale scale, final IGearProfile gearProfile)
			throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getDefaultDomain().createApplication(applicationName, applicationType, scale, gearProfile);
		}
		return null;
	}

	/**
	 * Create a new domain with the given id
	 * @param id the domain id
	 * @return the created domain
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException 
	 */
	public IDomain createDomain(String id)
			throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.createDomain(id);
		} 
		return null;
	}
	public IApplication getApplicationByName(String arg0)
			throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword() && delegate.hasDomain()) {
			return delegate.getDefaultDomain().getApplicationByName(arg0);
		} 
		return null;
	}
	public List<IApplication> getApplications() throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getDefaultDomain().getApplications();
		} 
		return null;
	}
	
	public String getAuthIV() {
		if(checkForPassword()) {
			return delegate.getAuthIV();
		} 
		return null;
	}
	
	public String getAuthKey() {
		if(checkForPassword()) {
			return delegate.getAuthKey();
		} 
		return null;
	}
	
	public List<ICartridge> getStandaloneCartridgeNames() throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getConnection().getStandaloneCartridges();
		} 
		return null;
	}
	
	public IDomain getDefaultDomain() throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getDefaultDomain();
		} 
		return null;
	}
	
	public List<IEmbeddableCartridge> getEmbeddableCartridges()
			throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getConnection().getEmbeddableCartridges();
		} 
		return null;
	}
	
	public boolean hasApplication(String name) throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.getDefaultDomain().hasApplicationByName(name);
		} 
		return false;
	}
	
	public boolean hasApplicationOfType(ICartridge type) throws OpenShiftException, SocketTimeoutException {
		if(hasDomain()) {
			return delegate.getDefaultDomain().hasApplicationByCartridge(type);
		} 
		return false;
	}
	
	public boolean hasDomain() throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			return delegate.hasDomain();
		}
		return false;
	}
	
	public boolean isValid() throws OpenShiftException {
		if(checkForPassword()) {
			return true;//delegate.isValid();
		} 
		return false;
	}
	
	public void refresh() throws OpenShiftException, SocketTimeoutException {
		if(checkForPassword()) {
			delegate.refresh();
		} 
	}

	/**
	 * @return the connected
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * @param connected the connected to set
	 */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey) throws SocketTimeoutException, OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
		return delegate.getSSHKeyByPublicKey(publicKey);
	}
	
	public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException, SocketTimeoutException {
		return delegate.putSSHKey(name, key);
	}
	
}
