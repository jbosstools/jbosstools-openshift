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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractSSHKeyWizardPageModel extends ObservableUIPojo implements ISSHKeyWizardPageModel {

	private String name;
	private Connection user;
	
	public AbstractSSHKeyWizardPageModel(Connection user) {
		this.user = user;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	@Override
	public boolean hasKeyName(String name) {
		return user.hasSSHKeyName(name);
	}

	@Override
	public boolean hasPublicKey(String publicKeyContent) {
		return user.hasSSHPublicKey(publicKeyContent);
	}
	
	protected Connection getUser() {
		return user;
	}

	protected String checkedGetSSH2Home() throws OpenShiftException {
		String ssh2Home = SSHUtils.getSSH2Home();
		if (ssh2Home == null 
				|| ssh2Home.trim().length() == 0) {
			throw new OpenShiftException("Could not determine your ssh2 home directory");
		}
		return ssh2Home;
	}
}
