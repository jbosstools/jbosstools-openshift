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
package org.jboss.tools.openshift.express.core;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCoreException extends OpenShiftException {

	private static final long serialVersionUID = 1L;

	public OpenShiftCoreException(String message, Object... arguments) {
		super(message, arguments);
	}

	public OpenShiftCoreException(Throwable cause, String message, Object... arguments) {
		super(cause, message, arguments);
	}

}
