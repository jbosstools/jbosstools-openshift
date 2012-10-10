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

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.OpenShiftException;
import com.openshift.client.SSHKeyPair;
import com.openshift.client.SSHKeyType;

/**
 * @author Andre Dietisheim
 */
public class NewSSHKeyWizardPageModel extends AbstractSSHKeyWizardPageModel {

	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_SSH2_HOME = "SSH2Home";
	public static final String PROPERTY_PRIVATEKEY_FILENAME = "privateKeyName";
	public static final String PROPERTY_PRIVATEKEY_PASSPHRASE = "privateKeyPassphrase";
	public static final String PROPERTY_PUBLICKEY_FILENAME = "publicKeyName";
	private static final String PUBLICKEY_SUFFIX = ".pub";

	private SSHKeyType type = SSHKeyType.SSH_RSA;
	private String ssh2Home = SSHUtils.getSSH2Home();
	private String privateKeyName;
	private String privateKeyPathphrase;
	private String publicKeyName;

	public NewSSHKeyWizardPageModel(Connection user) {
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

	public String getPrivateKeyName() {
		return privateKeyName;
	}

	public void setPrivateKeyName(String privateKeyName) {
		firePropertyChange(PROPERTY_PRIVATEKEY_FILENAME, this.privateKeyName, this.privateKeyName = privateKeyName);
		updatePublicKeyNameFromPrivateKey(privateKeyName);
	}

	private void updatePublicKeyNameFromPrivateKey(String privateKeyName) {
		if (StringUtils.isEmpty(publicKeyName)) {
			setPublicKeyName(privateKeyName + PUBLICKEY_SUFFIX);
		} else {
			String publicKeyNameNoSuffix = StringUtils.getWithoutSuffix(publicKeyName, PUBLICKEY_SUFFIX);
			if (privateKeyName.startsWith(publicKeyNameNoSuffix)) {
				setPublicKeyName(privateKeyName + PUBLICKEY_SUFFIX);
			}
		}
	}

	public String getPublicKeyName() {
		return publicKeyName;
	}

	public void setPublicKeyName(String publicKeyName) {
		firePropertyChange(PROPERTY_PUBLICKEY_FILENAME, this.publicKeyName, this.publicKeyName = publicKeyName);
	}

	public String getSSH2Home() {
		return ssh2Home;
	}

	public void setSSH2Home(String ssh2Home) {
		firePropertyChange(PROPERTY_SSH2_HOME, this.ssh2Home, this.ssh2Home = ssh2Home);
	}

	public File getPublicKey() {
		return new File(ssh2Home, publicKeyName);
	}

	public void addSSHKey() throws FileNotFoundException, OpenShiftException, IOException {
		SSHKeyPair keyPair = createSSHKey();
		SSHUtils.addToPrivateKeysPreferences(new File(keyPair.getPrivateKeyPath()));
		getUser().putSSHKey(getName(), keyPair);
	}

	private SSHKeyPair createSSHKey() {
		ensureSSHHomeExists(ssh2Home);
		File privateKey = new File(ssh2Home, privateKeyName);
		File publicKey = new File(ssh2Home, publicKeyName);
		SSHKeyPair keyPair =
				SSHKeyPair.create(privateKeyPathphrase, privateKey.getAbsolutePath(), publicKey.getAbsolutePath());
		SSHUtils.setPrivateKeyPermissions(privateKey);
		return keyPair;
	}

	private void ensureSSHHomeExists(String ssh2Home)
			throws OpenShiftException {
		File ssh2HomeFile = new File(ssh2Home);
		if (FileUtils.canRead(ssh2HomeFile)) {
			if (!FileUtils.isDirectory(ssh2HomeFile)) {
				throw new OpenShiftException(
						ssh2Home + " is a file instead of a directory. This prevents creation and usage of ssh keys");
			}
			return;
		}

		try {
			if(!ssh2HomeFile.mkdirs()) {
				throw new OpenShiftException("Could not create ssh2 home directory at {0}", ssh2Home);
			}
		} catch(SecurityException e) {
			throw new OpenShiftException(e, "Could not create ssh2 home directory at {0}", ssh2Home);
		}
	}

	
}
