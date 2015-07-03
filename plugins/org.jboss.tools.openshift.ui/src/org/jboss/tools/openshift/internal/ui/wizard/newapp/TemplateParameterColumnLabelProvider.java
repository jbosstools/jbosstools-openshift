/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;

import com.openshift.restclient.model.template.IParameter;

/**
 * A column provider for template parameters
 * @author jeff.cantrill
 */
public class TemplateParameterColumnLabelProvider implements IColumnLabelProvider<IParameter>{

	protected static final String GENERATED = "(generated)";

	@Override
	public String getValue(IParameter param) {
		boolean hasGenerator = isNotBlank(param.getGeneratorName());
		boolean hasValue = isNotBlank(param.getValue());
		if(hasGenerator) {
			return hasValue ? param.getValue() : GENERATED;
		}
		return defaultIfBlank(param.getValue(), "");
	}

}
