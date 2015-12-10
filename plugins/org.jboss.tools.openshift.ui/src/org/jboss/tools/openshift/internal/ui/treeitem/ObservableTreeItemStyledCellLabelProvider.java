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

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.viewer.GTK3WorkaroundStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;

/**
 * A tree viewer label provider for ObservableTreeItems. Delegates to
 * OpenShiftExplorerLabelProvider to get labels and images.
 * 
 * @author Andre Dietisheim
 */
public class ObservableTreeItemStyledCellLabelProvider extends GTK3WorkaroundStyledCellLabelProvider {

	private OpenShiftExplorerLabelProvider explorerLabelProvider;

	public ObservableTreeItemStyledCellLabelProvider() {
		this.explorerLabelProvider = new OpenShiftExplorerLabelProvider();
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (!(element instanceof ObservableTreeItem)) {
			return;
		}
		ObservableTreeItem item = (ObservableTreeItem) element;
		StyledString styledText = explorerLabelProvider.getStyledText(item.getModel());
		cell.setText(styledText.getString());
		cell.setStyleRanges(styledText.getStyleRanges());
		Image image = explorerLabelProvider.getImage(item.getModel());
		cell.setImage(image);
	}
}
