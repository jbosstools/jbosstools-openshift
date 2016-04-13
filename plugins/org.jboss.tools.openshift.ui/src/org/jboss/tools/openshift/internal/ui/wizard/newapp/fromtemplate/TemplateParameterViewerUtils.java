/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.openshift.restclient.model.template.IParameter;

/**
 * @author Andre Dietisheim
 */
public class TemplateParameterViewerUtils {

	private TemplateParameterViewerUtils() {
	}

	public static String getValueLabel(IParameter parameter) {
		if (parameter == null) {
			return null;
		}
		
		boolean hasGenerator = isNotBlank(parameter.getGeneratorName());
		boolean hasValue = isNotBlank(parameter.getValue());
		if(hasGenerator) {
			return hasValue ? parameter.getValue() : "(generated)";
		}
		return defaultIfBlank(parameter.getValue(), "");
	}

	/** 
	 * A viewer comparator that compares parameters in their names.
	 */
	public static class ParameterNameViewerComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			IParameter first = (IParameter) e1;
			IParameter other = (IParameter) e2;
			return first.getName().compareTo(other.getName());
		}
	}

}
