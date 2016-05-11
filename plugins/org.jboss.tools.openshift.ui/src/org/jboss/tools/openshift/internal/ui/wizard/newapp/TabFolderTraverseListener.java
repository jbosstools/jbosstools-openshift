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
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;

/**
 * Shifts focus from tab item header into its children and then out to the tab sibling.
 * 
 * @author Viacheslav Kabanovich
 */
public class TabFolderTraverseListener  implements Listener {
	TabFolder tabFolder;
	List<Control> firstControls = new ArrayList<>();

	public TabFolderTraverseListener(TabFolder tabFolder) {
		this.tabFolder = tabFolder;
		tabFolder.addListener(SWT.Traverse, this);
	}
	@Override
	public void handleEvent(Event event) {
		if(event.detail == SWT.TRAVERSE_ARROW_NEXT || event.detail == SWT.TRAVERSE_TAB_NEXT) {
			int i = tabFolder.getSelectionIndex();
			if(i >= 0 && i < firstControls.size() && firstControls.get(i) != null) {
				firstControls.get(i).forceFocus();
				event.doit = false;
			}
		}
	}

	/**
	 * Call this method for i-th tab with all controls that may be focused.
	 * 
	 * @param i
	 * @param controls
	 */
	public void bindTabControls(int i, final Control... controls) {
		while(firstControls.size() < i) {
			firstControls.add(null);
		}
		if(firstControls.size() == i) {
			firstControls.add(controls[0]);
		} else {
			firstControls.set(i, controls[0]);
		}
		for (Control c: controls) {
			c.addListener(SWT.Traverse, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if(event.detail == SWT.TRAVERSE_TAB_NEXT) {
						if(c != controls[controls.length - 1]) {
							setFocusToNextSibling(c);
						} else {
							setFocusToNextSibling(tabFolder);
						}
						event.doit = false;
					}
				}
			});
		}
	}

	private void setFocusToNextSibling(Control c) {
		Composite parent = c.getParent();
		Control[] children = parent.getTabList();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child == c) {
				for (int j = i + 1; j < children.length; j++) {
					Control nc = children[j];
					if (nc.isEnabled() && nc.setFocus()) {
						return;
					}
				}
			}
		}
	}

}
