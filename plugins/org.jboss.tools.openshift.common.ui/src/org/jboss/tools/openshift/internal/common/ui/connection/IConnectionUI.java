/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * A factory that can create connections
 * 
 * @author Andre Dietisheim
 */
public interface IConnectionUI<T extends IConnection> {

	/**
	 * Returns <code>true</code> if this connection ui handles the given type
	 * 
	 * @param connection
	 * @return
	 */
	public boolean handles(Class<T> clazz);
	
	/**
	 * Returns <code>true</code> if the connections handled in this UI can connect to the given host (url)
	 * 
	 * @param host
	 * @return
	 */
	public boolean canConnect(String host);

	/**
	 * Creates a connection.
	 * 
	 * @return
	 */
	public T create();
	
	/**
	 * Edits the given connection.
	 * 
	 * @param connection
	 * @return
	 */
	public void edit(T connection);

}
