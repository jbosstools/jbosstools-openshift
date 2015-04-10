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

import java.util.Collection;

import com.openshift.restclient.model.template.ITemplate;

/**
 * Page model that allows retrieval and selection of templates,
 * 
 * @author jeff.cantrill
 *
 */
public interface ITemplateListPageModel {
	
	static final String PROPERTY_TEMPLATE = "template";

	/**
	 * Get the collection of templates
	 * @return
	 */
	Collection<ITemplate> getTemplates();
	
	/**
	 * Set the selected template
	 * @param template
	 */
	void setTemplate(ITemplate template);
	
	/**
	 * Retrieve the selected template
	 * @return
	 */
	ITemplate getTemplate();
	
}
