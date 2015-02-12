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
package org.jboss.tools.openshift.express.core;

import java.io.OutputStream;

import org.eclipse.wst.server.core.IServer;

/**
 * This class is used to send information to an eclipse console, 
 * if one is available, for a given server. 
 * 
 * Since the eclipse console is a UI construct, no direct
 * references to the console classes may be done in a core plugin. 
 * Instead, the UI plugin should set a proper console handler
 * in the {@link ExpressCoreUIIntegration} class. 
 */
public interface IConsoleUtility {
	
	/**
	 * Open and bring to the front the console 
	 * for the given server. 
	 * @param server
	 */
	public void displayServerConsoleView(final IServer server);

	/**
	 * Return an outputstream which can be used to print 
	 * to the console for the given server. 
	 * 
	 * @param server
	 * @return
	 */
	public OutputStream getServerConsoleOutputStream(IServer server);
	
	/**
	 * Append the given text to the console for the given server. 
	 * This is a convenience method and is functionally equivilent to 
	 * getServerConsoleOutputStream(server).write(msg.getBytes());
	 * 
	 * @param server
	 * @param msg
	 */
	public void appendToServerConsole(IServer server, String msg);
}
