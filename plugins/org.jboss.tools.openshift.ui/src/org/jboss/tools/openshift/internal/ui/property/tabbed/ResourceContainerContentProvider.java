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
package org.jboss.tools.openshift.internal.ui.property.tabbed;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.internal.ui.models.IElementListener;
import org.jboss.tools.openshift.internal.ui.models.IOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.IResourceContainer;;

public class ResourceContainerContentProvider implements IStructuredContentProvider {
	private String resourceKind;

	public ResourceContainerContentProvider(String kind) {
		this.resourceKind= kind;
	}
	
	private StructuredViewer viewer;
	private IElementListener listener= new IElementListener() {
		
		@Override
		public void elementChanged(IOpenshiftUIElement<?, ?> element) {
			viewer.refresh();
		}
	};

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer= (StructuredViewer) viewer;
		if (oldInput instanceof IOpenshiftUIElement<?, ?>) {
			((IOpenshiftUIElement<?, ?>) oldInput).getRoot().removeListener(listener);
		}
		if (newInput instanceof IOpenshiftUIElement<?, ?>) {
			((IOpenshiftUIElement<?, ?>) newInput).getRoot().addListener(listener);
		}
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IResourceContainer<?, ?>) {
			return ((IResourceContainer<?, ?>) inputElement).getResourcesOfKind(resourceKind).toArray();
		}
		return null;
	}

}
