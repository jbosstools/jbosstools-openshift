/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.treeitem;

import java.util.Collections;
import java.util.List;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * @author Andre Dietisheim
 */
public class ProjectTreeItem extends ObservableTreeItem {

	public ProjectTreeItem(Object model) {
		super(model);
	}

	@Override
	protected List<? extends Object> loadChildren() {
		if (!(getModel() instanceof IProject)) {
			return Collections.emptyList();
		}
		return ((IProject) getModel()).getResources(ResourceKind.BUILD_CONFIG);
	}

	@Override
	protected ObservableTreeItem createChildItem(Object model) {
		return new BuildConfigTreeItem(model);
	}

}
