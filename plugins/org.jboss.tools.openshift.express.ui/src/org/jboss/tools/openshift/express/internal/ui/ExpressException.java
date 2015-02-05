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
package org.jboss.tools.openshift.express.internal.ui;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class ExpressException extends OpenShiftException {

	private static final long serialVersionUID = 1L;

	public ExpressException(String message, Object... arguments) {
		super(message, arguments);
	}

	public ExpressException(Throwable cause, String message, Object... arguments) {
		super(cause, message, arguments);
	}

}
