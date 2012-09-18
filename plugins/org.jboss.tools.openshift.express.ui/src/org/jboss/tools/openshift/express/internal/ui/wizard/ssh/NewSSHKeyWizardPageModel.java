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

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.SSHKeyType;

/**
 * @author Andre Dietisheim
 */
public class NewSSHKeyWizardPageModel extends AddSSHKeyWizardPageModel {

	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_SSH2_HOME = "SSH22Home";
	public static final String PROPERTY_PRIVATEKEY_PATH = "privateKeyPath";
	public static final String PROPERTY_PRIVATEKEY_PASSPHRASE = "privateKeyPassphrase";

	private SSHKeyType type;
	private String ssh2Home;
	private String privateKeyPath;
	private String privateKeyPathphrase;

	public NewSSHKeyWizardPageModel(UserDelegate user) {
		super(user);
	}

	public SSHKeyType getType() {
		return type;
	}

	public void setType(SSHKeyType type) {
		firePropertyChange(PROPERTY_TYPE, this.type, this.type = type);
	}

	public String getPrivateKeyPathphrase() {
		return privateKeyPathphrase;
	}

	public void setPrivateKeyPathphrase(String privateKeyPathphrase) {
		firePropertyChange(PROPERTY_PRIVATEKEY_PASSPHRASE,
				this.privateKeyPathphrase, this.privateKeyPathphrase = privateKeyPathphrase);
	}

	public String getPrivateKeyPath() {
		return privateKeyPath;
	}

	public void setPrivateKeyPath(String privateKeyPath) {
		firePropertyChange(PROPERTY_PRIVATEKEY_PATH, this.privateKeyPath, this.privateKeyPath = privateKeyPath);
	}

	public String getSSH2Home() {
		return ssh2Home;
	}

	public void setSSH2Home(String ssh2Home) {
		firePropertyChange(PROPERTY_SSH2_HOME, this.ssh2Home, this.ssh2Home = ssh2Home);
	}

}
