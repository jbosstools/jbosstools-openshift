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
package org.jboss.tools.openshift.internal.common.core.security;

/**
 * @author Xavier Coulon
 *
 */
public class SecureStoreException extends Exception {
	public static final String ID = "secureStoreException";

	/** generated serialVersionUID. */
	private static final long serialVersionUID = -1732042851833545771L;

	/**
	 * Full constructor
	 * @param message the message to print
	 * @param cause the underlying cause
	 */
	public SecureStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Full constructor
	 * @param message the message to print
	 */
	public SecureStoreException(String message) {
		super(message);
	}


}
