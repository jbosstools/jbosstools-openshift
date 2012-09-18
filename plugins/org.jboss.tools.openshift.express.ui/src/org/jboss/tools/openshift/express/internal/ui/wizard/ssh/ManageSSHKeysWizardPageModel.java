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

import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.IOpenShiftSSHKey;

/**
 * @author André Dietisheim
 */
public class ManageSSHKeysWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_KEY = "selectedSSHKey";

	private UserDelegate user;
	private IOpenShiftSSHKey selectedKey;

	public ManageSSHKeysWizardPageModel(UserDelegate user) {
		this.user = user;
	}

	public List<IOpenShiftSSHKey> loadSSHKeys() {
		return user.getSSHKeys();
	}

	public List<IOpenShiftSSHKey> getSSHKeys() {
		return user.getSSHKeys();
	}
	
	public IOpenShiftSSHKey getSelectedSSHKey() {
		return selectedKey;
	}
	
	public void setSelectedSSHKey(IOpenShiftSSHKey key) {
		firePropertyChange(PROPERTY_SELECTED_KEY, this.selectedKey, this.selectedKey = key);
	}

	public void removeKey() {
		if (selectedKey == null) {
			return;
		}
		selectedKey.destroy();
	}

	public void refresh() {
		user.refresh();
	}
	
	public UserDelegate getUser() {
		return user;
	}


}
