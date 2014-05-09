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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * @author Andre Dietisheim
 */
public class ProjectUtils {

	public static boolean exists(IProject project) {
		return project != null
				&& project.exists();
	}

	public static boolean exists(String name) {
		return exists(ResourcesPlugin.getWorkspace().getRoot().getProject(name));
	}

	public static boolean isAccessible(IProject project) {
		return project != null
				&& project.isAccessible();
	}
	
	public static String[] getAllOpenedProjects() {
		List<String> projects = new ArrayList<String>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (project.exists() 
					&& project.isOpen()) {
				projects.add(project.getName());
			}
		}
		return projects.toArray(new String[projects.size()]);
	}

}
