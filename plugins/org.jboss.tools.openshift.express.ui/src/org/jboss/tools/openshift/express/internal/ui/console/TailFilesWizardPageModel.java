/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.console;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class TailFilesWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_FILE_PATTERN = "filePattern";

	private final IApplication application;

	private String filePattern;

	public TailFilesWizardPageModel(final IApplication app) {
		this.application = app;
		this.filePattern = OpenShiftPreferences.INSTANCE.getTailFileOptions(application);
	}

	public void setFilePattern(final String filePattern) {
		firePropertyChange(
				PROPERTY_FILE_PATTERN, this.filePattern, this.filePattern = filePattern);
		OpenShiftPreferences.INSTANCE.saveTailFileOptions(application, filePattern);
	}

	public String getFilePattern() {
		return filePattern;
	}

	public void resetFilePattern() {
		setFilePattern(OpenShiftPreferences.INSTANCE.getTailFileOptions(null));
	}

	public IApplication getApplication() {
		return application;
	}

}
