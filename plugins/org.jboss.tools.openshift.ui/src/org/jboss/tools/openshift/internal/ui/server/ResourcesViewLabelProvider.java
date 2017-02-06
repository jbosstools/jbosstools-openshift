/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ResourcesViewLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (!(element instanceof ObservableTreeItem)) {
			return;
		} 
		if (!(((ObservableTreeItem) element).getModel() instanceof IResource)) {
				return;
		}
		
		IResource resource = (IResource) ((ObservableTreeItem) element).getModel();
		
		StyledString text = new StyledString();
		if (resource instanceof com.openshift.restclient.model.IProject) {
			createProjectLabel(text, (com.openshift.restclient.model.IProject) resource);
		} else if (resource instanceof IService) {
			createServiceLabel(text, (IService) resource);
		} else if (resource instanceof IReplicationController) {
		    createReplicationControllerLabel(text, (IReplicationController) resource);
		}

		cell.setText(text.toString());
		cell.setStyleRanges(text.getStyleRanges());
		super.update(cell);
	}

    private void createProjectLabel(StyledString text, com.openshift.restclient.model.IProject resource) {
		text.append(resource.getName());
	}

	private void createServiceLabel(StyledString text, IService service) {
		text.append(service.getName());
		String selectorsDecoration = org.jboss.tools.openshift.common.core.utils.StringUtils.toString(service.getSelector());
		if (!StringUtils.isEmpty(selectorsDecoration)) {
			text.append(" ", StyledString.DECORATIONS_STYLER);
			text.append(selectorsDecoration, StyledString.DECORATIONS_STYLER);
		}
	}

	private void createReplicationControllerLabel(StyledString text, IReplicationController rc) {
        text.append(rc.getName());
        String selectorsDecoration = org.jboss.tools.openshift.common.core.utils.StringUtils.toString(rc.getReplicaSelector());
        if (!StringUtils.isEmpty(selectorsDecoration)) {
            text.append(" ", StyledString.DECORATIONS_STYLER);
            text.append(selectorsDecoration, StyledString.DECORATIONS_STYLER);
        }
    }

}
