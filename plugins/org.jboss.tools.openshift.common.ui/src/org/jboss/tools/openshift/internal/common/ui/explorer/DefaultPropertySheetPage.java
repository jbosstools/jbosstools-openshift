/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.explorer;

import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;

public class DefaultPropertySheetPage extends PropertySheetPage {
	
	public static final String BASIC = "Basic";
	
	private static class DefaultPropertySheetSorter extends PropertySheetSorter {

		@Override
		public int compareCategories(String categoryA, String categoryB) {
			int result = categoryA.compareTo(categoryB);
			if (result != 0) {
				if (categoryA.equals(BASIC)) {
					result = -1;
				} else if (categoryB.equals(BASIC)) {
					result = 1;
				} else {
					result = categoryA.compareTo(categoryB);
				}
			}
			return result;
		}
	}

	private static final PropertySheetSorter SORTER = new DefaultPropertySheetSorter();

	public DefaultPropertySheetPage() {
		super();
		setSorter(SORTER);
	}
}
