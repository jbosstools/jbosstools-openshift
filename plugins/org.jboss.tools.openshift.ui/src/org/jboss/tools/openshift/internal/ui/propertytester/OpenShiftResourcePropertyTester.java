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
package org.jboss.tools.openshift.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

/**
 * Property tester to determine if a resource is being deleted.
 * @author jeff.cantrill
 * @author Jeff Maury
 *
 */
public class OpenShiftResourcePropertyTester extends PropertyTester {

	private static final String PROPERTY_IS_DELETING = "isDeleting";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (PROPERTY_IS_DELETING.equals(property) && receiver instanceof IResourceUIModel && expectedValue instanceof Boolean) {
		    IResourceUIModel adapter = (IResourceUIModel) receiver;
			Boolean expected = (Boolean) expectedValue;
			return expected.equals(adapter.isDeleting());
		}
		return false;
	}
}
