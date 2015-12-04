/******************************************************************************* 
 * Copyright (c) 2014 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Andre Dietisheim
 */
public class CollectionUtils {

	/**
	 * Adds the given element to the given collection.
	 * 
	 * @param element the element to add
	 * @param collection the collection to add to
	 * @return
	 */
	public static <C extends Collection<E>, E> C addTo(E element, C collection) {
		collection.add(element);
		return collection;
	}
	
	/**
	 * Adds all elements withing the given collection to the given other collection
	 * 
	 * @param toAdd all elements that should get add
	 * @param collection
	 * @return
	 */
	public static <C extends Collection<E>, E> C addAllTo(C toAdd, C collection) {
		collection.addAll(toAdd);
		return collection;
	}

	/**
	 * Removes all given elements from the given other collection.
	 * 
	 * @param toRemove
	 * @param cartridges
	 * @return
	 */
	public static <C extends Collection<E>, E> C removeAll(C toRemove, C cartridges) {
		cartridges.removeAll(toRemove);
		return cartridges;
	}

	public static <T> T getFirstElement(Collection<T> collection) {
		if (collection == null
				|| collection.isEmpty()) {
			return null;
		}
		
		return collection.iterator().next();
	}
	
	public static <E> List<E> toList(E[] elements) {
		List<E> list = new ArrayList<E>(elements.length);
		for (E element : elements) {
			list.add(element);
		}
		return list;
	}
	
}
