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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.core.util.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHSessionRepository;

import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.internal.client.ApplicationSSHSession;
import com.openshift.internal.client.utils.StreamUtils;

/**
 * @author Andre Dietisheim
 */
public class SaveSnapshotWizardModel extends ObservablePojo {

	private String filepath;
	private boolean deploymentSnapshot;
	private IApplication application;

	public SaveSnapshotWizardModel(IApplication application) {
		this.application = application;
		this.filepath = FileUtils.getAvailableFilepath(getSnapshotFromPreferences(application, deploymentSnapshot));
	}

	public IApplication getApplication() {
		return application;
	}

	public String setFilepath(String filename) {
		return this.filepath = filename;
	}

	public String getFilepath() {
		return filepath;
	}

	public boolean setDeploymentSnapshot(boolean deploymentSnapshot) {
		return this.deploymentSnapshot = deploymentSnapshot;
	}

	public boolean isDeploymentSnapshot() {
		return deploymentSnapshot;
	}

	public void saveSnapshot(IProgressMonitor monitor) throws IOException {
		if (monitor.isCanceled()) {
			return;
		}
		Session session = SSHSessionRepository.getInstance().getSession(application);
		if (isDeploymentSnapshot()) {
			InputStream saveResponse = new ApplicationSSHSession(application, session).saveDeploymentSnapshot();
			StreamUtils.writeTo(saveResponse, new FileOutputStream(getFilepath()));
		} else {
			InputStream saveResponse = new ApplicationSSHSession(application, session).saveFullSnapshot();
			StreamUtils.writeTo(saveResponse, new FileOutputStream(getFilepath()));
		}
		storeSnapshotToPreferences(filepath, deploymentSnapshot);
	}

	private void storeSnapshotToPreferences(String filepath, boolean deploymentSnapshot) {
		if (deploymentSnapshot) {
			OpenShiftPreferences.INSTANCE.saveDeploymentSnapshot(getApplication(), filepath);
		} else {
			OpenShiftPreferences.INSTANCE.saveFullSnapshot(getApplication(), filepath);
		}
	}
	
	private String getSnapshotFromPreferences(IApplication application, boolean deploymentSnapshot) {
		if (deploymentSnapshot) {
			return OpenShiftPreferences.INSTANCE.getDeploymentSnapshot(application);
		} else {
			return OpenShiftPreferences.INSTANCE.getFullSnapshot(application);
		}
	}
}