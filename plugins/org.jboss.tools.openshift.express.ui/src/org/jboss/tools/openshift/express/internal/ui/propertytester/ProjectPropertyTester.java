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
package org.jboss.tools.openshift.express.internal.ui.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;

/**
 * Property tester for properties that are related to the OpenShift server
 * adapter
 * 
 * @author Andre Dietisheim
 *
 */
public class ProjectPropertyTester extends PropertyTester {

	private static final String PROPERTY_IF_SHARED_IS_EGIT = "ifSharedIsEGit";
	private static final String PROPERTY_IS_INTERNAL_RSE = "isInternalRSE";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (PROPERTY_IF_SHARED_IS_EGIT.equals(property)) {
			return isIfSharedIsEGit(receiver, args, expectedValue);
		} else if (PROPERTY_IS_INTERNAL_RSE.equals(property)) {
			return isInternalRSE(receiver, args, expectedValue);
		}
		return false;
	}

	private boolean isIfSharedIsEGit(Object receiver, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IProject)
				|| !(expectedValue instanceof Boolean)) {
			return false;
		}

		IProject project = (IProject) receiver;
		Boolean expectedBoolean = (Boolean) expectedValue;
		if (EGitUtils.isShared(project)) {
			return expectedBoolean.equals(EGitUtils.isSharedWithGit(project));
		} else {
			return true;
		}
	}

	private boolean isInternalRSE(Object receiver, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IProject)
				|| !(expectedValue instanceof Boolean)) {
			return false;
		}
		return ((Boolean) expectedValue).equals(ProjectUtils.isInternalRSE(((IProject) receiver).getName()));
	}
}
