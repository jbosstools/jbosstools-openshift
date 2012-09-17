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

import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.IOpenShiftSSHKey;

/**
 * @author André Dietisheim
 */
public class ManageSSHKeysWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SSH_KEYS = "SSHKeys";

	private UserDelegate user;
	private List<IOpenShiftSSHKey> keys = new ArrayList<IOpenShiftSSHKey>();

	public ManageSSHKeysWizardPageModel(UserDelegate user) {
		this.user = user;
	}

	public List<IOpenShiftSSHKey> loadSSHKeys() {
		return setSSHKeys(user.getSSHKeys());
	}

	public List<IOpenShiftSSHKey> getSSHKeys() {
		return keys;
	}

	public List<IOpenShiftSSHKey> setSSHKeys(List<IOpenShiftSSHKey> keys) {
		this.keys = keys;
		firePropertyChange(PROPERTY_SSH_KEYS, this.keys, this.keys = keys);
		return this.keys;
	}

	public UserDelegate getUser() {
		return user;
	}

}
