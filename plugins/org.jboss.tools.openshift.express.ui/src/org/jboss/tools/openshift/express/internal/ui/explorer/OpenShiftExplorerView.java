/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;

/**
 * @author Xavier Coulon
 */
public class OpenShiftExplorerView extends CommonNavigator implements IConnectionsModelListener {

	@Override
	protected Object getInitialInput() {
		return ConnectionsModelSingleton.getInstance();
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aparent) {
		CommonViewer viewer = super.createCommonViewer(aparent);
		ConnectionsModelSingleton.getInstance().addListener(this);
		return viewer;
	}

	@Override
	public void dispose() {
		ConnectionsModelSingleton.getInstance().removeListener(this);
		super.dispose();
	}

	public void refreshViewer() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CommonViewer viewer = getCommonViewer();
				if (DisposeUtils.isDisposed(viewer)) { 
					return;
				}
				viewer.refresh();
			}
		});
	}

	public void refreshViewer(final Connection connection) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CommonViewer viewer = getCommonViewer();
				if (DisposeUtils.isDisposed(viewer)) {
					return;
				}
				viewer.refresh(connection);
			}
		});
	}

	@Override
	public void connectionAdded(Connection connection) {
		refreshViewer();
	}

	@Override
	public void connectionRemoved(Connection connection) {
		refreshViewer();
	}

	@Override
	public void connectionChanged(Connection connection) {
		refreshViewer(connection);
	}
}
