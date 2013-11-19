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

import java.util.Map;

import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

/**
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public abstract class AbstractEnvironmentVariablesWizard<MODEL extends AbstractEnvironmentVariablesWizardModel> extends AbstractOpenShiftWizard<MODEL> {

	private Map<String, String> environmentVarriableValueByKey;

	protected AbstractEnvironmentVariablesWizard(String title, MODEL wizardModel) {
		super(title, wizardModel);
	}

	@Override
	public void addPages() {
		addPage(new EnvironmentVariablesWizardPage(getModel(), this));
	}

	protected boolean isSupported() {
		return getModel().isSupported();
	}
	
	public Map<String, String> getEnvironmentVariables() {
		return environmentVarriableValueByKey;
	}
}
