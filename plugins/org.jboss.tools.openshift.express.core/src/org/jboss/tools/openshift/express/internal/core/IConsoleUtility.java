/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.internal.core;

import java.io.OutputStream;

import org.eclipse.wst.server.core.IServer;

public interface IConsoleUtility {
	
	public void displayServerConsoleView(final IServer server);

	public OutputStream getServerConsoleOutputStream(IServer server);
	
	public void appendToServerConsole(IServer server, String msg);
}
