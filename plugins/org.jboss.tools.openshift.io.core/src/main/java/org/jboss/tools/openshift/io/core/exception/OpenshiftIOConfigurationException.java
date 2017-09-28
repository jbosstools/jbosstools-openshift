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

public class OpenshiftIOConfigurationException extends OpenshiftIOException {

	public OpenshiftIOConfigurationException() {
	}

	public OpenshiftIOConfigurationException(String message) {
		super(message);
	}

	public OpenshiftIOConfigurationException(Throwable cause) {
		super(cause);
	}

	public OpenshiftIOConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenshiftIOConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
