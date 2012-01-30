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

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.Hyperlink;
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
			// tree editor takes some time to display, show text in the meantime
			createStyledText(property, cell);
			createLink(property, cell);
		} else {
			cell.setText(property.getValue());
		}
	}

	protected void createLink(IProperty property, final ViewerCell cell) {
		final Hyperlink link = new Hyperlink((Tree) cell.getControl(), SWT.TRANSPARENT);
		link.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ACTIVE_HYPERLINK_COLOR));
		link.setUnderlined(true);
		link.setText(property.getValue());
		link.setBackground(cell.getBackground());
		link.addMouseListener(onLinkClicked(property.getValue()));

		TreeUtils.createTreeEditor(link, property.getValue(), cell);
	}

	private void createStyledText(IProperty property, final ViewerCell cell) {
		StyledString.Styler style = new StyledString.Styler() {
			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.foreground = cell.getControl().getDisplay().getSystemColor(SWT.COLOR_BLUE);
				textStyle.underline = true;
			}
		};
		StyledString styledString = new StyledString(property.getValue(), style);
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setText(styledString.getString());
	}

	protected MouseAdapter onLinkClicked(final String url) {
		return new MouseAdapter() {

			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) { // left button only
					BrowserUtil.checkedCreateExternalBrowser(
							url, OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				}
			}
		};
	}
}
