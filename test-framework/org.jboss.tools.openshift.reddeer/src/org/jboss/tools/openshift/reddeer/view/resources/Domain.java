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

import org.jboss.reddeer.swt.api.TreeItem;

public class Domain extends AbstractOpenShiftExplorerItem {

	public Domain(TreeItem domainItem) {
		super(domainItem);
	}

	/**
	 * Get OpenShift 2 application.
	 * 
	 * @param name name of application without application type 
	 * @return OpenShift 2 application
	 */
	public OpenShift2Application getApplication(String name) {
		return new OpenShift2Application(treeViewerHandler.getTreeItem(item, name));
	}
}
	