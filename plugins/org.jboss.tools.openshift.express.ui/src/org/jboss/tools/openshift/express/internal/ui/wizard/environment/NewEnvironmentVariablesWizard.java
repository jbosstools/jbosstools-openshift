/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.openshift.client.IDomain;

/**
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public class NewEnvironmentVariablesWizard extends AbstractEnvironmentVariablesWizard<NewEnvironmentVariablesWizardModel> {

	private Map<String, String> environmentVarriableValueByKey;

	public NewEnvironmentVariablesWizard(Map<String, String> environmentVariables, IDomain domain) {
		super("Create Environment Variable(s)",
				new NewEnvironmentVariablesWizardModel(environmentVariables, domain));
	}

	@Override
	public boolean performFinish() {
		if (!isSupported()) {
			return true;
		}

		this.environmentVarriableValueByKey = (toMap(getModel().getVariables()));
		return true;
	}

	private Map<String, String> toMap(List<EnvironmentVariableItem> variables) {
		HashMap<String, String> environmentVariables = new LinkedHashMap<String, String>();
		for (EnvironmentVariableItem variable : variables) {
			environmentVariables.put(variable.getName(), variable.getValue());
		}
		return environmentVariables;
	}
	
	public Map<String, String> getEnvironmentVariables() {
		return environmentVarriableValueByKey;
	}
}
