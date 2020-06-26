/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;

/**
 * 
 * Abstract OpenShift Application Explorer Item implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public abstract class AbstractOpenShiftApplicationExplorerItem {

	protected TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();
	
	protected TreeItem item;	
	
	public AbstractOpenShiftApplicationExplorerItem(TreeItem item) {
		this.item = item;
	}
	
	protected void activateOpenShiftApplicationExplorerView() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.activate();
	}
	
	public void select() {
		activateOpenShiftApplicationExplorerView();		
		item.select();
	}
	
	public void expand() {
		activateOpenShiftApplicationExplorerView();
		item.expand();
		
		// There can be some processing, wait for it
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT);
	}
	
	public TreeItem getTreeItem() {
		return item;
	}
}
