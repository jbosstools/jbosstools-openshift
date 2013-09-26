/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
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
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractThreadedOperation {
	
	private static final int THREAD_SLEEP = 1 * 1000;
	
	private ExecutorService executor = Executors.newFixedThreadPool(1);

	protected AbstractThreadedOperation() {
	}

	protected <E> E submit(Callable<E> callable, IProgressMonitor monitor) throws OpenShiftException {
		try {
			Future<E> future = executor.submit(callable);
			while (!future.isDone()) {
				if (monitor.isCanceled()) {
					throw new OpenShiftException(OpenShiftExpressCoreMessages.OPERATION_CANCELLED);
				}
				Thread.sleep(THREAD_SLEEP);
			}
			return future.get();
		} catch (Exception e) { // InterruptedException and ExecutionException
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			OpenShiftCoreActivator.pluginLog().logError("Failed to create application", cause);
			throw new OpenShiftException("Failed to create application: {0}", cause.getMessage());
		} finally {
			executor.shutdown();
		}
	}
}
