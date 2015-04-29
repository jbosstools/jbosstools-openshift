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
 * Filter a viewer of resources by tag if the
 * resource is annotated with tags
 * 
 * @author jeff.cantrill
 */
public class AnnotationTagViewerFilter extends ViewerFilter {
	
	private ITextControl filterText;
	
	public AnnotationTagViewerFilter(ITextControl txtFilter) {
		this.filterText = txtFilter;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof TemplateNode) return true;
		if(!(element instanceof IResource)) {
			return false;
		}
		IResource resource = (IResource) element;
		return resource.accept(new CapabilityVisitor<ITags, Boolean>() {
			@Override
			public Boolean visit(ITags capability) {
				String tags = StringUtils.join(capability.getTags(),",");
				return tags.contains(filterText.getText());
			}
		}, Boolean.FALSE);
	}

}
