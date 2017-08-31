/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;

public abstract class AbstractOpenShiftExplorerItem {

	protected TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();
	
	protected TreeItem item;	
	
	public AbstractOpenShiftExplorerItem(TreeItem item) {
		this.item = item;
	}
	
	protected void activateOpenShiftExplorerView() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.activate();
	}
	
	public void refresh() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.REFRESH).select();	
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	public void select() {
		activateOpenShiftExplorerView();		
		item.select();
	}
	
	public void expand() {
		activateOpenShiftExplorerView();
		item.expand();
		
		// There can be some processing, wait for it
		new WaitWhile(new JobIsRunning(new Matcher[] {new IsEqual<String>("Loading OpenShift resources...")}), TimePeriod.DEFAULT);
	}
	
	public TreeItem getTreeItem() {
		return item;
	}
}
