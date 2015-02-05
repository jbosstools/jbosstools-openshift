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
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractDetailViews.IDetailView;

/**
 * A factory that can create connections
 * 
 * @author Andre Dietisheim
 */
public interface IConnectionUI<T extends IConnection> extends IDetailView {
	
	/**
	 * Edits the given connection.
	 * 
	 * @param connection
	 * @return
	 */
	public void updateConnection(T connection);

}
