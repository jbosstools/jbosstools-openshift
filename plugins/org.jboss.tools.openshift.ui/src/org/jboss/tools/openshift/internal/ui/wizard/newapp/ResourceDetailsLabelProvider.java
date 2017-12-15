/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsContentProvider.ResourceProperty;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;

/**
 * @author jeff.cantrill
 */
public class ResourceDetailsLabelProvider extends StyledCellLabelProvider implements IStyledLabelProvider {

	private static final String LABEL_NOT_PROVIDED = "(Not Provided)";
	private static final String LABEL_UNKNOWN = "(Unknown)";
	private static final String LABEL_UNKNOWN_PARAMETER = "(Unknown parameter {0})";

	private Map<String, IParameter> templateParameters;

	public ResourceDetailsLabelProvider(Map<String, IParameter> templateParameters) {
		super();
		this.templateParameters = templateParameters;
	}

	@Override
	public void update(ViewerCell cell) {
		final Object element = cell.getElement();
		StyledString text = getStyledText(element);
		if (text != null) {
			cell.setText(text.getString());
			cell.setStyleRanges(text.getStyleRanges());
		}
		super.update(cell);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			StyledString text = new StyledString(StringUtils.capitalize(resource.getKind()));
			text.append(" ").append(replaceParameters(resource.getName()), StyledString.QUALIFIER_STYLER);
			return text;
		}
		if (element instanceof ResourceProperty) {
			ResourceProperty property = (ResourceProperty) element;
			StyledString text = new StyledString(StringUtils.capitalize(property.getProperty()));
			text.append(": ");
			String value = null;
			if (property.getValue() instanceof Map) {
				value = org.jboss.tools.openshift.common.core.utils.StringUtils.serialize((Map) property.getValue());
			} else if (property.getValue() instanceof Collection) {
				value = StringUtils.join((Collection) property.getValue(), ", ");
			} else {
				value = property.getValue() != null ? property.getValue().toString() : "";
			}

			if (StringUtils.isBlank(value)) {
				if (property.isUnknownValue()) {
					value = LABEL_UNKNOWN;
				} else {
					value = LABEL_NOT_PROVIDED;
				}
			}
			text.append(replaceParameters(value), StyledString.QUALIFIER_STYLER);
			return text;
		}
		return null;
	}

	private String replaceParameters(String str) {
		StringBuffer result = new StringBuffer();
		Pattern p = Pattern.compile("\\$\\{[^}]+\\}");
		Matcher m = p.matcher(str);
		while (m.find()) {
			String parameterVariable = m.group();
			String parameterName = parameterVariable.substring(2, parameterVariable.length() - 1);
			if (this.templateParameters.containsKey(parameterName)) {
				m.appendReplacement(result, this.templateParameters.get(parameterName).getValue());
			} else {
				m.appendReplacement(result, NLS.bind(LABEL_UNKNOWN_PARAMETER, parameterName));
			}
		}
		m.appendTail(result);
		return result.toString();
	}

	@Override
	public Image getImage(Object paramObject) {
		return null;
	}

}
