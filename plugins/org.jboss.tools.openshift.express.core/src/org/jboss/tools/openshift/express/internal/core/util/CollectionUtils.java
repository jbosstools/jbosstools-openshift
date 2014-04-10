/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Andre Dietisheim
 */
public class CollectionUtils {

	public static <T> List<T> add(T cartridge, List<T> cartridges) {
		cartridges.add(cartridge);
		return cartridges;
	}
	
	public static <T> List<T> removeAll(Collection<T> toRemove, List<T> cartridges) {
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
