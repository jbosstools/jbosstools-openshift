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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.viewer.GTK3WorkaroundStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.ResourceDetailsContentProvider.ResourceProperty;

import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
public class ResourceDetailsLabelProvider extends GTK3WorkaroundStyledCellLabelProvider implements IStyledLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		final Object element = cell.getElement();
		StyledString text = getStyledText(element);
		if(text != null) {
			cell.setText(text.getString());
			cell.setStyleRanges(text.getStyleRanges());
		}		
		super.update(cell);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public  StyledString getStyledText(Object element) {
		if(element instanceof IResource) {
			IResource resource = (IResource) element;
			StyledString text = new StyledString(StringUtils.capitalize(resource.getKind().toString()));
			text.append(NLS.bind(" ({0})", resource.getName()), StyledString.QUALIFIER_STYLER);
			return text;
		}
		if(element instanceof ResourceProperty) {
			ResourceProperty property = (ResourceProperty) element;
			StyledString text = new StyledString(StringUtils.capitalize(property.getProperty()));
			text.append(": ");
			String value = null;
			if(property.getValue() instanceof Map) {
				value = org.jboss.tools.openshift.common.core.utils.StringUtils.serialize((Map) property.getValue());
			} else if(property.getValue() instanceof Collection) {
				value = StringUtils.join((Collection) property.getValue(), ", ");
			} else {
				value = property.getValue() != null ? property.getValue().toString() : "";
			}
			if(StringUtils.isBlank(value)) {
				value = ("(Not Provided)");
			}
			text.append(value, StyledString.QUALIFIER_STYLER);
			return text;
		}
		return null;
	}

	@Override
	public Image getImage(Object paramObject) {
		return null;
	}
	
	
}
