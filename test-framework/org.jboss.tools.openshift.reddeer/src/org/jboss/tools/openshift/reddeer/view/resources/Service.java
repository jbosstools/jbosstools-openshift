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
import org.jboss.reddeer.swt.api.TreeItem;

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
		if (nonstyledText == null || nonstyledText.equals("")) {
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
	
}
