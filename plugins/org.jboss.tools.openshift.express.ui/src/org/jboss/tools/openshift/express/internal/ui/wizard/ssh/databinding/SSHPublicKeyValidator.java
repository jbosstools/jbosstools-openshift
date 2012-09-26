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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh.databinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUserConfig;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.AddSSHKeyWizardPageModel;

import com.openshift.client.OpenShiftException;
import com.openshift.client.SSHPublicKey;

/**
 * @author Andre Dietisheim
 */
public class SSHPublicKeyValidator extends MultiValidator {

	private IObservableValue filePathObservable;
	private AddSSHKeyWizardPageModel model;

	public SSHPublicKeyValidator(IObservableValue filePathObservable, AddSSHKeyWizardPageModel model) {
		this.filePathObservable = filePathObservable;
		this.model = model;
	}

	@Override
	protected IStatus validate() {
		String filePath = (String) filePathObservable.getValue();
		if (StringUtils.isEmpty(filePath)) {
			return ValidationStatus.cancel("You have to supply a public SSH key.");
		}
		try {
			SSHPublicKey sshPublicKey = new SSHPublicKey(filePath);
			if (model.hasPublicKey(sshPublicKey.getPublicKey())) {
				return ValidationStatus.error("The public key in " + filePath + " is already in use on OpenShift. Choose another key.");
			}
		} catch (FileNotFoundException e) {
			return ValidationStatus.error("Could not load file: " + e.getMessage());
		} catch (OpenShiftException e) {
			return ValidationStatus.error(filePath + " is not a valid public SSH key: " + e.getMessage());
		} catch (IOException e) {
			return ValidationStatus.error("Could not load file: " + e.getMessage());
		}

		if (hasSSHConfigurationIdentityKey()) {
			return ValidationStatus.warning(
					NLS.bind("Your SSH config ({0}) contains fixed keys for OpenShift servers. " +
							"This can override any Eclipse specific SSH key preferences.", new SSHUserConfig(SSHUtils.getSSH2Home()).getFile()));
		} else if (!SSHUtils.publicKeyMatchesPrivateKeyInPreferences(new File(filePath))) {
			return ValidationStatus.warning(
					NLS.bind("Could not find the private key for your public key in the preferences. "
							+ "Make sure it is listed in the ssh2 preferences.", filePath));
		}
			
		
		return ValidationStatus.ok();
	}

	public boolean hasSSHConfigurationIdentityKey() {
		try {
			SSHUserConfig sshUserConfig = new SSHUserConfig(SSHUtils.getSSH2Home());
			return sshUserConfig.hasLibraIdentifyFile();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Workaround since JSchUIPlugin seems not to fire property change events if
	 * you change the private keys. Need to force revalidation manually.
	 */
	public void forceRevalidate() {
		revalidate();
	}

}
