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
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

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
	
	/**
	 * A map storing relation between Openshift projects and related watcher.
	 * The String is computed from the Openshift project and it not the Openshift
	 * project because 2 different Openshift connections may have projects with the
	 * same name.
	 */
	private Map<WatchKey, AtomicReference<IWatcher>> watches = new ConcurrentHashMap<>();
	
	private static class Holder {
		static WatchManager instance = new WatchManager();
	}
	
	public static WatchManager getInstance() {
		return Holder.instance;
	}
	
	private WatchManager() {
		ConnectionsRegistrySingleton.getInstance().addListener(new DeletedConnectionListener());
	}
	
	public void stopWatch(IProject project, IOpenShiftConnection connection) {
		AtomicReference<IWatcher> watcherRef = watches.remove(new WatchKey(connection, project));
		if((watcherRef != null) && (watcherRef.get() != null)) {
			watcherRef.get().stop();
		}
	}
	
	public void startWatch(final IProject project, final IOpenShiftConnection connection) {
		AtomicReference<IWatcher> watcherRef = new AtomicReference<>();
		if(watches.putIfAbsent(new WatchKey(connection, project), watcherRef) == null) {
				WatchListener listener = new WatchListener(project, connection, KINDS, 0, 0);
				startWatch(project, 0, 0, listener);
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
	
	/**
	 * Class representing a key in the global watches table.
	 */
	private static class WatchKey {
	    private IOpenShiftConnection connection;
        private IProject project;

        private WatchKey(IOpenShiftConnection connection, IProject project) {
	        this.connection = connection;
	        this.project = project;
	    }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((connection == null) ? 0 : connection.hashCode());
            result = prime * result + ((project == null) ? 0 : project.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WatchKey other = (WatchKey) obj;
            if (connection == null) {
                if (other.connection != null)
                    return false;
            } else if (!connection.equals(other.connection))
                return false;
            if (project == null) {
                if (other.project != null)
                    return false;
            } else if (!project.equals(other.project))
                return false;
            return true;
        }
	}
	
	private class WatchListener implements IOpenShiftWatchListener{
		
		private static final int NOT_FOUND = -1;
		
		final private IOpenShiftConnection conn;
		final private IProject project;
		final private String[] kinds;
		private int backoff = 0;
		private long lastConnect = 0;
		private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);
		private List<IResource> resources = Collections.synchronizedList(new ArrayList<>());


		public WatchListener(IProject project, IOpenShiftConnection conn, String[] kinds, int backoff, long lastConnect) {
			Trace.debug("Adding WatchListener for {0} and kinds {1}", project.getName(), kinds);
			this.project = project;
			this.conn = conn;
			this.backoff = backoff;
			this.lastConnect = lastConnect;
			this.kinds = kinds;
			
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
			if(state.get() == State.STARTING) {
				Trace.debug("Returning early from restart.  Already starting for project {0} and kinds {1}", project.getName(), kinds);
				return;
			}
			try {
				// TODO enhance fix to only check project once
				conn.getResource(project);
				Trace.debug("Rescheduling watch job for project {0} and kinds {1}", project.getName(), kinds);
				startWatch(project, backoff, lastConnect, this);
			}catch(Exception e) {
				Trace.debug("Unable to rescheduling watch job for project {0} and kinds {1}", e, project.getName(), kinds);
				stopWatch(project, conn);
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
					Trace.debug("Exception starting watch on project {0} and {1} kinds",e,project.getName(), kinds);
					backoff++;
					if(backoff >= FIBONACCI.length) {
						Trace.info("Exceeded backoff attempts trying to reconnect watch for {0} and kinds {1}",project.getName(), kinds);
						watches.remove(project);
						state.set(State.DISCONNECTED);
						return Status.OK_STATUS;
					}
					final long delay = FIBONACCI[backoff] * BACKOFF_MILLIS;
					Trace.debug("Delaying watch restart by {0}ms for project {1} and kinds {2} ", delay, project.getName(), kinds);
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
			Trace.info("Starting watch on project {0} for kinds {1}", project.getName(), kinds);
			IClient client = getClientFor(project);
			if(client != null) {
				new RestartWatchJob(client).schedule();
			}
		}
		
		private void connect(IClient client) {
		    WatchKey key = new WatchKey(conn, project);
			if(watches.containsKey(key)) {
				AtomicReference<IWatcher> watcherRef = watches.get(key);
				watcherRef.set(client.watch(project.getName(), this, kinds));
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
					.filter(k->k.connection.equals(conn))
					.collect(Collectors.toList())
					.forEach(k->stopWatch(k.project, k.connection));
			}
		}
		
	}
}
