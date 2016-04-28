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
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.util.List;

/**
 * Model interface for manipulating env variables
 * @author jeff.cantrill
 *
 */
public interface IEnvironmentVariablesPageModel {
	
	static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";
	static final String PROPERTY_SELECTED_ENVIRONMENT_VARIABLE = "selectedEnvironmentVariable";
	
	List<EnvironmentVariable> getEnvironmentVariables();
	void setEnvironmentVariables(List<EnvironmentVariable> envVars);
	
	void setSelectedEnvironmentVariable(EnvironmentVariable envVar);
	EnvironmentVariable getSelectedEnvironmentVariable();
	EnvironmentVariable getEnvironmentVariable(String key);
	boolean isEnvironmentVariableModified(EnvironmentVariable envVar);
	
	void removeEnvironmentVariable(EnvironmentVariable envVar);
	void resetEnvironmentVariable(EnvironmentVariable envVar);
	void updateEnvironmentVariable(EnvironmentVariable envVar, String key, String value);
	void addEnvironmentVariable(String key, String value);
	
}
