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
 * A UI that can display/edit a given connection instance. All implementors are
 * able to display different types of connections and are displayed by
 * {@link ConnectionUIViews}
 * 
 * @author Andre Dietisheim
 * 
 * @see IConnection
 * @see ConnectionUIViews
 * @see IDetailView#isViewFor(Object)
 */
public interface IConnectionUI<T extends IConnection> extends IDetailView {

}
