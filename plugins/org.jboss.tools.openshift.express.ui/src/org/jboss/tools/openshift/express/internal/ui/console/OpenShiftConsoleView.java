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
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.core.connection.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;

/**
 * @author Xavier Coulon
 */
public class OpenShiftConsoleView extends CommonNavigator implements IConnectionsModelListener {

	protected Object getInitialInput() {
		return ConnectionsModelSingleton.getInstance();
	}

	protected CommonViewer createCommonViewer(Composite aParent) {
		CommonViewer v = super.createCommonViewer(aParent);
		ConnectionsModelSingleton.getInstance().addListener(this);
		return v;
	}

	public void dispose() {
		ConnectionsModelSingleton.getInstance().removeListener(this);
		super.dispose();
	}

	public void refreshViewer() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (!DisposeUtils.isDisposed(getCommonViewer())) {
					getCommonViewer().refresh();
				}
			}
		});
	}

	public void connectionAdded(Connection user) {
		refreshViewer();
	}

	public void connectionRemoved(Connection user) {
		refreshViewer();
	}

	public void connectionChanged(Connection user) {
		refreshViewer();
	}
}
