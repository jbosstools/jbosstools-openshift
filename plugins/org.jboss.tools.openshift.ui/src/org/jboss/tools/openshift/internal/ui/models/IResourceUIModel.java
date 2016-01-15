/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import com.openshift.restclient.model.IResource;

/**
 * UI Model to wrapper an OpenShift resource
 * @author jeff.cantrill
 *
 */
public interface IResourceUIModel extends IAncestorable {

	/**
	 * Get the underlying resource for this display model.
	 * This will throw a cast exception if it can not be cast
	 * @return
	 */
	IResource getResource();
	
}
