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

import org.hamcrest.Matcher;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsKilled;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * OpenShift service is represented as a TreeItem in OpenShift 
 * Explorer view, placed under a project.
 *
 * @author mlabuda@redhat.com
 * 
 *
 */
public class Service extends AbstractOpenShiftExplorerItem {

	public Service(TreeItem item) {
		super(item);
	}

	/**
	 * Gets a name of an OpenShift service
	 * 
	 * @return name of an OpenShift service 
	 */
	public String getName() {
		return treeViewerHandler.getNonStyledText(item);
	}
	
	/**
	 * Gets a route of an OpenShift service.
	 * 
	 * @return route of an OpenShift service if exists, null otherwise
	 */
	public String getRoute() {
		String nonstyledText = treeViewerHandler.getStyledTexts(item)[0].trim();
		if (nonstyledText == null || "".equals(nonstyledText)) {
			return null;
		} else {
			return nonstyledText;
		}
	}
	
	/**
	 * Gets a pod with a specified name.
	 * 
	 * @param name name of a pod
	 * @return Pod belonging to an OpenShift service
	 */
	public Pod getPod(String name) {
		return new Pod(treeViewerHandler.getTreeItem(item, name));
	}
	
	
	/**
	 * Gets first pod matching a specified matcher.
	 * 
	 * @param nameMatcher name matcher of a pod
	 * @return Pod belonging to an OpenShift service if there is any matching
	 * 	a specified matcher; null otherwise.
	 */
	@SuppressWarnings("unchecked")
	public Pod getPod(Matcher<String>... matchers) {
		for (TreeItem treeItem: item.getItems()) {
			if (!treeItem.isDisposed()) {
				String treeItemText = treeItem.getText();
				boolean matches = true;
				for (Matcher<String> matcher: matchers) {
					if (!matcher.matches(treeItemText)) {
						matches = false;
						break;
					}
				}
				if (matches) {
					return new Pod(treeItem);
				}
			}
		}
		return null;
	}
	
	/**
	 * Creates server adapter for this service with default values.
	 */
	
	public void createServerAdapter() {
		select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_ADAPTER_FROM_EXPLORER).select();
		
		new DefaultShell(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS);
		new FinishButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.SERVER_ADAPTER_SETTINGS));
		new WaitUntil(new JobIsKilled("Refreshing server adapter list"), TimePeriod.LONG, false);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
	}
	
}
