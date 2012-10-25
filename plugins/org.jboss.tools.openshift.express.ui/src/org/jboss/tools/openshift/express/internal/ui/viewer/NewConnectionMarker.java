/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * A class that is used to have an entry <New Connection> in a combo view with
 * connections.
 * 
 * @author Andre Dietisheim
 * 
 */
public class NewConnectionMarker extends Connection {

	public NewConnectionMarker() {
		super("<New Connection>");
	}

	@Override
	public int hashCode() {
		return getUsername().hashCode();
	}
	
	public boolean equals(Object object) {
		return object instanceof NewConnectionMarker;
	}

}
