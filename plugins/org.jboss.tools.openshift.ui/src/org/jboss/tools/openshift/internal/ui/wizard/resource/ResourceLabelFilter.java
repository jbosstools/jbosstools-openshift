/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class ResourceLabelFilter extends ViewerFilter {

	private IObservableValue<KeyValueFilter> labelFilter;
	private Viewer viewer;

	public ResourceLabelFilter(IObservableValue<KeyValueFilter> labelFilter) {
		this.labelFilter = labelFilter;
		this.labelFilter.addValueChangeListener(event -> {
			if (viewer != null) {
				viewer.refresh();
			}
		});
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		this.viewer = viewer;
		if (!(element instanceof IResource)) {
			return false;
		}

		IResource resource = (IResource) element;
		return ResourceUtils.hasMatchingLabels(labelFilter.getValue(), resource);
	}

}
