/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
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

public class DiffUtils {

	private DiffUtils() {
		// inhibit instantiation
	}
	
	public static <T> Diff<T> getDiff(Collection<T> source, Collection<T> target) {
		return new Diff<T>(source, target);
	}
	
	public static <T> Collection<T> getAdditions(Collection<T> source, Collection<T> target) {
		List<T> addedElements = new ArrayList<T>();
		for(T element : target) {
			if (!source.contains(element)) {
				addedElements.add(element);
			}
		}
		return addedElements;
	}

	public static <T> Collection<T> getRemovals(Collection<T> source, Collection<T> target) {
		List<T> removedElement = new ArrayList<T>();
		for(T element : source) {
			if (!target.contains(element)) {
				removedElement.add(element);
			}
		}
		return removedElement;
	}

	public static class Diff<T> {

		private Collection<T> additions;
		private Collection<T> removals;

		public Diff(Collection<T> source, Collection<T> target) {
			this.additions = DiffUtils.getAdditions(source, target);
			this.removals = DiffUtils.getRemovals(source, target);
		}

		public Collection<T> getAdditions() {
			return additions;
		}

		public Collection<T> getRemovals() {
			return removals;
		}
	}
}
