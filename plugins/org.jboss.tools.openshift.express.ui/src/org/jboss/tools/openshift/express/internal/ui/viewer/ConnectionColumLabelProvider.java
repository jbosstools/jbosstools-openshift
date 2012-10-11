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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author Andre Dietisheim
 */
public class ConnectionColumLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
		if (!(element instanceof Connection)) {
			return super.getText(element);
		}
		Connection connection = (Connection) element;
		return connection.getUsername() + " - " + connection.getHost();
	}

}
