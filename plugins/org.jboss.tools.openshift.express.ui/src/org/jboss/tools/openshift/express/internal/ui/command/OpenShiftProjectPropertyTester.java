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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftProjectUtils;

/**
 * Property tester used to compare the given Server's serverTypeId with a given value. 
 * This property tester is configured in the plugin.xml file and used to display extra menus item only when the server is on OpenShift one.
 * 
 * @author Andre Dietisheim
 *
 */
public class OpenShiftProjectPropertyTester extends PropertyTester {

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if(!(receiver instanceof IProject)) {
			return false;
		}
		
		return OpenShiftProjectUtils.isOpenShiftProject((IProject) receiver);
	}
}
