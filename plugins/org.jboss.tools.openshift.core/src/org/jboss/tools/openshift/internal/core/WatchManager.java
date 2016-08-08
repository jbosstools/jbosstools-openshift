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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class WatchManager {

	private static final int[] FIBONACCI = new int[] { 0, 1, 1, 2, 3, 5, 8, 13, 21 };
	private static final long BACKOFF_MILLIS = 5000;
	private static final long BACKOFF_RESET = FIBONACCI[FIBONACCI.length - 1] * BACKOFF_MILLIS * 2;
	public static final String [] KINDS = new String[] {
			ResourceKind.BUILD, 
			ResourceKind.BUILD_CONFIG, 
			ResourceKind.DEPLOYMENT_CONFIG, 
			ResourceKind.EVENT, 
			ResourceKind.IMAGE_STREAM, 
			ResourceKind.POD,
			ResourceKind.REPLICATION_CONTROLLER, 
			ResourceKind.ROUTE,
			ResourceKind.PVC,
			ResourceKind.SERVICE, 
			ResourceKind.TEMPLATE
	};
	
	private Map<IProject, ConcurrentMap<String,IWatcher>> watches = new ConcurrentHashMap<>();
	
	private static class Holder {
		static WatchManager instance = new WatchManager();
	}
	
	public static WatchManager getInstance() {
		return Holder.instance;
	}
	
	private WatchManager() {
		ConnectionsRegistrySingleton.getInstance().addListener(new DeletedConnectionListener());
	}
	
	public void stopWatch(IProject project) {
		Map<String, IWatcher>  watchList = watches.remove(project);
		if(watchList != null) {
			watchList.values().forEach(w->w.stop());
		}
	}

	public void startWatch(final IProject project) {
		ConcurrentMap<String, IWatcher> kindMap = new ConcurrentHashMap<>();
		if(watches.putIfAbsent(project, kindMap) == null){
			final Connection conn = ConnectionsRegistryUtil.getConnectionFor(project);
			if(conn == null) return;
			for (String kind: KINDS) {
				WatchListener listener = new WatchListener(project, conn, kind, 0, 0);
				startWatch(project, 0, 0, listener);
			}
		}
	}
	
	private void startWatch(final IProject project, int backoff, long lastConnect, WatchListener listener) {
		if(listener == null) return;
		listener.start(backoff, lastConnect);
	}

	private static enum State {
		STARTING,
		CONNECTED,
		DISCONNECTED, 
		STOPPING
	}
	
	private class WatchListener implements IOpenShiftWatchListener{
		
		private static final int NOT_FOUND = -1;
		
		final private Connection conn;
		final private IProject project;
		final private String kind;
		private int backoff = 0;
		private long lastConnect = 0;
		private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);
		private List<IResource> resources = Collections.synchronizedList(new ArrayList<>());


		public WatchListener(IProject project, Connection conn, String kind, int backoff, long lastConnect) {
			Trace.debug("WatchManager Adding WatchListener for {0} and kind {1}", project.getName(), kind);
			this.project = project;
			this.conn = conn;
			this.backoff = backoff;
			this.lastConnect = lastConnect;
			this.kind = kind;
			
			if(System.currentTimeMillis() - lastConnect > BACKOFF_RESET) {
				backoff = 0;
			}
			Trace.debug("WatchManager 	Initial watch backoff of {0} ms", FIBONACCI[backoff] * BACKOFF_MILLIS );

		}
	
		@Override
		public void connected(List<IResource> resources) {
			Trace.debug("WatchManager Endpoint connected to {0} with {1} resources", conn.toString(), resources.size());
			this.resources.addAll(resources);
		}

		@Override
		public void disconnected() {
			Trace.debug("WatchManager Endpoint disconnected to {0}.", conn.toString());
			state.set(State.DISCONNECTED);
		}

		@Override
		public void error(Throwable err) {
			Trace.warn("WatchManager Reconnecting. There was an error watching connection {0}: ", err, conn.toString());
			restart();
		}
		
		@SuppressWarnings("incomplete-switch")
		private void restart() {
			switch(state.get()) {
			case STARTING:
				Trace.debug("Returning early from restart.  Already starting for project {0} and kind {1}", project.getName(), kind);
			case DISCONNECTED:
				Trace.debug("Endpoint disconnected and skipping restart for project {0} and kind {1}", project.getName(), kind);
				return;
			}
			try {
				// TODO enhance fix to only check project once
				conn.getResource(project);
				Trace.debug("WatchManager Rescheduling watch job for project {0} and kind {1}", project.getName(), kind);
				startWatch(project, backoff, lastConnect, this);
			}catch(Exception e) {
				Trace.debug("WatchManager Unable to rescheduling watch job for project {0} and kind {1}", e, project.getName(), kind);
				stopWatch(project);
			}
		}



		private class RestartWatchJob extends Job{
			private IClient client;

			RestartWatchJob(IClient client){
				super("OpenShift WatchManager Job");
				this.client = client;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					connect(client);
				}catch(Exception e) {
					Trace.debug("Exception starting watch on project {0} and {1} kind",e,project.getName(), kind);
					backoff++;
					if(backoff >= FIBONACCI.length) {
						Trace.info("Exceeded backoff attempts trying to reconnect watch for {0} and kind {1}",project.getName(), kind);
						watches.remove(project);
						state.set(State.DISCONNECTED);
						return Status.OK_STATUS;
					}
					final long delay = FIBONACCI[backoff] * BACKOFF_MILLIS;
					Trace.debug("Delaying watch restart by {0}ms for project {1} and kind {2} ", delay, project.getName(), kind);
					new RestartWatchJob(client).schedule(delay);
				}
				return Status.OK_STATUS;
			}
			
		}
		
		public void start(int backoff, long lastConnect){
			if(state.getAndSet(State.STARTING) == State.STARTING) {
				Trace.debug("In the process of starting watch already.  Returning early");
				return;
			}
			this.backoff = backoff;
			this.lastConnect = lastConnect;
			Trace.info("Starting watch on project {0} for kind {1}", project.getName(), kind);
			IClient client = getClientFor(project);
			if(client != null) {
				new RestartWatchJob(client).schedule();
			}
		}
		
		private void connect(IClient client) {
			if(watches.containsKey(project)) {
				Map<String, IWatcher> all = watches.get(project);
				all.put(kind, client.watch(project.getName(), this, kind));
				state.set(State.CONNECTED);
				lastConnect = System.currentTimeMillis();
			}
		}
		
		private IClient getClientFor(IProject project) {
			IClient client = project.accept(new CapabilityVisitor<IClientCapability, IClient>() {
				
				@Override
				public IClient visit(IClientCapability cap) {
					return cap.getClient();
				}
			}, null);
			if(client == null) {
				Trace.warn("Unable to start watch.  Project {0} does not support IClientCapability", null, project.getName());
			}
			return client;
		}
		
		@Override
		public void received(IResource resource, ChangeType change) {
			Trace.debug("Watch received change in {0} state\n{1}", state, resource.toJson(false));
			if(State.CONNECTED == state.get()) {
				IResource newItem = null;
				IResource oldItem = null;
				int index = resources.indexOf(resource);
				if (ChangeType.ADDED.equals(change)) {
					resources.add(resource);
					newItem = resource;
				} else if (ChangeType.DELETED.equals(change)) {
					oldItem = index > NOT_FOUND ? resources.remove(index) : resource;
				} else if (ChangeType.MODIFIED.equals(change)) {				
					if(index > NOT_FOUND) {
						oldItem = resources.remove(index);
					}
					resources.add(resource);
					newItem = resource;
				}
				ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_RESOURCE, oldItem, newItem);
			}
		}
		
	}

	private class DeletedConnectionListener extends ConnectionsRegistryAdapter {
		
		@Override
		public void connectionRemoved(IConnection connection) {
			if(!(connection instanceof Connection)){
				return;
			}
			Connection conn = (Connection)connection;
			synchronized (watches) {
				watches.keySet().stream()
					.filter(p->conn.ownsResource(p))
					.collect(Collectors.toList())
					.forEach(p->stopWatch(p));
			}
		}
		
	}
}
