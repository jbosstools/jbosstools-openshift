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
import org.jboss.tools.openshift.express.internal.core.console.IUserModelListener;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;

public class ExpressConsoleView extends CommonNavigator implements IUserModelListener {
	protected Object getInitialInput() {
		return UserModel.getDefault();
	}

	protected CommonViewer createCommonViewer(Composite aParent) {
		CommonViewer v = super.createCommonViewer(aParent);
		UserModel.getDefault().addListener(this);
		return v;
	}

	public void dispose() {
		UserModel.getDefault().removeListener(this);
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

	public void userAdded(UserDelegate user) {
		refreshViewer();
	}

	public void userRemoved(UserDelegate user) {
		refreshViewer();
	}

	public void userChanged(UserDelegate user) {
		refreshViewer();
	}
}
