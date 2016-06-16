/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import com.openshift.restclient.model.IProject;

/**
 * Wrapper for projects with late loading support.
 * @author Thomas Mäder
 *
 */
public interface IProjectWrapper extends IResourceWrapper<IProject, ConnectionWrapper>, IResourceContainer<IProject, ConnectionWrapper> {
	/**
	 * @return the state the project is in.
	 */
	LoadingState getState();

	/**
	 * Start loading the resources in this project from Openshift. When the
	 * loading is complete, a change notification will be sent. Loading will
	 * only start when no attempt at loading has been made (i.e. not failed, not
	 * in loading, etc.)
	 * 
	 * @param handler a callback where errors will be reported. May be called from an arbitrary thread.
	 * @return whether a load job has been started.
	 */
	boolean load(IExceptionHandler handler);
}
