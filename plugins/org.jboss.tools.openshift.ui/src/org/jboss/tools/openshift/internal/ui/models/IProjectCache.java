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

import java.util.Collection;

import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

public interface IProjectCache {

	Collection<IProjectAdapter> getProjectsFor(IOpenShiftConnection conn);

	void flushFor(IOpenShiftConnection conn);

	void addListener(IProjectCacheListener listener);
	
	void removeListener(IProjectCacheListener listener);

	static interface IProjectCacheListener {
		
		void handleAddToCache(IProjectCache cache, IProjectAdapter adapter);
		void handleRemoveFromCache(IProjectCache cache, IProjectAdapter adapter);
		void handleUpdateToCache(IProjectCache cache, IProjectAdapter adapter);
		
	}
}
