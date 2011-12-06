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
package org.jboss.tools.openshift.express.internal.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

/**
 * @author André Dietisheim
 */
public class JavaProjectUtils {

	public static boolean isJavaProject(IProject project) {
		try {
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				return false;
			}
		} catch (CoreException e) {
			// project is not opened, does not exist, etc.
			return false;
		}
		return true;
	}

}
