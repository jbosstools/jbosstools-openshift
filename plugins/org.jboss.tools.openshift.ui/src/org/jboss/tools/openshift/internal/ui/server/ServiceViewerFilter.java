/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServiceViewerFilter extends ViewerFilter {

	private Text filterText;

	public ServiceViewerFilter(Text filterText) {
		Assert.isLegal(!DisposeUtils.isDisposed(filterText));
		this.filterText = filterText;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (!(element instanceof ObservableTreeItem)) {
			return false;
		}
		if (!(((ObservableTreeItem) element).getModel() instanceof IResource)) {
			return false;
		}
		
		IResource resource = (IResource) ((ObservableTreeItem) element).getModel();
		if (resource instanceof IService) {
			return isMatching(filterText.getText(), (IService) resource);
		} else {
			return true;
		}
	}

	private boolean isMatching(String filter, IService service) {
		for (String label : service.getSelector().values()) {
			if (!StringUtils.isEmpty(label) 
					&& label.contains(filter)) {
				return true;
			}
		}
		return false;
	}
}
