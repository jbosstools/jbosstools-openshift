/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class CommandTimeoutException extends TimeoutException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 37067116146846743L;

	private static String getTimeoutError(List<String> output, List<String> err) {
		StringBuilder msg = new StringBuilder();
		msg.append("Process output:\n");
		output.forEach(line -> msg.append("   ").append(line));
		err.forEach(line -> msg.append("   ").append(line));
		return msg.toString();
	}

	private List<String> inLines;
	private List<String> errLines;

	public CommandTimeoutException(List<String> inLines, List<String> errLines) {
		super(getTimeoutError(inLines, errLines));
		this.inLines = inLines;
		this.errLines = errLines;
	}

	public List<String> getInLines() {
		return inLines;
	}

	public List<String> getErrLines() {
		return errLines;
	}

}
