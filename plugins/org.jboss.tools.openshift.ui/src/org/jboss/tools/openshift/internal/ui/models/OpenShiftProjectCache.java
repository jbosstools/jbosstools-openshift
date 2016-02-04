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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
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
 * @author jeff.cantrill
 *
 */
public class OpenShiftProjectCache implements IProjectCache, IConnectionsRegistryListener {
	private Map<String, Set<IProjectAdapter>> cache = Collections.synchronizedMap(new HashMap<>());
	private Set<IProjectCacheListener> listeners = Collections.synchronizedSet(new HashSet<>());

	public OpenShiftProjectCache() {
		ConnectionsRegistrySingleton.getInstance().addListener(this);
	}
	
	@Override
	public synchronized Collection<IProjectAdapter> getProjectsFor(final IOpenShiftConnection conn) {
		String key = getCacheKey(conn);
		if(!cache.containsKey(key)) {
			Collection<IProject> projects = conn.<IProject>getResources(ResourceKind.PROJECT);
			Set<IProjectAdapter> adapters = new HashSet<>();
			for (IProject project : projects) {
				newProjectAdapter(adapters, conn, project);
			}
			cache.put(key, adapters);
			notifyAdd(adapters);
		}
		return new ArrayList<>(cache.get(key));
	}
	
	
	
	@Override
	public synchronized void flushFor(IOpenShiftConnection conn) {
		Set<IProjectAdapter> adapters = cache.remove(getCacheKey(conn));
		if(adapters != null) {
			notifyRemove(adapters);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		if (!(connection instanceof IOpenShiftConnection)) {
			return;
		}
		if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) && (oldValue instanceof List) && (newValue instanceof List)) {
			List<IProject> newProjects = (List<IProject>) newValue;
			List<IProject> oldProjects = (List<IProject>) oldValue;
			IOpenShiftConnection conn = (IOpenShiftConnection)connection;
			final String key = getCacheKey(conn);
			
			Set<IProjectAdapter> adapters = (Set<IProjectAdapter>) ObjectUtils.defaultIfNull(cache.get(key), new HashSet<>());
			Map<IProject, IProjectAdapter> projectMap = adapters.stream().collect(Collectors.toMap(IProjectAdapter::getProject, Function.identity()));
			
			ListDiff diffs = Diffs.computeListDiff(oldProjects, newProjects);
			Set<IProjectAdapter> added = new HashSet<>();
			Set<IProjectAdapter> removed = new HashSet<>();
			diffs.accept(new ListDiffVisitor() {
				
				@Override
				public void handleRemove(int index, Object element) {
					if(!(element instanceof IProject)) return;
					IProject project = (IProject) element;
					if(projectMap.containsKey(project)) {
						IProjectAdapter adapter = projectMap.remove(project);
						if(adapters.remove(adapter)) {
							removed.add(adapter);
						}
					}
				}
				
				@Override
				public void handleAdd(int index, Object element) {
					if(!(element instanceof IProject)) return;
					IProjectAdapter adapter = newProjectAdapter(adapters, conn, (IProject)element);
					if(adapter != null) {
						added.add(adapter);
					}
				}
			});
			cache.put(key, adapters);
			notifyRemove(removed);
			notifyAdd(added);
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

	private IProjectAdapter newProjectAdapter(Collection<IProjectAdapter> adapters, IOpenShiftConnection conn, IProject project) {
		OpenShiftProjectUIModel model = new OpenShiftProjectUIModel(conn, project);
		if(adapters.add(model)) {;
			return model;
		}
		return null;
	}
	
	
	private void notifyAdd(Collection<IProjectAdapter> adapters) {
		synchronized (listeners) {
			try {
				for (IProjectAdapter a : adapters) {
					listeners.forEach(l->l.handleAddToCache(this, a));
				}
			}catch(Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
		}
	}

	private void notifyRemove(Collection<IProjectAdapter> adapters) {
		synchronized (listeners) {
			try {
				for (IProjectAdapter a : adapters) {
					listeners.forEach(l->l.handleRemoveFromCache(this, a));
					a.dispose();
				}
			}catch(Exception e) {
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
