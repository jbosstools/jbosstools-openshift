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
package org.jboss.tools.openshift.express.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * This class is used to interact with EGit UI
 * 
 * Since the EGit UI is a UI construct, no direct references to the console
 * classes may be done in a core plugin. Instead, the UI plugin should set a
 * proper console handler in the {@link OpenshiftCoreUIIntegration} class.
 */
public interface IEGitUI {

	/**
	 * Opens the EGit Commit dialog and performs the commit for the given
	 * project unless the users cancels it. The commit is performed in a
	 * background job and thus operations that rely on commit completion can be
	 * executed in the given job change listener.
	 * 
	 * @param project
	 * @param remote 
	 * @param commitJobListener
	 * @throws CoreException
	 */
	public void commitWithUI(IProject project, String remote, String applicationName, Runnable runnable) throws CoreException;

}
