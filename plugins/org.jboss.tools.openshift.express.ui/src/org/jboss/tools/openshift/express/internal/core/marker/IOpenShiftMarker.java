/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.marker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Andre Dietisheim
 */
public interface IOpenShiftMarker {

	/**
	 * Adds this marker to the given project. Returns the new marker if it was
	 * created, <code>null</code> otherwise.
	 * 
	 * @param project the project to add the marker to
	 * @param monitor the monitor to report progress to
	 * @return the marker file that was created, null otherwise
	 * @throws CoreException
	 */
	public IFile addTo(IProject project, IProgressMonitor monitor) throws CoreException;
}
