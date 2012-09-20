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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.utils.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUserConfig;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;

import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.OpenShiftException;
import com.openshift.client.SSHKeyPair;
import com.openshift.client.SSHPublicKey;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardPageModel extends ObservableUIPojo {

	public static final String ID_RSA_FILENAME = "id_rsa";
	public static final String LIBRA_ID_RSA_FILENAME = "libra_id_rsa";
	private static final String PUBLIC_KEY_SUFFIX = ".pub";
	private static final String KEYS_SEPARATOR = ",";

	private static final String SSHKEY_DEFAULT_NAME = "jbosstools"; //$NON-NLS-1$

	public static final String PROPERTY_DOMAIN_ID = "domainId";
	public static final String PROPERTY_SSHKEY = "sshKey";

	private String domainId;
	private IDomain domain;
	private String sshKey;
	private UserDelegate user;

	public NewDomainWizardPageModel(UserDelegate user) {
		this.user = user;
	}

	public void initSshKey() throws OpenShiftException {
		if (!publicKeyExists()) {
			return;
		}
		File libraPublicKey = getPublicKey();
		setSshKey(libraPublicKey.getAbsolutePath());
	}

	
	/**
	 * Returns the file of the libra public key. It is not checking if the file exists.
	 *  
	 * @return the libra public key 
	 * @throws OpenShiftException 
	 */
	public File getPublicKey() throws OpenShiftException {
		File libraPrivateKey = getPrivateKey();
		return new File(libraPrivateKey.getParent(), getPublicKeyPath(libraPrivateKey.getName()));
	}

	private String getPublicKeyPath(String privateKeyPath) {
		return privateKeyPath + PUBLIC_KEY_SUFFIX;
	}

	public File getPrivateKey() throws OpenShiftException {
		String ssh2Home = checkedGetSSH2Home();
		ensureSSHHomeExists(ssh2Home);
		if (hasConfiguredFixedPrivateKeys()) {
			return new File(ssh2Home, LIBRA_ID_RSA_FILENAME);
		} else {
			return new File(ssh2Home, ID_RSA_FILENAME);
		}
	}

	private String checkedGetSSH2Home() throws OpenShiftException {
		String ssh2Home = SSHUtils.getSSH2Home();
		if (ssh2Home == null 
				|| ssh2Home.trim().length() == 0) {
			throw new OpenShiftException("Could not determine your ssh2 home directory");
		}
		return ssh2Home;
	}
	
	public boolean hasConfiguredFixedPrivateKeys() {
		try {
			SSHUserConfig sshUserConfig = new SSHUserConfig(checkedGetSSH2Home());
			return sshUserConfig.hasLibraIdentifyFile();
		} catch (OpenShiftException e) {
			return false;
		}
	}


	public String getDomainId() {
		return this.domainId;
	}

	public void createDomain() throws OpenShiftException, IOException {
		user.createDomain(domainId);
		ISSHPublicKey sshKey = loadSshKey();
		IOpenShiftSSHKey sshKeyResource = user.getSSHKeyByPublicKey(sshKey.getPublicKey());
		if (sshKeyResource == null) {
			user.putSSHKey(getTimestampKeyname(), sshKey);
		}
	}

	private String getTimestampKeyname() {
		return new StringBuilder(
				SSHKEY_DEFAULT_NAME)
				.append(new SimpleDateFormat("yyyyMMddhmS").format(new Date()))
				.toString();
	}

	public String getSshKey() {
		return sshKey;
	}
	
	public boolean publicKeyExists() throws OpenShiftException {
		return FileUtils.canRead(getPublicKey());
	}

	public void createLibraKeyPair(String passPhrase) throws FileNotFoundException, OpenShiftException {
		File libraPublicKey = getPublicKey();
		if (libraPublicKey.canRead()) {
			// key already exists
			return;
		}
		ensureSSHHomeExists(checkedGetSSH2Home());
		File libraPrivateKey = getPrivateKey();
		SSHKeyPair keyPair = SSHKeyPair.create(passPhrase, libraPrivateKey.getAbsolutePath(), libraPublicKey.getAbsolutePath());
		setFilePermissions(libraPrivateKey);
		addToPrivateKeysPreferences(keyPair);
		setSshKey(keyPair.getPublicKeyPath());
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

	private void setFilePermissions(File file) {
		// set f permission to correspond to 'chmod 0600' read/write only for user
		// First clear all permissions for both user and others
		file.setReadable(false, false);
		file.setWritable(false, false);
		// Enable only readable for user
		file.setReadable(true, true); 
		file.setWritable(true, true);
	}
	
	private void addToPrivateKeysPreferences(SSHKeyPair keyPair) {
		Preferences preferences = JSchCorePlugin.getPlugin().getPluginPreferences();
		String privateKeys = preferences.getString(IConstants.KEY_PRIVATEKEY);
		if (privateKeys != null 
				&& privateKeys.trim().length() > 0) {
			privateKeys = privateKeys + ","	+ keyPair.getPrivateKeyPath();
		} else {
			privateKeys = keyPair.getPrivateKeyPath();
		}
		preferences.setValue(IConstants.KEY_PRIVATEKEY, privateKeys);
	    JSchCorePlugin.getPlugin().setNeedToLoadKeys(true);
	    JSchCorePlugin.getPlugin().savePluginPreferences();
	}
		
	public void setSshKey(String sshKey) {
		firePropertyChange(PROPERTY_SSHKEY, this.sshKey, this.sshKey = sshKey);
	}

	private ISSHPublicKey loadSshKey() throws IOException, OpenShiftException {
		return new SSHPublicKey(new File(sshKey));
	}

	public void setDomainId(String domainId) {
		firePropertyChange(PROPERTY_DOMAIN_ID, this.domainId, this.domainId = domainId);
	}

	public boolean hasDomain() {
		return domain != null;
	}

	public IDomain getDomain() {
		return domain;
	}

}
