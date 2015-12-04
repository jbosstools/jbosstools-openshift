/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * @author Andre Dietisheim
 */
public class ProjectUtils {

	private static final String RSE_INTERNAL_PROJECTS = "RemoteSystems";
	private static final String EXTERNAL_PLUGIN_LIBRARIES = "External Plug-in Libraries"; // "External Plug-in Libraries" 
	
	public static boolean exists(IProject project) {
		return project != null
				&& project.exists();
	}

	public static boolean exists(String name) {
		return exists(ResourcesPlugin.getWorkspace().getRoot().getProject(name));
	}

	
	public static IProject getProject(String name) {
		if (StringUtils.isEmptyOrNull(name)) {
			return null;
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return exists(project)?project:null;
	}
	
	public static boolean isAccessible(IProject project) {
		return project != null
				&& project.isAccessible();
	}

	public static List<IProject> getAllAccessibleProjects(boolean excludeInternalProjects) {
		return Arrays.stream(ResourcesPlugin.getWorkspace().getRoot().getProjects())
					 .filter((IProject project) -> { 
							if (isAccessible(project)) {
								if (!excludeInternalProjects) {
									return true;
								} else {
									return !(isInternalRSE(project.getName())
											|| isInternalPde(project.getName()));

								}
							}
							return false;
						 }
					 )
					 .collect(Collectors.toList());
	}

	public static String[] getAllAccessibleProjectNames() {
		return getAllAccessibleProjectNames(true);
	}

	public static String[] getAllAccessibleProjectNames(boolean excludeInternalProjects) {
		return getAllAccessibleProjects(excludeInternalProjects)
				.stream()
				.map(IProject::getName)
				.toArray(String[]::new);
	}

	public static List<IProject> getAllAccessibleProjects() {
		return getAllAccessibleProjects(true);
	}
	
	/**
	 * Returns <code>true</code> if the given project name matches the name used
	 * for internal rse projects.
	 * 
	 * @param projectName
	 * @return
	 */
	public static boolean isInternalRSE(String projectName) {
		return projectName != null
				&& projectName.startsWith(RSE_INTERNAL_PROJECTS);
	}

	/**
	 * Returns <code>true</code> if the given project name matches the name used
	 * for the pde "External Plug-in Libraries" project.
	 * 
	 * @param projectName
	 * @return
	 */
	public static boolean isInternalPde(String projectName) {
		return projectName != null
				&& projectName.startsWith(EXTERNAL_PLUGIN_LIBRARIES);
	}

	public static String getName(IProject project) {
		if (isAccessible(project)) {
			return project.getName();
		} else {
			return null;
		}
	}
}
