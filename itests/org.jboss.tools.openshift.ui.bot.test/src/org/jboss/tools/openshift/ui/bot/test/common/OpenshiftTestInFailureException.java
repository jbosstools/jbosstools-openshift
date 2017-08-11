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
package org.jboss.tools.openshift.ui.bot.test.common;

public class OpenshiftTestInFailureException extends RuntimeException {
	
	public OpenshiftTestInFailureException(String message) {
		super(message);
	}
	
	public OpenshiftTestInFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
