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
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

/**
 * @author Aurélien Pupier
 */
public class OpenShiftSpringBootProfileDetector implements IOpenshiftServerAdapterProfileDetector {

	private static final String COMPONENT_OF_SPRINGBOOT_APP = "spring-boot-starter";
	public static final String PROFILE = "openshift3.springboot";

	@Override
	public String getProfile() {
		return PROFILE;
	}

	@Override
	public boolean detect(IConnection connection, IResource resource, IProject eclipseProject) {
		return ProjectUtils.isAccessible(eclipseProject) && hasSpringBootDependency(eclipseProject);
	}

	private boolean hasSpringBootDependency(IProject eclipseProject) {
		boolean hasSpringBootDependency = false;
		try {
			if (eclipseProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(eclipseProject);
				IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
				hasSpringBootDependency = Stream.of(classpath).anyMatch(
						classpathEntry -> classpathEntry.getPath().lastSegment().contains(COMPONENT_OF_SPRINGBOOT_APP));
			}
		} catch (CoreException e) {
			OpenShiftCoreActivator.logError(NLS.bind("Cannot determine if the project {0} is a SpringBoot starter one.",
					eclipseProject.getName()), e);
		}
		return hasSpringBootDependency;
	}

}
