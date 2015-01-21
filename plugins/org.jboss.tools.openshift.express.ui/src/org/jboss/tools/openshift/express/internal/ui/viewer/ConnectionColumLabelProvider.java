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
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

/**
 * @author Andre Dietisheim
 */
public class ConnectionColumLabelProvider extends ColumnLabelProvider {

	private static final String DEFAULT_MARKER = " (default)";
	private static final String LABEL_NEW_CONNECTION = "<New ExpressConnection>";

	@Override
	public String getText(Object element) {
		if (element instanceof NewConnectionMarker) {
			return LABEL_NEW_CONNECTION;
		}

		if (!(element instanceof ExpressConnection)) {
			return super.getText(element);
		}
		return createLabel((ExpressConnection) element);
	}

	private String createLabel(ExpressConnection connection) {
		StringBuilder builder =
				new StringBuilder(connection.getUsername()).append(" - ").append(connection.getHost());
		if (connection.isDefaultHost()) {
			builder.append(DEFAULT_MARKER);
		}
		return builder.toString();
	}

}
