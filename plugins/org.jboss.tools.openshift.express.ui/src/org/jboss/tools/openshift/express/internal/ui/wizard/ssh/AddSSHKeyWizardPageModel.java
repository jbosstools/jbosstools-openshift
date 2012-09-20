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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUserConfig;

import com.openshift.client.OpenShiftException;
import com.openshift.client.SSHPublicKey;

/**
 * @author Andre Dietisheim
 */
public class AddSSHKeyWizardPageModel extends AbstractSSHKeyWizardPageModel {

	public static final String PROPERTY_PUBLICKEY_PATH = "publicKeyPath";
	
	private String keyPath;
	
	public AddSSHKeyWizardPageModel(UserDelegate user) {
		super(user);
	}

	public String getPublicKeyPath() {
		return keyPath;
	}

	public File getPublicKey() {
		return new File(keyPath);
	}

	public void setPublicKeyPath(String keyPath) {
		firePropertyChange(PROPERTY_PUBLICKEY_PATH, this.keyPath, this.keyPath = keyPath);
	}

	public boolean hasPublicKey(String publicKeyContent) {
		return getUser().hasSSHPublicKey(publicKeyContent);
	}	
	
	public void addSSHKey() throws FileNotFoundException, OpenShiftException, IOException {
		SSHPublicKey sshPublicKey = new SSHPublicKey(getPublicKey());
		getUser().putSSHKey(getName(), sshPublicKey);
	}
}
