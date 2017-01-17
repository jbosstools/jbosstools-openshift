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
import org.jboss.tools.openshift.reddeer.enums.ResourceState;


/**
 * Pod, one or more containers deployed together on a single host, are represented
 * as a TreeItem in OpenShift Explorer view. Pods are placed right under an OpenShift
 * Service in OpenShift explorer. Group of pods belong to a service.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class Pod extends AbstractOpenShiftExplorerItem {

	public Pod(TreeItem item) {
		super(item);
	}
	
	/**
	 * Gets state of a pod.
	 * 
	 * @return state of a pod
	 */
	public ResourceState getState() {
		String state = treeViewerHandler.getStyledTexts(item)[0].trim();
		return ResourceState.valueOf(state);
	}
	
	/**
	 * Gets name of a pod.
	 * @return name of a pod
	 */
	public String getName() {
		return treeViewerHandler.getNonStyledText(item).trim();
	}
}
