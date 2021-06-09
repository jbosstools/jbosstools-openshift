/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * OpenShift Application Explorer Devfile implemented with RedDeer.
 * 
 */
public class OpenShiftODODevfile extends AbstractOpenShiftApplicationExplorerItem {
		
	private String name;
	
	public OpenShiftODODevfile(TreeItem devfileItem) {
		super(devfileItem);
		this.name = treeViewerHandler.getNonStyledText(item);
	}
	
  public void openCreateComponentWizard() {
    select();
    new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_COMPONENT).select();
  }

	 public String getName() {
		return name;
	}	
}
