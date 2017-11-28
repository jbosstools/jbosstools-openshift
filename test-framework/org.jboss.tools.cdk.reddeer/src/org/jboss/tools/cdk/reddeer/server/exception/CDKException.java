/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.exception;

import org.eclipse.reddeer.common.exception.RedDeerException;

/**
 * General CDK exception
 * @author odockal
 *
 */
public class CDKException extends RedDeerException {
		
	/**
	 * Generated UID
	 */
	private static final long serialVersionUID = 4614750495162802272L;

	/**
	 * Instantiates a new cdk server wizard exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public CDKException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * Instantiates a new cdk server wizard exception.
	 *
	 * @param message the message
	 */	
	public CDKException(String message) {
		super(message);
	}	
}
