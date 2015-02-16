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
 * A class that is used to have an entry "<Automatic>" in a combo that
 * shows all connection factories.
 * 
 * @author Andre Dietisheim
 * 
 */
public class AutomaticConnectionFactoryMarker implements IConnectionFactory {

	private static final AutomaticConnectionFactoryMarker INSTANCE = new AutomaticConnectionFactoryMarker();
	
	public static AutomaticConnectionFactoryMarker getInstance() {
		return INSTANCE;
	}
	
	private AutomaticConnectionFactoryMarker() {
	}
	
	@Override
	public String getName() {
		return "<Automatic>";
	}

	@Override
	public String getId() {
		return "org.jboss.tools.openshift.connectionfactory.automatic";
	}

	@Override
	public IConnection create(String host) {
		return null;
	}

	@Override
	public String getDefaultHost() {
		return null;
	}

	@Override
	public boolean hasDefaultHost() {
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof AutomaticConnectionFactoryMarker;
	}

	@Override
	public <T extends Class<? extends IConnection>> boolean canCreate(T clazz) {
		return false;
	}

}
