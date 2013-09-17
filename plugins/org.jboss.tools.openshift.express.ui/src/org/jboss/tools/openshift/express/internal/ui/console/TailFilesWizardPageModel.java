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

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class TailFilesWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_FILE_PATTERN = "filePattern";

	public static final String PROPERTY_ALL_GEARS = "allGears";
	
	private final IApplication application;
	
	private String filePattern = null;
	
	private boolean allGears = false;
	
	public TailFilesWizardPageModel(final IApplication app) {
		this.application = app;
		if (this.application != null && this.application.getApplicationScale() != null
				&& this.application.getApplicationScale() == ApplicationScale.SCALE) {
			allGears = true;
		}
	}

	public void setFilePattern(final String filePattern) {
		firePropertyChange(
				PROPERTY_FILE_PATTERN, this.filePattern, this.filePattern = filePattern);
	}

	public String getFilePattern() {
		return this.filePattern;
	}

	public void resetFilePattern() {
		setFilePattern("-f -n 100 */logs/*");
	}

	public void setAllGears(final boolean allGears) {
		firePropertyChange(
				PROPERTY_ALL_GEARS, this.allGears, this.allGears = allGears);
	}

	public boolean getAllGears() {
		return this.allGears;
	}
	
	public IApplication getApplication() {
		return application;
	}
	
}
