/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftSpringBootApplicationProfileDetector implements IOpenshiftServerAdapterProfileDetector {
	
	private static final String COMPONENT_OF_SPRINGBOOT_APP = "spring-boot-starter";
	public static final String PROFILE = "openshift3.springboot";

	@Override
	public String getProfile() {
		return PROFILE;
	}

	@Override
	public boolean detect(IConnection connection, IResource resource, IProject eclipseProject) {
		//would it be better to search for the OpenSHift image label?
		return ProjectUtils.isAccessible(eclipseProject)&& hasSpringBootDependency(eclipseProject);
	}

	private boolean hasSpringBootDependency(IProject eclipseProject) {
		try {
			if (eclipseProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(eclipseProject);
				IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
				return Stream.of(classpath)
						.filter(classpathEntry -> classpathEntry.getPath().lastSegment().contains(COMPONENT_OF_SPRINGBOOT_APP))
						.findAny()
						.isPresent();
				
			}
		} catch (CoreException e) {
			OpenShiftCoreActivator.logError("Cannot determine if the project is a SpringBoot starter one.", e);//$NON-NLS-0$
		}
		return false;
	}

}
