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

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectToOpenShiftWizard;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IDomain;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.ISSHPublicKey;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

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
	
	public IApplication createApplication(String arg0, ICartridge arg1)
			throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.createApplication(arg0, arg1);
		}
		return null;
	}

	public IDomain createDomain(String arg0, ISSHPublicKey arg1)
			throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.createDomain(arg0, arg1);
		} 
		return null;
	}
	public IApplication getApplicationByName(String arg0)
			throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getApplicationByName(arg0);
		} 
		return null;
	}
	public List<IApplication> getApplications() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getApplications();
		} 
		return null;
	}
	public List<IApplication> getApplicationsByCartridge(ICartridge arg0)
			throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getApplicationsByCartridge(arg0);
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
	public ICartridge getCartridgeByName(String arg0) throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getCartridgeByName(arg0);
		} 
		return null;
	}
	public List<ICartridge> getCartridges() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getCartridges();
		} 
		return null;
	}
	public IDomain getDomain() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getDomain();
		} 
		return null;
	}
	public List<IEmbeddableCartridge> getEmbeddableCartridges()
			throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getEmbeddableCartridges();
		} 
		return null;
	}
	public ISSHPublicKey getSshKey() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getSshKey();
		} 
		return null;
	}
	public String getUUID() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.getUUID();
		} 
		return null;
	}
	public boolean hasApplication(String arg0) throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.hasApplication(arg0);
		} 
		return false;
	}
	public boolean hasApplication(ICartridge arg0) throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.hasApplication(arg0);
		} 
		return false;
	}
	public boolean hasDomain() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.hasDomain();
		}
		return false;
			
	}
	public boolean isValid() throws OpenShiftException {
		if(checkForPassword()) {
			return delegate.isValid();
		} 
		return false;
	}
	public void refresh() throws OpenShiftException {
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
}
