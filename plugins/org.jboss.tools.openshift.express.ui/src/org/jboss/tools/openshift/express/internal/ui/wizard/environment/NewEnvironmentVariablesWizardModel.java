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

import java.util.Map;

import com.openshift.client.IDomain;


/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public class NewEnvironmentVariablesWizardModel extends AbstractEnvironmentVariablesWizardModel {

	private Map<String, String> environmentVariables;
	private IDomain domain;
	
	public NewEnvironmentVariablesWizardModel(Map<String, String> environmentVariables, IDomain domain) {
		this.environmentVariables = environmentVariables;
		this.domain = domain;
	}

	public void refreshEnvironmentVariables() {
		loadEnvironmentVariables();
	}

	@Override
	public void loadEnvironmentVariables() {
		clear();
		if (environmentVariables == null
				|| environmentVariables.isEmpty()) {
			return;
		}

		add(environmentVariables);
	}

	private void add(Map<String, String> environmentVariables) {
		for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
			add(new EnvironmentVariableItem(entry.getKey(), entry.getValue()));
		}
	}

	@Override
	public boolean isSupported() {
		return domain != null
				&& domain.canCreateApplicationWithEnvironmentVariables();
	}
	
	public String getHost() {
		if (domain == null
				|| domain.getUser() == null) {
			return null;
		}
		return domain.getUser().getServer();
	}
}
