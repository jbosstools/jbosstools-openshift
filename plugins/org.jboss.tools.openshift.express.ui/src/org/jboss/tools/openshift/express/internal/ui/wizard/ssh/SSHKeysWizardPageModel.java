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
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class SSHKeysWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_KEY = "selectedSSHKey";
	public static final String PROPERTY_HAS_KEY = "hasSSHKey";

	private ExpressConnection connection;
	private IOpenShiftSSHKey selectedKey;

	public SSHKeysWizardPageModel(ExpressConnection connection) {
		this.connection = connection;
	}

	public List<IOpenShiftSSHKey> getSSHKeys() throws OpenShiftException {
		List<IOpenShiftSSHKey> keys = connection.getSSHKeys();
		fireHasSSHKey();
		return keys;
	}
	
	public IOpenShiftSSHKey getSelectedSSHKey() {
		return selectedKey;
	}
	
	public void setSelectedSSHKey(IOpenShiftSSHKey key) {
		firePropertyChange(PROPERTY_SELECTED_KEY, this.selectedKey, this.selectedKey = key);
	}

	public boolean getHasSSHKey() {
		return connection.hasSSHKeys();
	}
	
	protected void fireHasSSHKey() {
		firePropertyChange(PROPERTY_HAS_KEY, null, getHasSSHKey());
	}
	
	public void removeKey() throws OpenShiftException{
		if (selectedKey == null) {
			return;
		}
		selectedKey.destroy();
		selectedKey = null;
		restoreSelectedSSHKey();
	}

	public void refresh()  throws OpenShiftException {
		connection.refresh();
		restoreSelectedSSHKey();
	}

	private void restoreSelectedSSHKey() {
		IOpenShiftSSHKey keyToSelect = selectedKey;
		if (keyToSelect == null
				|| !connection.hasSSHKeyName(keyToSelect.getName())) {
			keyToSelect = getFirstKey();
		}
		setSelectedSSHKey(keyToSelect);
	}

	private IOpenShiftSSHKey getFirstKey() {
		if(getSSHKeys().size() == 0) {
			return null;
		} 
		return getSSHKeys().get(0);
	}

	
	public ExpressConnection getConnection() {
		return connection;
	}

}
