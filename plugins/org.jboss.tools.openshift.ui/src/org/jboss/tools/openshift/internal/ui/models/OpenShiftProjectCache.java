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
package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.Trace;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * Project cache impl
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class OpenShiftProjectCache implements IProjectCache, IConnectionsRegistryListener {

	private Map<String, Set<IProjectAdapter>> cache = new ConcurrentHashMap<String, Set<IProjectAdapter>>();
	private Set<IProjectCacheListener> listeners = new CopyOnWriteArraySet<>();

	public OpenShiftProjectCache() {
		ConnectionsRegistrySingleton.getInstance().addListener(this);
	}
	
	@Override
	public Collection<IProjectAdapter> getProjectsFor(final IOpenShiftConnection conn) {
		Set<IProjectAdapter> adapters = getCacheFor(conn);
		if(adapters == null) {
			Collection<IProject> projects = conn.<IProject>getResources(ResourceKind.PROJECT);
			boolean initialize = false;
			synchronized (cache) {
				adapters = getCacheFor(conn);
				if(adapters == null) {
					initialize = true;
					adapters = createEntry(conn);
				}
			}
			if(initialize) {
				synchronized (adapters) {
					for (IProject p : projects) {
						addNewProjectAdapter(adapters, conn, p);
					}
				}
				adapters.forEach(a->notifyAdd(a));
			}
		}
		return new ArrayList<>(adapters);
	}

	@Override
	public void flushFor(IOpenShiftConnection conn) {
		Set<IProjectAdapter> adapters = removeEntry(getCacheKey(conn));
		if (adapters != null) {
			notifyRemove(adapters);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		if (!(connection instanceof IOpenShiftConnection)) {
			return;
		}
		if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) 
				&& (oldValue instanceof List)
				&& (newValue instanceof List)) {
			IOpenShiftConnection openshiftConnection = (IOpenShiftConnection) connection;
			Set<IProjectAdapter> adapters;
			synchronized (cache) {
				adapters = getCacheFor(openshiftConnection);
				if(adapters == null) { //called where cache was not initialized for the connection
					adapters = createEntry(openshiftConnection);
				}
			}
			addAndRemoveAdaptersFor((List<IProject>) newValue, (List<IProject>) oldValue, openshiftConnection, adapters);
		}
	}

	private boolean hasCacheFor(IOpenShiftConnection conn) {
		final String key = getCacheKey(conn);
		return cache.containsKey(key);
	}

	/**
	 * Returns set of project adapters or null if they are not initialized for the connection.
	 * 
	 * @param conn
	 * @return
	 */
	private Set<IProjectAdapter> getCacheFor(IOpenShiftConnection conn) {
		synchronized (cache) {
			final String key = getCacheKey(conn);
			if(cache.containsKey(key)) {
				return cache.get(key);
			}
		}
		return null;
	}

	private Set<IProjectAdapter> createEntry(IOpenShiftConnection conn) {
		final String key = getCacheKey(conn);
		Set<IProjectAdapter> adapters = new HashSet<>();
		cache.put(key, adapters);
		return adapters;
	}

	private Set<IProjectAdapter> removeEntry(String key) {
		return cache.remove(key);
	}

	private void addAndRemoveAdaptersFor(List<IProject> newProjects, List<IProject> oldProjects, IOpenShiftConnection conn, Set<IProjectAdapter> adapters) {
		synchronized (adapters) {
			Map<IProject, IProjectAdapter> projectMap = adapters.stream()
					.collect(Collectors.toMap(IProjectAdapter::getProject, Function.identity()));

			ListDiff diffs = Diffs.computeListDiff(oldProjects, newProjects);
			diffs.accept(new ListDiffVisitor() {

				@Override
				public void handleRemove(int index, Object element) {
					if (!(element instanceof IProject))
						return;
					IProject project = (IProject) element;
					if (projectMap.containsKey(project)) {
						IProjectAdapter adapter = projectMap.remove(project);
						if (adapters.remove(adapter)) {
							adapter.dispose();
							notifyRemove(adapter);
						}
					}
				}

				@Override
				public void handleAdd(int index, Object element) {
					if (!(element instanceof IProject)) {
						return;
					}
					IProjectAdapter adapter = addNewProjectAdapter(adapters, conn, (IProject) element);
					if (adapter != null) {
						notifyAdd(adapter);
					}
				}
			});
		}
	}

	@Override
	public void connectionAdded(IConnection connection) {
	}

	@Override
	public void connectionRemoved(IConnection connection) {
		if (!(connection instanceof IOpenShiftConnection)) {
			return;
		}
		IOpenShiftConnection conn = (IOpenShiftConnection) connection;
		flushFor(conn);
	}

	private IProjectAdapter addNewProjectAdapter(Collection<IProjectAdapter> adapters, IOpenShiftConnection conn, IProject project) {
		OpenShiftProjectUIModel model = new OpenShiftProjectUIModel(conn, project);
		synchronized(adapters) {
			if (adapters.add(model)) {
				return model;
			}
			return null;
		}
	}

	private void notifyAdd(IProjectAdapter adapter) {
		if (adapter == null) {
			return;
		}
		for (IProjectCacheListener l : listeners) {
			try {
				l.handleAddToCache(this, adapter);
			} catch (Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
		}
	}

	private void notifyRemove(Collection<IProjectAdapter> adapters) {
		IProjectAdapter [] copy;
		synchronized (adapters) {
			copy = adapters.toArray(new IProjectAdapter [] {});
		}
		for (IProjectAdapter adapter : copy) {
			notifyRemove(adapter);
		}
	}

	private void notifyRemove(IProjectAdapter adapter) {
		if (adapter == null) {
			return;
		}
		for (IProjectCacheListener l : listeners) {
			try {
				l.handleRemoveFromCache(this, adapter);
			} catch (Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
			
		}
	}

	private String getCacheKey(IOpenShiftConnection conn) {
		return NLS.bind("{0}@{1}", conn.getUsername(), conn.toString());
	}

	@Override
	public void addListener(IProjectCacheListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(IProjectCacheListener listener) {
		listeners.remove(listener);
	}

	
}
