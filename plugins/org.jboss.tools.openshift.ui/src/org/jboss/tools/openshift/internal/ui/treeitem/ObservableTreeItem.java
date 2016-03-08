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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

/**
 * A tree item that can load children lazily and notify interested parties about the change.
 * 
 * @author Andre Dietisheim
 *
 */
public class ObservableTreeItem extends ObservableUIPojo {

	public static final String PROPERTY_CHILDREN = "children";

	private List<ObservableTreeItem> children;

	private Object model;
	private IModelFactory factory;

	public ObservableTreeItem(Object model) {
		this(model, null);
	}

	public ObservableTreeItem(Object model, IModelFactory factory) {
		this.model = model;
		this.factory = factory;
	}

	public List<ObservableTreeItem> getChildren() {
		return children;
	}

	public void setChildren(List<ObservableTreeItem> children) {
		firePropertyChange(PROPERTY_CHILDREN, this.children, this.children = children);
	}

	public Object getModel() {
		return model;
	}

	public void load() {
		List<ObservableTreeItem> children = createTreeItems(loadChildren());
		setChildren(children);
		for (ObservableTreeItem item : children) {
			item.load();
		}
	}

	public boolean isLoaded() {
		return children != null;
	}
	
	protected List<? extends Object> loadChildren() {
		if (factory != null) {
			return factory.createChildren(model);
		} else {
			return Collections.emptyList();
		}
	}

	protected List<ObservableTreeItem> createTreeItems(List<?> children) {
		List<ObservableTreeItem> items = new ArrayList<>();
		for (Object child : children) {
			ObservableTreeItem childItem = createChildItem(child);
			if (childItem != null) {
				items.add(childItem);
			}
		}
		return items;
	}

	protected ObservableTreeItem createChildItem(Object model) {
		return new ObservableTreeItem(model, this.factory);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ObservableTreeItem))
			return false;
		ObservableTreeItem other = (ObservableTreeItem) obj;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ObservableTreeItem [children=" + children + ", model=" + model + "]";
	}

}
