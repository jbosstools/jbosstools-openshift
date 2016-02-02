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
package org.jboss.tools.openshift.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.mockito.Mockito;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class ResourceMocks {

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz) {
		return createResources(numOf, clazz, null);
	}

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz, IResourceVisitor<R> visitor) {
		ArrayList<R> resources = new ArrayList<>(numOf);
		
		for (int i = 0; i< numOf; i++) {
			R mock = createResource(clazz, visitor);
			resources.add(mock);
		}
		return resources;
	}

	public static <R extends IResource> R createResource(Class<R> clazz, IResourceVisitor<R> visitor) {
		R mock = Mockito.mock(clazz);
		if (visitor != null) {
			visitor.visit(mock);
		}
		return mock;
	}

	public static List<ObservableTreeItem> createObservableTreeItems(Collection<? extends IResource> resources) {
		return resources.stream()
				.map(r -> new ObservableTreeItem(r))
				.collect(Collectors.toList());
	}
	
	public static interface IResourceVisitor<R extends IResource> {
		public void visit(R resource);
	}
	
}
