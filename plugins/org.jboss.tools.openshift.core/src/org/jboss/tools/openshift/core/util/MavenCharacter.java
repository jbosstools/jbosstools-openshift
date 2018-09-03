/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.IMavenConstants;

public class MavenCharacter {

	private IProject project;

	public MavenCharacter(IProject project) {
		this.project = project;
	}

	/**
	 * Returns {@code true} if the given project is a maven project. This is if it
	 * has the maven nature and an accessible pom.
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public boolean hasNature() throws CoreException {
		if (project == null) {
			return false;
		}
		return project.hasNature(IMavenConstants.NATURE_ID) 
				&& hasPom();
	}

	/**
	 * Returns {@code true} if the given project has an accessible pom.
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public boolean hasPom() {
		if (project == null) {
			return false;
		}
		IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
		return pom != null
				&& pom.isAccessible();
	}

}
