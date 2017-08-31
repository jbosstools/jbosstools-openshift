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
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

public abstract class AbstractOpenShiftConnection extends AbstractOpenShiftExplorerItem {

	public AbstractOpenShiftConnection(TreeItem connectionItem) {
		super(connectionItem);
	}
	
	/**
	 * Removes connection from OpenShift explorer view.
	 */
	public void remove() {
		item.select();
		
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_CONNECTION).select();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.REMOVE_CONNECTION));
		
		new DefaultShell(OpenShiftLabel.Shell.REMOVE_CONNECTION);
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.REMOVE_CONNECTION));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	
}
