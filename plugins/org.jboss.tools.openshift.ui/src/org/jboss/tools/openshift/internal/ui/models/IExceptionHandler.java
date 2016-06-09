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

/**
 * Callback interface for handling exceptions in asynchronous operations. The
 * implementer of this interface is responsible for doing any handling/logging,
 * etc. 
 * 
 * @author Thomas Mäder
 */
public interface IExceptionHandler {
	public static IExceptionHandler NULL_HANDLER = new IExceptionHandler() {

		@Override
		public void handleException(Throwable e) {
			// do nothing
		}
	};

	/**
	 * Handle the exception. May be called from an arbitrary thread.
	 * @param e the exception that occcured.
	 */
	void handleException(Throwable e);
}
