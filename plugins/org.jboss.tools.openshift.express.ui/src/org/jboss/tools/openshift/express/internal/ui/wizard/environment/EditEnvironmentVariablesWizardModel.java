/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import com.openshift.client.IApplication;
import com.openshift.client.IEnvironmentVariable;


/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author Andre Dietisheim
 */
public class EditEnvironmentVariablesWizardModel extends AbstractEnvironmentVariablesWizardModel {

	private IApplication application;
	
	public EditEnvironmentVariablesWizardModel(IApplication application) {
		this.application = application;		
	}

	@Override
	public void loadEnvironmentVariables() {
		if (!isSupported()) {
			return;
		}
		
		super.loadEnvironmentVariables();

		for (IEnvironmentVariable variable : application.getEnvironmentVariables().values()) {
			add(new EnvironmentVariableItem(variable.getName(), variable.getValue()));
		}
	}

	public IApplication getApplication() {
		return application;
	}

	@Override
	public boolean isSupported() {
		return application != null
				&& application.canUpdateEnvironmentVariables()
				&& application.canGetEnvironmentVariables();
	}
	
	public String getHost() {
		if (application == null
				|| application.getDomain() == null
				|| application.getDomain().getUser() == null) {
			return "";
		}
		return application.getDomain().getUser().getServer();
	}
}
