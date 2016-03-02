/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.utils;

import java.util.List;
import java.util.stream.Stream;

import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

/**
 * Utility class for the {@link ObservableTreeItem}
 */
public class ObservableTreeItemUtils {

	/**
	 * Flattens the hierarchy of {@link ObservableTreeItem} of the given {@code treeItem}. 
	 * @param treeItem the parent {@link ObservableTreeItem} to flatten
	 * @return a Stream containing the given {@code treeItem} along with all its children, grand-children, etc. items.
	 */
	public static Stream<ObservableTreeItem> flatten(final ObservableTreeItem treeItem) {
		return Stream.concat(Stream.of(treeItem), treeItem.getChildren().stream().flatMap(ObservableTreeItemUtils::flatten));
	}

	/**
	 * Checks if the given hierarchies of {@link ObservableTreeItem} in the given {@code items} contain the given {@code targetItem}
	 * @param items the hierarchies of {@link ObservableTreeItem} to traverse
	 * @param targetItem the target item to find
	 * @return <code>true</code> if any match was found, <code>false</code> otherwise.
	 */
	public static boolean contains(final List<ObservableTreeItem> items, final Object targetItem) {
		if (items == null || targetItem == null) {
			return false;
		}
		return items.stream().flatMap(ObservableTreeItemUtils::flatten).anyMatch(item -> item.getModel() == targetItem);
	}
}
