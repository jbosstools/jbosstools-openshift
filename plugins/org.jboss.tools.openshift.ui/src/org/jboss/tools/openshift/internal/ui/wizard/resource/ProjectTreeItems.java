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
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.ResourceKind;

/**
 * @author Jeff Maury
 */
public class ProjectTreeItems implements IModelFactory {

	public static final ProjectTreeItems INSTANCE = new ProjectTreeItems();
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> createChildren(Object parent) {
		if (parent instanceof Connection) {
			return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
		}
		return Collections.emptyList();
	}

	public List<ObservableTreeItem> create(Collection<?> openShiftObjects) {
		if (openShiftObjects == null) {
			return Collections.emptyList();
		}
		List<ObservableTreeItem> items = new ArrayList<>();
		for (Object openShiftObject : openShiftObjects) {
			ObservableTreeItem item = create(openShiftObject);
			if (item != null) {
				items.add(item);
			}
		}
		return items;
	}

	@Override
	public ObservableTreeItem create(Object object) {
		return new ObservableTreeItem(object, this);
	}
}
