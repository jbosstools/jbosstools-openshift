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
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;

/**
 * @author Andre Dietisheim
 */
public class DataBindingUtils {

	/**
	 * Adds the given status providers to the given data binding context.
	 * 
	 * @param providers the providers to add
	 * @param dbc the context to add to
	 * 
	 * @see ValidationStatusProvider
	 * @see DataBindingContext
	 */
	public static void addValidationStatusProviders(Collection<ValidationStatusProvider> providers, DataBindingContext dbc) {
		for (ValidationStatusProvider provider: new ArrayList<ValidationStatusProvider>(providers)) {
			dbc.addValidationStatusProvider(provider);
		}
	}

	/**
	 * Removes the given status providers from the given data binding context.
	 * 
	 * @param providers the providers to remove
	 * @param dbc the context to remove from
	 * 
	 * @see ValidationStatusProvider
	 * @see DataBindingContext
	 */
	public static void removeValidationStatusProviders(Collection<ValidationStatusProvider> providers, DataBindingContext dbc) {
		for (ValidationStatusProvider provider: new ArrayList<ValidationStatusProvider>(providers)) {
			dbc.removeValidationStatusProvider(provider);
		}
	}
	
	public static void dispose(List<ValidationStatusProvider> providers) {
		for (ValidationStatusProvider provider : providers) {
			dispose(provider);
		}
	}

	public static boolean isDisposed(ValidationStatusProvider provider) {
		return provider == null
				|| provider.isDisposed();
	}

	public static void dispose(ValidationStatusProvider provider) {
		if (isDisposed(provider)) {
			return;
		}
		
		provider.dispose();
	}
	
	public static void dispose(DataBindingContext dbc) {
		if (dbc != null) {
			dbc.dispose();
		}
	}

}
