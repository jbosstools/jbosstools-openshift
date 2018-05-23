/*******************************************************************************
 * Copyright (c) 2016-2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerConsoleWriter;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.OCBinaryOperation;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.capability.resources.IRSyncable;
import com.openshift.restclient.capability.resources.IRSyncable.LocalPeer;
import com.openshift.restclient.capability.resources.IRSyncable.Peer;
import com.openshift.restclient.capability.resources.IRSyncable.PodPeer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

public class RSync {

	private static final String POD_STATUS_RUNNING = "Running";

	private final IResource resource;
	private final String podPath;
	private final IServer server;
	private Connection connection;

	public RSync(final IResource resource, final String podPath, final IServer server) {
		this.resource = resource;
		this.podPath = sanitizeUnixPath(podPath);
		this.server = server;
		this.connection = OpenShiftServerUtils.getConnection(server);
	}

	public MultiStatus syncPodsToDirectory(File localFolder, final IServerConsoleWriter consoleWriter) {
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, IStatus.OK,
				NLS.bind("Could not sync all pods to folder {0}.", localFolder.getAbsolutePath()), null);
		new OCBinaryOperation() {
			@Override
			protected void runOCBinary() {
				List<IPod> pods = ResourceUtils.getPodsFor(resource,
						connection.getResources(ResourceKind.POD, resource.getNamespaceName()));
				for (IPod pod : pods) {
					try {
						syncPodToDirectory(pod, localFolder, podPath, consoleWriter);
					} catch (OpenShiftException e) {
						status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
					}
				}
			}
		}.run(connection);
		return status;
	}

	public MultiStatus syncDirectoryToPods(File localFolder, final IServerConsoleWriter consoleWriter) {
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, IStatus.OK,
				NLS.bind("Could not sync folder {0} to all pods.", localFolder.getAbsolutePath()), null);
		new OCBinaryOperation() {

			@Override
			protected void runOCBinary() {
				List<IPod> pods = ResourceUtils.getPodsFor(resource,
						connection.getResources(ResourceKind.POD, resource.getNamespaceName()));
				for (IPod pod : pods) {
					try {
						syncDirectoryToPod(pod, localFolder, podPath, consoleWriter);
					} catch (OpenShiftException e) {
						status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
					}
				}
			}
		}.run(connection);
		return status;
	}

	private void syncPodToDirectory(IPod pod, File localFolder, String podPath, final IServerConsoleWriter consoleWriter) {
		localFolder.mkdirs();
		sync(new PodPeer(podPath, pod), new LocalPeer(sanitizePath(localFolder.getAbsolutePath())), pod, consoleWriter);
	}

	private void syncDirectoryToPod(final IPod pod, final File localFolder, String podPath, final IServerConsoleWriter consoleWriter) {
		sync(new LocalPeer(sanitizePath(localFolder.getAbsolutePath())), new PodPeer(podPath, pod), pod, consoleWriter);
	}

	private void sync(final Peer source, final Peer destination, final IPod pod, final IServerConsoleWriter consoleWriter) {
		if (!POD_STATUS_RUNNING.equals(pod.getStatus())) {
			return;
		}
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				final InputStream syncStream = rsyncable.sync(source, destination, 
							IRSyncable.exclude(".git", ".npm"),
							IRSyncable.NO_PERMS,
							IBinaryCapability.SKIP_TLS_VERIFY);
				asyncWriteLogs(syncStream, consoleWriter);
				try {
					rsyncable.await();
				} catch (InterruptedException e) {
					OpenShiftCoreActivator.logError("Thread interrupted while running rsync", e); //$NON-NLS-1$
					Thread.currentThread().interrupt();
				}
				return rsyncable;
			}

		}, null);
	}

	/**
	 * Asynchronously writes the logs from the 'rsync' command, provided by the
	 * given {@code syncStream} into the given {@code outputStream}.
	 * 
	 * @param syncStream
	 *            the {@link InputStream} to read from
	 * @param outputStream
	 *            the {@link OutputStream} to write into
	 */
	private void asyncWriteLogs(final InputStream syncStream, final IServerConsoleWriter consoleWriter) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(syncStream));) {
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						consoleWriter.writeToShell(RSync.this.server.getId(), new String[] { line });
					}
				}
			} catch (IOException e) {
				OpenShiftCoreActivator.logError("Error occurred while printing 'rsync' command output", e);
			}
		});
	}

	protected String sanitizePath(String path) {
		return sanitizePath(path, File.separator);
	}

	protected String sanitizeUnixPath(String path) {
		return sanitizePath(path, "/");
	}

	private String sanitizePath(String path, String slash) {
		if (path == null) {
			return null;
		}
		if (path.endsWith(slash) || path.endsWith(slash + ".")) { //$NON-NLS-1$ //$NON-NLS-2$
			return path;
		}
		return path + slash; // $NON-NLS-1$
	}

}