/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
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
import java.util.concurrent.Executors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerConsoleWriter;
import org.jboss.tools.openshift.internal.core.OCBinaryOperation;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability.OpenShiftBinaryOption;
import com.openshift.restclient.capability.resources.IRSyncable;
import com.openshift.restclient.capability.resources.IRSyncable.LocalPeer;
import com.openshift.restclient.capability.resources.IRSyncable.PodPeer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public class RSync {

	private final IResource resource;
	private final String podPath;
	private final IServer server;

	public RSync(final IResource resource, final String podPath, final IServer server) {
		this.resource = resource;
		this.podPath = sanitizePath(podPath);
		this.server = server;
	}

	private static String sanitizePath(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith("/") || path.endsWith("/.")) { //$NON-NLS-1$ //$NON-NLS-2$
			return path;
		}
		return path+"/"; //$NON-NLS-1$
	}
	
	
	public void syncPodsToDirectory(File deployFolder, MultiStatus status, final IServerConsoleWriter consoleWriter) {
		new OCBinaryOperation() {
			@Override
			protected void runOCBinary(MultiStatus multiStatus) {
				// If our deploy folder is empty, sync all pods to this directory
				boolean shouldSync = true;
				//boolean shouldSync = !deployFolder.exists() || deployFolder.listFiles().length == 0; 
				if (shouldSync) {
					for (IPod pod : ResourceUtils.getPodsForResource(resource, resource.getProject().getResources(ResourceKind.POD))) {
						try {
						    if ("Running".equals(pod.getStatus())) {
	                            syncPodToDirectory(pod, podPath, deployFolder, consoleWriter);
						    }
						} catch (IOException | OpenShiftException e) {
							status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
						}
					}
				}
			}
		}.run(status);
	}

	// Sync the directory back to all pods
	public void syncDirectoryToPods(File deployFolder, MultiStatus status, final IServerConsoleWriter consoleWriter, final OpenShiftBinaryOption... options) {
		new OCBinaryOperation() {
			
			@Override
			protected void runOCBinary(MultiStatus multiStatus) {
				for (IPod pod : ResourceUtils.getPodsForResource(resource, resource.getProject().getResources(ResourceKind.POD))) {
					try {
					    if ("Running".equals(pod.getStatus())) {
	                        syncDirectoryToPod(pod, deployFolder, podPath, consoleWriter);
					    }
					} catch (IOException | OpenShiftException e) {
						status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
					}
				}
			}
		}.run(status);
	}
	
	private void syncPodToDirectory(IPod pod, String podPath, File destination,
			final IServerConsoleWriter consoleWriter) throws IOException {
		destination.mkdirs();
		String destinationPath = sanitizePath(destination.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@SuppressWarnings("resource") //$NON-NLS-1$
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				final InputStream syncStream = rsyncable.sync(new PodPeer(podPath, pod),
						new LocalPeer(destinationPath), OpenShiftBinaryOption.EXCLUDE_GIT_FOLDER, OpenShiftBinaryOption.SKIP_TLS_VERIFY);
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

	private void syncDirectoryToPod(final IPod pod, final File source, final String podPath, final IServerConsoleWriter consoleWriter) throws IOException {
		String sourcePath = sanitizePath(source.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				final InputStream syncStream = rsyncable.sync(new LocalPeer(sourcePath), new PodPeer(podPath, pod), OpenShiftBinaryOption.EXCLUDE_GIT_FOLDER, OpenShiftBinaryOption.SKIP_TLS_VERIFY);
				asyncWriteLogs(syncStream, consoleWriter);
				try {
					rsyncable.await();
				} catch (InterruptedException e) {
					OpenShiftCoreActivator.logError("Thread interrupted while running rsync", e);
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
	 * @param syncStream the {@link InputStream} to read from
	 * @param outputStream the {@link OutputStream} to write into
	 */
	private void asyncWriteLogs(final InputStream syncStream, final IServerConsoleWriter consoleWriter) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
			try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(syncStream));) {
				String line;
					while ((line = bufferedReader.readLine()) != null) {
						consoleWriter.writeToShell(RSync.this.server.getId(), new String[]{line});
					}
			}
			} catch(IOException e)  {
				OpenShiftCoreActivator.logError("Error occurred while printing 'rsync' command output", e);
			}
		});
	}

}