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

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.util.FileUtils;

/**
 * @author André Dietisheim
 */
public class SaveSnapshotWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_FILEPATH = "filepath";
	public static final String PROPERTY_DEPLOYMENT_SNAPSHOT = "deploymentSnapshot";

	private SaveSnapshotWizardModel wizardModel;
	private String directory;
	
	public SaveSnapshotWizardPageModel(SaveSnapshotWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		String filepath = wizardModel.getFilepath();
		if(filepath != null){
			this.directory = StringUtils.left(filepath, 
				filepath.length() - FilenameUtils.getName(filepath).length());
		}
			
	}

	public void setFilepath(String filepath) {
		firePropertyChange(
				PROPERTY_FILEPATH, this.wizardModel.getFilepath(), this.wizardModel.setFilepath(filepath));
	}

	public String getFilepath() {
		return wizardModel.getFilepath();
	}
	
	public String getDestination(){
		return this.directory;
	}
	
	public void setDestination(String directory) {
		this.directory = directory;
		String filepath = getFilePath(directory, wizardModel.getApplication().getName(), getSnapshotTypeString(isDeploymentSnapshot()));
		setFilepath(filepath);
	}

	private String getFilePath(String directory, String applicationName, String snapshotType) {
		String snashotFilename = MessageFormat.format("{0}-{1}.tar.gz", applicationName, snapshotType);
		File destinationFile = new File(directory, snashotFilename );
		String filepath = FileUtils.getAvailableFilepath(destinationFile.getAbsolutePath());
		return filepath;
	}

	public void setProject(IProject project) {
		if (project == null) {
			return;
		}
		wizardModel.setProject(project);
		setDestination(project.getLocation().toString());
	}
	
	private String getSnapshotTypeString(boolean deploymentSnapshot) {
		if (deploymentSnapshot) {
			return "deployment";
		} else {
			return "full";
		}
	}

	public void setDeploymentSnapshot(boolean deploymentSnapshot) {
		firePropertyChange(
				PROPERTY_DEPLOYMENT_SNAPSHOT, this.wizardModel.isDeploymentSnapshot(),
				this.wizardModel.setDeploymentSnapshot(deploymentSnapshot));
		if (directory != null) {
			String filepath = getFilePath(directory, wizardModel.getApplication().getName(), getSnapshotTypeString(deploymentSnapshot));
			setFilepath(filepath);
		}
	}

	public boolean isDeploymentSnapshot() {
		return wizardModel.isDeploymentSnapshot();
	}
}
