/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.propertytable;

import java.util.ArrayList;
import java.util.List;

public class ContainerElement implements IProperty {

		private final String property;
		private final List<Object> children;
		private final ContainerElement parent;

		public ContainerElement(String property) {
			this(property, null);
		}

		public ContainerElement(String property, ContainerElement parent) {
			this.property = property;
			this.children = new ArrayList<Object>();
			this.parent = parent;
		}

		public final String getProperty() {
			return property;
		}

		public final void add(StringElement child) {
			children.add(child);
		}

		public final void add(ContainerElement child) {
			children.add(child);
		}

		public final Object[] getChildren() {
			return children.toArray();
		}

		public boolean hasChildren() {
			return !children.isEmpty();
		}

		public final ContainerElement getParent() {
			return parent;
		}

		public boolean isLink() {
			return false;
		}

		@Override
		public String getName() {
			return property;
		}

		@Override
		public String getValue() {
			return null;
		}
	}