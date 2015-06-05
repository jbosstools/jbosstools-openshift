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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.ITextControl;
import org.jboss.tools.openshift.internal.ui.wizard.application.TemplateListPage.TemplateNode;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.IResource;

/**
 * Filter a list of templates by name or if
 * they have a tag annotation
 * 
 * @author jeff.cantrill
 */
public class TemplateViewerFilter extends ViewerFilter {
	
	private ITextControl filterText;
	
	public TemplateViewerFilter(ITextControl txtFilter) {
		this.filterText = txtFilter;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof TemplateNode) return true;
		if(!(element instanceof IResource)) {
			return false;
		}
		final String text = filterText.getText();
		if(StringUtils.isBlank(text)) {
			return true;
		}
		IResource resource = (IResource) element;
		if(resource.getName().contains(text)) {
			return true;
		}
		return resource.accept(new CapabilityVisitor<ITags, Boolean>() {
			@Override
			public Boolean visit(ITags capability) {
				String tags = StringUtils.join(capability.getTags(),",");
				return tags.contains(text);
			}
		}, Boolean.FALSE);
	}

}
