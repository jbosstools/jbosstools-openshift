/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core.exception;

public abstract class OpenshiftIOException extends RuntimeException {

	private static final long serialVersionUID = -66495414279364584L;

	public OpenshiftIOException() {
	}

	public OpenshiftIOException(String message) {
		super(message);
	}

	public OpenshiftIOException(Throwable cause) {
		super(cause);
	}

	public OpenshiftIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenshiftIOException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
