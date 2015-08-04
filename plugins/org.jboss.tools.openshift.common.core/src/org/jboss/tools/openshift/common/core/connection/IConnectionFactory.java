/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

/**
 * @author Andre Dietisheim
 */
public interface IConnectionFactory {

	/**
	 * Returns the human readable name of this factory.
	 *  
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns the unique id of this factory.
	 * 
	 * @return
	 */
	public String getId();

	/**
	 * Creates a connection for the given host. Returns <code>null</code> otherwise. The connection is not authorized yet.
	 * 
	 * @param url
	 * @return
	 */
	public IConnection create(String host);
	
	/**
	 * Returns the default host for this factory. 
	 * 
	 * @return
	 */
	public String getDefaultHost();

	boolean hasDefaultHost();

	public String getSignupUrl(String host);
	
	/**
	 * Returns <code>true</code> if this factory can create a connection of the given type.
	 * @param connection
	 * @return
	 */
	public <T extends IConnection> boolean canCreate(Class<T> clazz);
}
