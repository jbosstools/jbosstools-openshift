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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHSessionRepository;

import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.internal.client.ApplicationSSHSession;
import com.openshift.internal.client.utils.StreamUtils;

/**
 * @author Andre Dietisheim
 */
public class RestoreSnapshotWizardModel extends ObservablePojo {

	private String filepath;
	private boolean deploymentSnapshot;
	private boolean hotDeploy;
	private IApplication application;

	public RestoreSnapshotWizardModel(IApplication application) {
		this.application = application;
		this.filepath = getSnapshotFromPreferences(application, isDeploymentSnapshot());
	}

	public IApplication getApplication() {
		return application;
	}

	public String setFilepath(String filepath) {
		return this.filepath = filepath;
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

	public String restoreSnapshot(IProgressMonitor monitor) throws IOException {
		if (monitor.isCanceled()) {
			return null;
		}
		storeSnapshotToPreferences(filepath, deploymentSnapshot);
		Session session = SSHSessionRepository.getInstance().getSession(application);
		FileInputStream snapshotFileInputStream = new FileInputStream(new File(getFilepath()));
		InputStream saveResponse = null;
		if (isDeploymentSnapshot()) {
			saveResponse = new ApplicationSSHSession(application, session).restoreDeploymentSnapshot(snapshotFileInputStream, hotDeploy);
		} else {
			saveResponse = new ApplicationSSHSession(application, session).restoreFullSnapshot(snapshotFileInputStream);
		}
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		StreamUtils.writeTo(saveResponse, byteArrayOut);
		return new String(byteArrayOut.toByteArray());
	}

	public boolean isHotDeploy() {
		return hotDeploy;
	}

	public boolean setHotDeploy(boolean hotDeploy) {
		return this.hotDeploy = hotDeploy;
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