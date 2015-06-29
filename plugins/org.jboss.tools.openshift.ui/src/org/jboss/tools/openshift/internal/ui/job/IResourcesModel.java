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
package org.jboss.tools.openshift.internal.ui.job;

import java.util.Collection;

import com.openshift.restclient.model.IResource;

/**
 * A contract for handling resources
 * @author jeff.cantrill
 *
 */
public interface IResourcesModel {

	/**
	 * Get the resources
	 * @return the {@link java.util.Collection} of resources
	 */
	Collection<IResource> getResources();
}
