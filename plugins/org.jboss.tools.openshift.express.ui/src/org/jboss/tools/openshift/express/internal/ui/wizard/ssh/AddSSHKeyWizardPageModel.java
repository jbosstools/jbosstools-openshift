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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.OpenShiftException;
import com.openshift.client.SSHPublicKey;

/**
 * @author Andre Dietisheim
 */
public class AddSSHKeyWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_PUBLICKEY_PATH = "publicKeyPath";
	public static final String PROPERTY_NAME = "name";
	
	private String name;
	private String filePath;
	private UserDelegate user;
	
	public AddSSHKeyWizardPageModel(UserDelegate user) {
		this.user = user;
	}

	public String getPublicKeyPath() {
		return filePath;
	}

	public void setPublicKeyPath(String filePath) {
		firePropertyChange(PROPERTY_PUBLICKEY_PATH, this.filePath, this.filePath = filePath);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	public boolean hasKeyName(String name) {
		return user.hasSSHKeyName(name);
	}

	public boolean hasPublicKey(String publicKeyContent) {
		return user.hasSSHPublicKey(publicKeyContent);
	}

	public void addConfiguredSSHKey() throws FileNotFoundException, OpenShiftException, IOException {
		SSHPublicKey sshPublicKey = new SSHPublicKey(new File(filePath));
		user.putSSHKey(name, sshPublicKey);
	}

	
}
