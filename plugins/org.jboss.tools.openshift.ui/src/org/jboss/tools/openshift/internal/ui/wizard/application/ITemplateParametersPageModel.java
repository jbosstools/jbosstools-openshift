/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import java.util.List;

import com.openshift.restclient.model.template.IParameter;

/**
 * @author jeff.cantrill
 */
public interface ITemplateParametersPageModel {
	
	static final String PROPERTY_SELECTED_PARAMETER = "selectedParameter";
	static final String PROPERTY_PARAMETERS = "parameters";
	
	/**
	 * Retrieve the list of template parameters
	 * @return
	 */
	List<IParameter> getParameters();
	
	/**
	 * Set the list of template parameters
	 * @param parameters
	 */
	void setParameters(List<IParameter> parameters);
	
	/**
	 * Get the selected parameter
	 * @return
	 */
	IParameter getSelectedParameter();
	
	/**
	 * Set the selected parameter
	 * @param parameter
	 */
	void setSelectedParameter(IParameter parameter);
	
	/**
	 * Update the given parameter with the given value
	 * @param param
	 * @param value
	 */
	void updateParameterValue(IParameter param, String value);

	/**
	 * Reset the given parameter to its default value
	 * @param parameter
	 */
	void resetParameter(IParameter parameter);

}
