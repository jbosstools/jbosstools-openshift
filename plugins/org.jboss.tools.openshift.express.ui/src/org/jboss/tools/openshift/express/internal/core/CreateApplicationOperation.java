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
package org.jboss.tools.openshift.express.internal.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftApplicationNotAvailableException;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class CreateApplicationOperation {

	private static final int APP_CREATION_TIMEOUT = 180;
	private UserDelegate user;

	public CreateApplicationOperation(UserDelegate user) {
		this.user = user;
	}

	public IApplication execute(final String name, final ICartridge cartridge,
			final ApplicationScale scale, final IGearProfile gearProfile, final IProgressMonitor monitor)
			throws OpenShiftApplicationNotAvailableException, OpenShiftException {
		if (user == null) {
			throw new OpenShiftException("Could not create application, have no valid user credentials");
		}
		ExecutorService executor = Executors.newFixedThreadPool(1);
		try {
			FutureTask<IApplication> future = new FutureTask<IApplication>(
					new Callable<IApplication>() {
						@Override
						public IApplication call() throws Exception {
							monitor.setTaskName("Creating application \"" + name + "\"...");
							Logger.debug("creating application...");
							final IApplication application =
									user.createApplication(name, cartridge, scale, gearProfile);
							monitor.beginTask(
									"Waiting for application to be reachable...", IProgressMonitor.UNKNOWN);
							Logger.debug("Waiting for application to be reachable...");
							waitForAccessible(application, monitor);
							return application;
						}
					});
			executor.execute(future);
			while (!future.isDone()) {
				if (monitor.isCanceled()) {
					throw new OpenShiftException("Operation was cancelled by user.");
				}
				Thread.sleep(1000);
			}
			final IApplication application = future.get();
			return application;
		} catch (Exception e) { // InterruptedException and ExecutionException
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			Logger.error("Failed to create application", cause);
			throw new OpenShiftException("Failed to create application: {0}", cause.getMessage());
		} finally {
			executor.shutdown();
		}
	}

	private void waitForAccessible(IApplication application, IProgressMonitor monitor)
			throws OpenShiftApplicationNotAvailableException, OpenShiftException {
		// monitor.subTask("waiting for application to become accessible...");
		if (!application.waitForAccessible(APP_CREATION_TIMEOUT * 1000)) {
			throw new OpenShiftApplicationNotAvailableException(NLS.bind(
					OpenShiftExpressUIMessages.HOSTNAME_NOT_ANSWERING, application.getApplicationUrl()));
		}
	}

}
