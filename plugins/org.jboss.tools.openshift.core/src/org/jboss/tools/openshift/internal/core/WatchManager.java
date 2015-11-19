/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class WatchManager {

	private static final int[] FIBONACCI = new int[] { 0, 1, 1, 2, 3, 5, 8, 13, 21 };
	private static final long BACKOFF_MILLIS = 5000;
	private static final long BACKOFF_RESET = FIBONACCI[FIBONACCI.length - 1] * BACKOFF_MILLIS * 2;
	private static final String [] KINDS = new String[] {
			ResourceKind.BUILD_CONFIG, 
			ResourceKind.DEPLOYMENT_CONFIG, 
			ResourceKind.SERVICE, 
			ResourceKind.POD,
			ResourceKind.REPLICATION_CONTROLLER, 
			ResourceKind.BUILD, 
			ResourceKind.IMAGE_STREAM, 
			ResourceKind.ROUTE
	};
	
	private Map<IProject, IWatcher> watches = Collections.synchronizedMap(new HashMap<>());
	
	private static class Holder {
		static WatchManager instance = new WatchManager();
	}
	
	public static WatchManager getInstance() {
		return Holder.instance;
	}
	public void startWatch(final IProject project) {
		if(!watches.containsKey(project)){
			startWatch(project, 0, 0);
		}
	}
	
	private void startWatch(final IProject project, int backoff, long lastConnect) {
		final Connection conn = ConnectionsRegistryUtil.getConnectionFor(project);
		final WatchListener listener = new WatchListener(project, conn, backoff, lastConnect);
		listener.start();
	}

	private static enum State {
		STARTING,
		CONNECTED,
		DISCONNECTED
	}
	
	private class WatchListener implements IOpenShiftWatchListener{
		
		private static final int NOT_FOUND = -1;
		
		final private Connection conn;
		final private IProject project;
		private int backoff = 0;
		private long lastConnect = 0;
		private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);
		private List<IResource> resources = Collections.synchronizedList(new ArrayList<>());

		public WatchListener(IProject project, Connection conn, int backoff, long lastConnect) {
			Trace.debug("Adding WatchListener for {0}", project.getName());
			this.project = project;
			this.conn = conn;
			this.backoff = backoff;
			this.lastConnect = lastConnect;
			
			if(System.currentTimeMillis() - lastConnect > BACKOFF_RESET) {
				backoff = 0;
			}
			Trace.debug("Initial watch backoff of {0} ms", FIBONACCI[backoff] * BACKOFF_MILLIS );

		}
	
		@Override
		public void connected(List<IResource> resources) {
			Trace.debug("Endpoint connected to {0} with {1} resources", conn.toString(), resources.size());
			this.resources.addAll(resources);
		}

		@Override
		public void disconnected() {
			Trace.debug("Endpoint disconnected to {0}.", conn.toString());
			restart();
		}

		@Override
		public void error(Throwable err) {
			Trace.warn("Reconnecting. There was an error watching connection {0}:", err, conn.toString());
			restart();
		}
		
		private synchronized void restart() {
			if(state.get() == State.DISCONNECTED) {
				Trace.debug("Restart called but returning early because already disconnected");
				return;
			}
			state.set(State.DISCONNECTED);
			Trace.debug("Rescheduling watch job for project {0}", project.getName());
			startWatch(project, backoff, lastConnect);
		}
		
		private class RestartWatchJob extends Job{
			private IClient client;

			RestartWatchJob(IClient client){
				super("");
				this.client = client;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					connect(client);
				}catch(Exception e) {
					Trace.debug("Exception starting watch on project {0}",e,project.getName());
					backoff++;
					if(backoff > FIBONACCI.length) {
						Trace.info("Exceeded backoff attempts trying to reconnect watch for {0}",project.getName());
						watches.remove(project);
						state.set(State.DISCONNECTED);
						return Status.OK_STATUS;
					}
					final long delay = FIBONACCI[backoff] * BACKOFF_MILLIS;
					Trace.debug("Delaying watch restart by {0}ms", delay);
					new RestartWatchJob(client).schedule(delay);
				}
				return Status.OK_STATUS;
			}
			
		}
		
		public void start() {
			if(state.get() == State.STARTING) {
				Trace.debug("In the process of starting watch already.  Returning early");
				return;
			}
			state.set(State.STARTING);
			Trace.info("Starting watch on project {0}", project.getName());
			IClient client = getClientFor(project);
			new RestartWatchJob(client).schedule();
		}
		
		private void connect(IClient client) {
			watches.put(project, client.watch(project.getName(), this, KINDS));
			state.set(State.CONNECTED);
			lastConnect = System.currentTimeMillis();
		}
		
		private IClient getClientFor(IProject project) {
			IClient client = project.accept(new CapabilityVisitor<IClientCapability, IClient>() {
				
				@Override
				public IClient visit(IClientCapability cap) {
					return cap.getClient();
				}
			}, null);
			if(client == null) {
				throw new OpenShiftException("Unable to start watch.  Project %s does not support IClientCapability", project.getName());
			}
			return client;
		}
		
		@Override
		public void received(IResource resource, ChangeType change) {
			Trace.debug("Watch received change\n{0}",resource.toJson(false));
			IResource newItem = null;
			IResource oldItem = null;
			int index = resources.indexOf(resource);
			switch(change) {
			case ADDED:
				resources.add(resource);
				newItem = resource;
				break;
			case DELETED:
				oldItem = index > NOT_FOUND ? resources.remove(index) : resource;
				break;
			case MODIFIED:
				if(index > NOT_FOUND) {
					oldItem = resources.remove(index);
				}
				resources.add(resource);
				newItem = resource;
				break;
			}
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_RESOURCE, oldItem, newItem);
		}
		
	}
}
