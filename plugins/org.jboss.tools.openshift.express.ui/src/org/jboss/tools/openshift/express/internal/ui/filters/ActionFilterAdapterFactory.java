/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.filters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

/**
 * IAdapterFactory to provide simple property filtering for items (e.g. for
 * use with decorators).
 * 
 * This adapter must be registered with the org.eclipse.runtime.adapters
 *
 */
public class ActionFilterAdapterFactory implements IAdapterFactory{

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if(adapterType != IActionFilter.class){
			return null;
		}
		return new SimplePropertyActionFilter();
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class [] {IActionFilter.class};
	}

}
