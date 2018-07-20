/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.io.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.openshift.internal.io.ui.dialog.BrowserBasedLoginDialog;
import org.jboss.tools.openshift.io.core.LoginProvider;
import org.jboss.tools.openshift.io.core.LoginResponse;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;

/**
 * Login provider that will launch a browser to perform the login and extract
 * the JSON.
 * 
 */
public class DefaultLoginProvider implements LoginProvider {

	private static final int TIMEOUT_JOB_ON_UI_THREAD = 10000;

	private class LoginJob extends UIJob {
		private boolean runninginUI = false;
		private boolean shouldRun = true;
		private ICluster cluster;
		private IAccount account;
		private LoginResponse response;

		public LoginJob(ICluster cluster, IAccount account) {
			super("Logging to OpenShift.io");
			this.cluster = cluster;
			this.account = account;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (shouldRun && !monitor.isCanceled()) {
				runninginUI = true;
				response = loginInUI(cluster, account);
				return new Status(IStatus.OK, OpenShiftIOUIActivator.PLUGIN_ID, null);
			}
			return new Status(IStatus.CANCEL, OpenShiftIOUIActivator.PLUGIN_ID, null);
		}

		public boolean isRunninginUI() {
			return runninginUI;
		}

		public void setShouldRun(boolean shouldRun) {
			this.shouldRun = shouldRun;
		}

		public LoginResponse getResponse() {
			return response;
		}
	}

	@Override
	public LoginResponse login(ICluster cluster, IAccount account) {
		if (null == Display.getCurrent()) {
			return runInJob(cluster, account);
		} else {
			return loginInUI(cluster, account);
		}
	}

	LoginResponse runInJob(ICluster cluster, IAccount account) {
		LoginJob job = new LoginJob(cluster, account);
		job.schedule();
		IProgressMonitor monitor = new NullProgressMonitor();
		try {
			if (job.join(TIMEOUT_JOB_ON_UI_THREAD, monitor)) {
				return job.getResponse();
			} else {
				throw new InterruptedException();
			}
		} catch (OperationCanceledException e) {
			return null;
		} catch (InterruptedException e) {
			if (job.isRunninginUI()) {
				try {
					job.join();
					return job.getResponse();
				} catch (InterruptedException e1) {
					return null;
				}
			} else {
				job.setShouldRun(false);
				monitor.setCanceled(true);
				job.cancel();
				return null;
			}
		}
	}

	public LoginResponse loginInUI(ICluster cluster, IAccount account) {
		LoginResponse response = null;

		BrowserBasedLoginDialog dialog = new BrowserBasedLoginDialog(Display.getCurrent().getActiveShell(),
				cluster.getLoginURL(), cluster.getLandingURL());
		if (dialog.open() == Window.OK) {
			response = dialog.getInfo();
		}
		return response;
	}
}
