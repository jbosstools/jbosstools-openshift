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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;

/**
 * Property tester for properties that are related to the OpenShift server
 * adapter
 * 
 * @author Xavier Coulon
 * @author Andre Dietisheim
 *
 */
public class ServerTypePropertyTester extends PropertyTester {

	private final static String PROPERTY_IS_OPENSHIFT_SERVER = "isOpenShiftServer";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (PROPERTY_IS_OPENSHIFT_SERVER.equals(property)) {
			return isOpenShiftServer(receiver, args, expectedValue);
		}
		return false;
	}

	private boolean isOpenShiftServer(Object receiver, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IServer)
				|| !(expectedValue instanceof Boolean)) {
			return false;
		}

		return ((Boolean) expectedValue).equals(
				OpenShiftServerUtils.isOpenShiftRuntime((IServer) receiver));
	}
}
