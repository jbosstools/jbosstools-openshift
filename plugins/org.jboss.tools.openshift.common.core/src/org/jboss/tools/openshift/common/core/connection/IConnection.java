/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import java.io.IOException;


/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public interface IConnection {

	public boolean connect();

	public String getHost();
	
	String getUsername();

	public boolean isDefaultHost();

	public String getScheme();
	
	public ConnectionType getType();

	public void refresh();

	public boolean canConnect() throws IOException;
}
