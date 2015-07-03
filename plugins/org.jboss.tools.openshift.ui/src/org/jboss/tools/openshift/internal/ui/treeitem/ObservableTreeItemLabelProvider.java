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
package org.jboss.tools.openshift.internal.ui.treeitem;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;

/**
 * A tree viewer label provider for ObservableTreeItems. Delegates to
 * OpenShiftExplorerLabelProvider to get labels and images.
 * 
 * @author Andre Dietisheim
 */
public class ObservableTreeItemLabelProvider 
	extends BaseLabelProvider 
	implements IStyledLabelProvider, ILabelProvider {

	private OpenShiftExplorerLabelProvider explorerLabelProvider;

	public ObservableTreeItemLabelProvider() {
		this.explorerLabelProvider = new OpenShiftExplorerLabelProvider();
	}

	@Override
	public Image getImage(Object element) {
		if (element == null
				|| !(element instanceof ObservableTreeItem)) {
			return null;
		}
		return explorerLabelProvider.getImage(((ObservableTreeItem) element).getModel());
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element == null) {
			return null;
		} else if (!(element instanceof ObservableTreeItem)) {
			return new StyledString(ObjectUtils.toString(element));
		}
		return explorerLabelProvider.getStyledText(((ObservableTreeItem) element).getModel());
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public String getText(Object element) {
		if (element == null) {
			return null;
		} else if (!(element instanceof ObservableTreeItem)) {
			return ObjectUtils.toString(element);
		}
		return explorerLabelProvider.getText(((ObservableTreeItem) element).getModel());
	}

}
