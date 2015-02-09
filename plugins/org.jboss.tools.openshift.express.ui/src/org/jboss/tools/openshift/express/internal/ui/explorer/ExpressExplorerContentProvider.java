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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * Content Provider for OpenShift v2 content
 */
public class ExpressExplorerContentProvider extends BaseExplorerContentProvider {

	/**
	 * Called to obtain the root elements of the tree viewer, the connections
	 */
	@Override
	public Object[] getExplorerElements(final Object parentElement) {
		if (parentElement instanceof ConnectionsRegistry) {
			ConnectionsRegistry registry = (ConnectionsRegistry) parentElement;
			return registry.getAll(ExpressConnection.class).toArray();
		} else if (parentElement instanceof ExpressConnection) {
			List<IDomain> domains = ((ExpressConnection) parentElement).getDomains();
			return domains.toArray(new IDomain[domains.size()]);
		} else {
			return new Object[0];
		}
	}

	/**
	 * Called to obtain the children of any element in the tree viewer, ie, from
	 * a connection or an application
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ExpressConnection) {
			ExpressConnection connection = (ExpressConnection) parentElement;
			if (!connection.isConnected()
					&& !connection.canPromptForPassword()) {
				return new Object[] { new NotConnectedUserStub() };
			}
			return loadChildren(parentElement);
		} else if (parentElement instanceof IDomain) {
			return loadChildren(parentElement);
		} else if (parentElement instanceof IApplication) {
			return loadChildren(parentElement);
		} else {
			return getChildrenFor(parentElement);
		}
	}

	@Override
	protected Object[] getChildrenFor(Object parentElement) {
		Object[] children = new Object[0];
		try {
			if (parentElement instanceof OpenShiftExplorerContentCategory) {
				ExpressConnection user = ((OpenShiftExplorerContentCategory) parentElement).getUser();
				children = new Object[] { user };
			} else if (parentElement instanceof ExpressConnection) {
				final ExpressConnection connection = (ExpressConnection) parentElement;
				children = connection.getDomains().toArray();
			} else if (parentElement instanceof IDomain) {
				final IDomain domain = (IDomain) parentElement;
				children = domain.getApplications().toArray();
			} else if (parentElement instanceof IApplication) {
				children = ((IApplication) parentElement).getEmbeddedCartridges().toArray();
			}
		} catch (OpenShiftException e) {
			addException(parentElement, e);
		}

		return children;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ConnectionsRegistry
				|| element instanceof IConnection
				|| element instanceof IDomain
				|| element instanceof IApplication;
	}
}
