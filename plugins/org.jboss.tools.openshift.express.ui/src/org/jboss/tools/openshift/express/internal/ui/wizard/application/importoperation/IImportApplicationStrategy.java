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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author André Dietisheim
 */
public interface IImportApplicationStrategy {

	/**
	 * Executes this import operation and returns the projects that were
	 * imported.
	 * 
	 * @param monitor the monitor to report progress to
	 * @return the list of projects that were imported to the workspace.
	 * @throws Exception
	 */
	public IProject execute(IProgressMonitor monitor) throws Exception;

}
