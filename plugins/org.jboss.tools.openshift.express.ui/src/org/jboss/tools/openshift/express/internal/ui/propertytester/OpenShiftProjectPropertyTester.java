/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftProjectUtils;

/**
 * Property tester used to compare the given Server's serverTypeId with a given
 * value. This property tester is configured in the plugin.xml file and used to
 * display extra menus item only when the server is on OpenShift one.
 * 
 * @author Andre Dietisheim
 *
 */
public class OpenShiftProjectPropertyTester extends PropertyTester {

	private final static String PROPERTY_IS_OPENSHIFT_PROJECT = "isOpenShiftProject";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (PROPERTY_IS_OPENSHIFT_PROJECT.equals(property)) {
			return isOpenShiftProject(receiver, args, expectedValue);
		}
		return false;
	}

	private boolean isOpenShiftProject(Object receiver, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IProject)
				|| !(expectedValue instanceof Boolean)) {
			return false;
		}
		return ((Boolean) expectedValue).equals(
				OpenShiftProjectUtils.isOpenShiftProject((IProject) receiver));
	}
}
