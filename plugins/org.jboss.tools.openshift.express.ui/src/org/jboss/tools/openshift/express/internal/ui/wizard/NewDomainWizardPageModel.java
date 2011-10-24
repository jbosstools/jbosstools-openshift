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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;
import org.jboss.tools.openshift.express.client.IDomain;
import org.jboss.tools.openshift.express.client.ISSHPublicKey;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.client.SSHKeyPair;
import org.jboss.tools.openshift.express.client.SSHPublicKey;
import org.jboss.tools.openshift.express.internal.ui.common.FileUtils;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardPageModel extends ObservableUIPojo {

	private static final String OPENSHIFT_KEY_PREFIX = "openshift_id_rsa_";
	private static final String PUBLIC_KEY_SUFFIX = ".pub";

	public static final String PROPERTY_NAMESPACE = "namespace";
	public static final String PROPERTY_SSHKEY = "sshKey";
	public static final String PROPERTY_DOMAIN = "domain";

	private String namespace;
	private IDomain domain;
	private String sshKey;
	private IUser user;

	public NewDomainWizardPageModel(String namespace, IUser user) {
		this.namespace = namespace;
		this.user = user;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void createDomain() throws OpenShiftException, IOException {
		IDomain domain = user.createDomain(namespace, loadSshKey());
		setDomain(domain);
	}

	public String getSshKey() {
		return sshKey;
	}

	public void createSShKeyPair(String passPhrase) throws FileNotFoundException, OpenShiftException {
		String sshKeysDirectory = SshPrivateKeysPreferences.getSshKeyDirectory();
		SSHKeyPair keyPair = createSshKeyPair(passPhrase, sshKeysDirectory);
		SshPrivateKeysPreferences.add(keyPair.getPrivateKeyPath());
		setSshKey(keyPair.getPublicKeyPath());
	}
	
	private SSHKeyPair createSshKeyPair(String passPhrase, String sshKeysDirectory) throws OpenShiftException {
		String privateKeyPath = getKeyPairFileName(sshKeysDirectory);
		String publicKeyPath = getPublicKeyPath(privateKeyPath);
		return SSHKeyPair.create(passPhrase, privateKeyPath, publicKeyPath);
	}
	
	private String getKeyPairFileName(String sshKeysDirectory) {
		int i = 0;
		File privateKey = null;
		while (FileUtils.canRead(privateKey = new File(sshKeysDirectory, OPENSHIFT_KEY_PREFIX + i))
				|| FileUtils.canRead(new File(sshKeysDirectory, getPublicKeyPath(privateKey.getName())))) {
			i++;
		}
		return privateKey.getAbsolutePath();
	}

	private String getPublicKeyPath(String privateKeyPath) {
		return privateKeyPath + PUBLIC_KEY_SUFFIX;
	}

	public void setSshKey(String sshKey) {
		firePropertyChange(PROPERTY_SSHKEY, this.sshKey, this.sshKey = sshKey);
	}

	private ISSHPublicKey loadSshKey() throws IOException, OpenShiftException {
		return new SSHPublicKey(new File(sshKey));
	}

	public void setNamespace(String namespace) {
		firePropertyChange(PROPERTY_NAMESPACE, this.namespace, this.namespace = namespace);
	}

	public boolean hasDomain() {
		return domain != null;
	}

	public IDomain getDomain() {
		return domain;
	}

	public void setDomain(IDomain domain) {
		firePropertyChange(PROPERTY_DOMAIN, this.domain, this.domain = domain);
		if (domain != null) {
			setNamespace(domain.getNamespace());
		}
	}
}
