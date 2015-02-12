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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
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
	private IProject project;

	public SaveSnapshotWizardModel(IApplication application) {
		this.application = application;
		this.filepath = FileUtils.getAvailableFilepath(getSnapshotFromPreferences(application, deploymentSnapshot));
	}

	public IApplication getApplication() {
		return application;
	}

	public void setProject(IProject project){
		this.project = project;
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

	public void saveSnapshot(IProgressMonitor monitor) throws IOException, CoreException {
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
		if (project != null) {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}

	private void storeSnapshotToPreferences(String filepath, boolean deploymentSnapshot) {
		if (deploymentSnapshot) {
			ExpressCorePreferences.INSTANCE.saveDeploymentSnapshot(getApplication(), filepath);
		} else {
			ExpressCorePreferences.INSTANCE.saveFullSnapshot(getApplication(), filepath);
		}
	}
	
	private String getSnapshotFromPreferences(IApplication application, boolean deploymentSnapshot) {
		if (deploymentSnapshot) {
			return ExpressCorePreferences.INSTANCE.getDeploymentSnapshot(application);
		} else {
			return ExpressCorePreferences.INSTANCE.getFullSnapshot(application);
		}
	}
}