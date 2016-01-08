/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.List;

import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * @author Andre Dietisheim
 * 
 * @see WizardFragment
 */
public class OpenShiftServerWizardFragment extends WizardFragment {

	@Override
	protected void createChildFragments(List<WizardFragment> list) {
		ConnectionWizardFragment connectionWizardFragment = new ConnectionWizardFragment();
		ServerSettingsWizardFragment serverSettingsWizardFragment = new ServerSettingsWizardFragment();

		connectionWizardFragment.addConnectionChangeListener(serverSettingsWizardFragment.getConnectionChangeListener());

		list.add(connectionWizardFragment);
		list.add(serverSettingsWizardFragment);
	}
}
