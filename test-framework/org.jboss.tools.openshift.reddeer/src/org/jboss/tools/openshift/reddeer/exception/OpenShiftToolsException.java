/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.exception;

/**
 * 
 * Exception for handling exceptional situations in OpenShift tests.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShiftToolsException extends RuntimeException {

	private static final long serialVersionUID = 13L;

	public OpenShiftToolsException() {
		super();
	}

	public OpenShiftToolsException(Throwable t) {
		super(t);
	}

	public OpenShiftToolsException(String msg) {
		super(msg);
	}
}
