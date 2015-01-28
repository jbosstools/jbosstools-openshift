/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.ICredentialsConnection;

/**
 * @author Andre Dietisheim
 */
public class ConnectionColumLabelProvider extends ColumnLabelProvider {

	private static final String DEFAULT_MARKER = " (default)";

	@Override
	public String getText(Object element) {
		if (!(element instanceof IConnection)) {
			return element.toString();
		}
		
		return createLabel((IConnection) element);
	}

	private String createLabel(IConnection connection) {
		StringBuilder builder = new StringBuilder();
		if (ICredentialsConnection.class.isAssignableFrom(connection.getClass())) {
			builder.append(((ICredentialsConnection) connection).getUsername()).append(" - ");
		}
		builder.append(connection.getHost());
		if (connection.isDefaultHost()) {
			builder.append(DEFAULT_MARKER);
		}
		return builder.toString();
	}

}
