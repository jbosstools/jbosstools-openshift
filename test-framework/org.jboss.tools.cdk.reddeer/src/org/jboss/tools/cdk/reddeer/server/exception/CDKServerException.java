/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.exception;

import org.jboss.reddeer.common.exception.RedDeerException;

/**
 * Exception class for CDK server failures
 * @author odockal
 *
 */
public class CDKServerException extends RedDeerException {

	/**
	 * Generated UID
	 */
	private static final long serialVersionUID = -1484606432928003515L;

	/**
	 * Instantiates a new cdk server wizard exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public CDKServerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new cdk server wizard exception.
	 *
	 * @param message the message
	 */
	public CDKServerException(String message) {
		super(message);
	}

}
