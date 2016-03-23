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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

/**
 * Utility class for the {@link ObservableTreeItem}
 */
public class ObservableTreeItemUtils {

	/**
	 * Flattens the hierarchy of {@link ObservableTreeItem} of the given {@code treeItem} into . 
	 * @param treeItem the parent {@link ObservableTreeItem} to flatten
	 * @return a Stream containing the given {@code treeItem} along with all its children, grand-children, etc. items.
	 */
	public static Stream<ObservableTreeItem> flatten(final ObservableTreeItem treeItem) {
		return Stream.concat(
				Stream.of(treeItem), 
				treeItem.getChildren().stream()
					.flatMap(ObservableTreeItemUtils::flatten));
	}

	/**
	 * Checks if the given hierarchies of {@link ObservableTreeItem} in the given {@code items} contain the given {@code targetItem}
	 * @param items the hierarchies of {@link ObservableTreeItem} to traverse
	 * @param targetModel the target item to find
	 * @return <code>true</code> if any match was found, <code>false</code> otherwise.
	 */
	public static boolean contains(final Object targetModel, final List<ObservableTreeItem> items) {
		if (items == null
				|| items.isEmpty()
				|| targetModel == null) {
			return false;
		}
		return items.stream()
				.flatMap(ObservableTreeItemUtils::flatten)
				.anyMatch(item -> targetModel.equals(item.getModel()));
	}
	
	public static ObservableTreeItem getItemFor(Object model, final List<ObservableTreeItem> items) {
		if (items == null
				|| model == null) {
			return null;
		}
		Optional<ObservableTreeItem> item =  items.stream()
				.filter(i -> Objects.equals(model, ((ObservableTreeItem) i).getModel())).findFirst();
		if (item.isPresent()) {
			return item.get();
		} else {
			return null;
		}
	}

	/**
	 * Returns all models within the given items that match the given type. Wont recursively inspect child items.
	 * 
	 * @param type
	 * @param items
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getAllModels(Class<? extends T> type, List<ObservableTreeItem> items) {
		if (items == null 
				|| items.isEmpty()) {
			return Collections.emptyList();
		}

		return items.stream()
			.filter(item -> ((ObservableTreeItem) item).getModel() != null 
						&& type.isAssignableFrom(((ObservableTreeItem) item).getModel().getClass()))
			.map(child -> (T) ((ObservableTreeItem) child).getModel()) 
			.collect(Collectors.toList());
	}

	/**
	 * Returns the 1st model that is of the given type. Returns {@code null}
	 * otherwise. Recursively inspects all items and their children.
	 * 
	 * @param type
	 * @param items
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFirstModel(Class<? extends T> type, List<ObservableTreeItem> items) {
		if (items == null) {
			return null;
		}
		return items.stream()
				.flatMap(ObservableTreeItemUtils::flatten)
				.filter(item -> item.getModel() != null 
					&& (type == null
						|| type.isAssignableFrom(item.getModel().getClass())))
				.map(item -> (T) item.getModel())
				.findFirst()
				.orElseGet(() -> null);
	}
}
