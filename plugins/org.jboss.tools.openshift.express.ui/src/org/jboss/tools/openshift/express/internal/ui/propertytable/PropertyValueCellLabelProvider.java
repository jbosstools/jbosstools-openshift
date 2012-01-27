/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.propertytable;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Tree;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TreeUtils;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class PropertyValueCellLabelProvider extends AbstractPropertyCellLabelProvider {

	protected void update(IProperty property, ViewerCell cell) {
		if (property.isLink()) {
			createLink(property, cell);
		} else {
			cell.setText(property.getValue());
		}
	}

	protected void createLink(IProperty property, final ViewerCell cell) {
		Link link = new Link((Tree) cell.getControl(), SWT.NONE);
		link.setText("<a>" + property.getValue() + "</a>");
		link.setBackground(cell.getBackground());
		link.addMouseListener(onLinkClicked(property.getValue()));

		TreeUtils.createTreeEditor(link, property.getValue(), cell);
	}

	protected MouseAdapter onLinkClicked(final String url) {
		return new MouseAdapter() {

			@Override
			public void mouseUp(MouseEvent e) {
				BrowserUtil.checkedCreateExternalBrowser(
						url, OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
			}
		};
	}
}
