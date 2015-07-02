/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IApplication;

/**
 * @author André Dietisheim
 */
public class SelectApplicationWizard extends AbstractOpenShiftWizard<OpenShiftApplicationWizardModel> {


	private SelectApplicationWizardPage selectApplicationPage;

	public SelectApplicationWizard(OpenShiftApplicationWizardModel wizardModel) {
		super("Select Existing Application", wizardModel);
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	@Override
	public void addPages() {
		addPage(this.selectApplicationPage = new SelectApplicationWizardPage(getModel(), this));
	}
	
	public IApplication getSelectedApplication() {
		return selectApplicationPage.getSelectedApplication();
	}
}
