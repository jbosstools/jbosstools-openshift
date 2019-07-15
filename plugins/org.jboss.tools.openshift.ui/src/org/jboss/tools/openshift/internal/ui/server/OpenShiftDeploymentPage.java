/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.swt.widgets.Composite;
import org.jboss.ide.eclipse.as.core.server.IServerWorkingCopyProvider;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentPage;
import org.jboss.ide.eclipse.as.ui.editor.ModuleDeploymentOptionsComposite;
import org.jboss.tools.as.core.internal.modules.DeploymentPreferencesLoader;

@SuppressWarnings("restriction")
public class OpenShiftDeploymentPage extends DeploymentPage implements IServerWorkingCopyProvider {

	@Override
	public void createPartControl(Composite parent) {
		this.preferences = DeploymentPreferencesLoader.loadPreferencesFromServer(getServer());

		new ModuleDeploymentOptionsComposite(parent, this, getFormToolkit(parent.getDisplay()), getPreferences()) {

			@Override
			protected boolean showTemporaryColumn() {
				return false;
			}

			@Override
			protected String getDefaultDeploymentTypeFilter() {
				return DEPLOYED;
			}
		};
	}
	
}