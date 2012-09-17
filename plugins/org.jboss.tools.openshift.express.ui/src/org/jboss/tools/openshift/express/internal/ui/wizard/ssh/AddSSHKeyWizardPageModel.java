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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

/**
 * @author Andre Dietisheim
 */
public class AddSSHKeyWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_FILEPATH = "filePath";
	public static final String PROPERTY_NAME = "name";
	
	private String name;
	private String filePath;
	private UserDelegate user;
	
	public AddSSHKeyWizardPageModel(UserDelegate user) {
		this.user = user;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	public void setFilePath(String filePath) {
		firePropertyChange(PROPERTY_FILEPATH, this.filePath, this.filePath = filePath);
	}
	
	public void addConfiguredSSHKey() {
	}
	
}
