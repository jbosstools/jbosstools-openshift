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
package org.jboss.tools.openshift.common.core.connection;

/**
 * A factory that can create connections
 * 
 * @author Andre Dietisheim
 */
public interface IConnectionUI {

	public <T extends IConnection> boolean edits(T connection);
	
	/**
	 * Returns <code>true</code> if the connections handled in this UI can connect to the given host (url)
	 * 
	 * @param host
	 * @return
	 */
	public boolean canConnect(String host);

	public IConnection create();
	
}
