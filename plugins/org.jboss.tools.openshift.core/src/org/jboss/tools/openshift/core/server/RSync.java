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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.capability.resources.IRSyncable;
import com.openshift.restclient.capability.resources.IRSyncable.LocalPeer;
import com.openshift.restclient.capability.resources.IRSyncable.PodPeer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;

public class RSync extends OCBinaryOperation {

	private IService service;
	private String podPath;
	private IServer server;

	public RSync(IService service, String podPath, IServer server) {
		this.service = service;
		this.podPath = sanitizePath(podPath);
		this.server = server;
	}

	private static String sanitizePath(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith("/") || path.endsWith("/.")) {
			return path;
		}
		return path+"/";
	}
	
	
	public void syncPodsToDirectory(File deployFolder, MultiStatus status) {
		String location = OCBinary.getInstance().getLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);

		
		// If our deploy folder is empty, sync all pods to this directory
		boolean shouldSync = true;
		//boolean shouldSync = !deployFolder.exists() || deployFolder.listFiles().length == 0; 
		if (shouldSync) {
			for (IPod pod : service.getPods()) {
				try {
					syncPodToDirectory(pod, podPath, deployFolder);
				} catch (IOException | OpenShiftException e) {
					status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
				}
			}
		}
	}

	// Sync the directory back to all pods
	public void syncDirectoryToPods(File deployFolder, MultiStatus status) {
		String location = OCBinary.getInstance().getLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);

		for (IPod pod : service.getPods()) {
			try {
				syncDirectoryToPod(pod, deployFolder, podPath);
			} catch (IOException | OpenShiftException e) {
				status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
			}
		}
	}
	
	
	@Override
	@Deprecated
	protected void runOCBinary(MultiStatus status) {
		// Deprecated?  Doesn't fit the workflow really since we have to split the functionality
	}

	private void syncPodToDirectory(IPod pod, String podPath, File destination) throws IOException {
		destination.mkdirs();
		String destinationPath = sanitizePath(destination.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				rsyncable.sync(new PodPeer(podPath, pod), new LocalPeer(destinationPath));
				return rsyncable;
			}
		}, null);
	}

	private void syncDirectoryToPod(IPod pod, File source, String podPath) throws IOException {
		String sourcePath = sanitizePath(source.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				rsyncable.sync(new LocalPeer(sourcePath), new PodPeer(podPath, pod));
				return rsyncable;
			}
		}, null);
	}

}