/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage;

import org.jboss.tools.openshift.internal.ui.wizard.common.IEnvironmentVariablesPageModel;

/**
 * Page model for configuring a build configuration;
 * 
 * @author jeff.cantrill
 *
 */
public interface IBuildConfigPageModel{
	
	static final String PROPERTY_GIT_REPOSITORY_URL = "gitRepositoryUrl";
	static final String PROPERTY_GIT_REFERENCE = "gitReference";
	static final String PROPERTY_CONTEXT_DIR = "contextDir";
	static final String PROPERTY_CONFIG_WEB_HOOK = "configWebHook";
	static final String PROPERTY_CONFIG_CHANGE_TRIGGER = "configChangeTrigger";
	static final String PROPERTY_IMAGE_CHANGE_TRIGGER = "imageChangeTrigger";
	
	/**
	 * Retrieve the page model to support manipulating env variables
	 * using during a build
	 * 
	 * @return
	 */
	IEnvironmentVariablesPageModel getEnvVariablesModel();
	
	String getGitRepositoryUrl();
	void setGitRepositoryUrl(String url);
	
	String getGitReference();
	void setGitReference(String ref);
	
	String getContextDir();
	void setContextDir(String contextDir);
	
	void setConfigWebHook(boolean value);
	boolean isConfigWebHook();
	
	void setConfigChangeTrigger(boolean value);
	boolean isConfigChangeTrigger();

	void setImageChangeTrigger(boolean value);
	boolean isImageChangeTrigger();
	
	/**
	 * Initialize the model for cases where we need
	 * deferred initialization (e.g. remote loading of image meta data)
	 */
	void init();
	
	/**
	 * The name in the form of NAME:TAG
	 * @return
	 */
	String getBuilderImageName();
	
	String getBuilderImageNamespace();
	
}
