/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.debug;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public class DebugTrackerContributionEvaluation {
	private static final String ID = "org.jboss.tools.openshift.core.debugSessionTracker"; //$NON-NLS-0$

	public static void startDebugSession(IServer server, int port) throws CoreException {
		Collection<DebugSessionTracker> trackers = getTrackers();
		trackers.forEach(tracker -> {

			SafeRunner.run(new ISafeRunnable() {

				@Override
				public void run() throws Exception {
					tracker.startDebugSession(server, port);
				}

				@Override
				public void handleException(Throwable e) {
					OpenShiftCoreActivator.logError(e.getMessage(), e);
				}
			});
		});
	}

	public static void stopDebugSession(IServer server) {
		Collection<DebugSessionTracker> trackers = getTrackers();
		trackers.forEach(tracker -> {

			SafeRunner.run(new ISafeRunnable() {

				@Override
				public void run() throws Exception {
					tracker.stopDebugSession(server);
				}

				@Override
				public void handleException(Throwable e) {
					OpenShiftCoreActivator.logError(e.getMessage(), e);
				}
			});

		});
	}

	public static boolean isDebugSessionAlive(IServer server) {
		Collection<DebugSessionTracker> trackers = getTrackers();
		// There are no debug session tracker extensions
		if (trackers.isEmpty()) {
			return false;
		}

		for (DebugSessionTracker tracker : trackers) {
			if (!tracker.isDebugSessionAlive(server)) {
				return false;
			}
		}
		return true;
	}

	private static Collection<DebugSessionTracker> getTrackers() {
		IConfigurationElement[] elements = getConfigurationElements();
		Collection<DebugSessionTracker> trackers = new ArrayList<>();
		for (IConfigurationElement element : elements) {
			try {
				Object extension = element.createExecutableExtension("class"); //$NON-NLS-1$
				if (extension instanceof DebugSessionTracker) {
					trackers.add((DebugSessionTracker) extension);
				}
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		return trackers;
	}

	private static IConfigurationElement[] getConfigurationElements() {
		return Platform.getExtensionRegistry().getConfigurationElementsFor(ID);
	}
}
