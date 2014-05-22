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
package org.jboss.tools.openshift.express.internal.ui.wizard.snapshot;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

/**
 * @author André Dietisheim
 */
public class RestoreSnapshotWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_FILEPATH = "filepath";
	public static final String PROPERTY_DEPLOYMENT_SNAPSHOT = "deploymentSnapshot";
	public static final String PROPERTY_HOT_DEPLOY = "hotDeploy";
	
	private RestoreSnapshotWizardModel wizardModel;

	public RestoreSnapshotWizardPageModel(RestoreSnapshotWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	public void setFilepath(String filepath) {
		firePropertyChange(
				PROPERTY_FILEPATH, this.wizardModel.getFilepath(), this.wizardModel.setFilepath(filepath));
	}

	public String getFilepath() {
		return wizardModel.getFilepath();
	}

	public void setDeploymentSnapshot(boolean deploymentSnapshot) {
		firePropertyChange(
				PROPERTY_DEPLOYMENT_SNAPSHOT, this.wizardModel.isDeploymentSnapshot(),
				this.wizardModel.setDeploymentSnapshot(deploymentSnapshot));
	}

	public boolean isDeploymentSnapshot() {
		return wizardModel.isDeploymentSnapshot();
	}

	public void setHotDeploy(boolean hotDeploy) {
		firePropertyChange(
				PROPERTY_HOT_DEPLOY, this.wizardModel.isHotDeploy(), this.wizardModel.setHotDeploy(hotDeploy));
	}

	public boolean isHotDeploy() {
		return wizardModel.isHotDeploy();
	}
}
