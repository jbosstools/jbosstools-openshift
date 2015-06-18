/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.viewer.GTK3WorkaroundStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.TemplateNode;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A label provider for template list page
 * 
 * @author jeff.cantrill
 *
 */
public class TemplateListPageLabelProvier  extends GTK3WorkaroundStyledCellLabelProvider{
	
	@Override
	public void update(ViewerCell cell) {
		final Object element = cell.getElement();
		StyledString text = getStyledText(element);
		if(text != null) {
			cell.setText(text.getString());
			cell.setStyleRanges(text.getStyleRanges());
		}
		cell.setImage(getImage(element));
		super.update(cell);
	}

	public Image getImage(Object element) {
		if(element instanceof TemplateNode)
			return OpenShiftCommonImages.GLOBE_IMG;
		return null;
	}

	public StyledString getStyledText(Object element) {
		if(element instanceof TemplateNode) {
			return new StyledString(element.toString());
		}else if(element instanceof ITemplate) {
				ITemplate template = (ITemplate) element;
				final StyledString text = new StyledString(template.getName());
				template.accept(new CapabilityVisitor<ITags, Object>() {
			            @Override
			            public Object visit(ITags capability) {
			                text.append(NLS.bind(" ({0})", StringUtils.join(capability.getTags(), ", ")), StyledString.DECORATIONS_STYLER);
			                return null;
			            }
			        }, null);
				return text;
			}
		return null;
	}

}
