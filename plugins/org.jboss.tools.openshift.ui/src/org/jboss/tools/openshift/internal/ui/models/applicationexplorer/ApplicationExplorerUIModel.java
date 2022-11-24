/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.utils.ConfigHelper;
import org.jboss.tools.openshift.core.odo.utils.ConfigWatcher;
import org.jboss.tools.openshift.core.odo.utils.ConfigWatcher.Listener;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.odo.OdoCliFactory;

import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;

/**
 * @author Red Hat Developers
 *
 */
public class ApplicationExplorerUIModel
		extends AbstractOpenshiftUIModel<ApplicationExplorerUIModel.ClusterClient, ApplicationExplorerUIModel>
		implements Listener, IResourceChangeListener, IResourceDeltaVisitor {

	private static ApplicationExplorerUIModel INSTANCE;

	public static ApplicationExplorerUIModel getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ApplicationExplorerUIModel();
		}
		return INSTANCE;
	}

	public static class ClusterClient {
		private Odo odo;

		public Odo getOdo() {
			return odo;
		}

	}

	private final Map<String, ComponentDescriptor> components = new ConcurrentHashMap<>();

	private Odo odo;

	private Job watcherJob;

	private Config config;

	private DevfileRegistriesElement registries;

	protected ApplicationExplorerUIModel(ClusterClient clusterClient) {
		super(null, clusterClient);
		watcherJob = Job.createSystem("Watching kubeconfig", this::startWatcher);
		watcherJob.schedule();
		this.config = loadConfig();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		refresh();
	}

	private ApplicationExplorerUIModel() {
		this(new ClusterClient());
	}

	protected OdoCliFactory getFactory() {
		return OdoCliFactory.getInstance();
	}

	private CompletableFuture<Odo> initializeOdo() {
		return getFactory().getOdo().whenComplete((odo, err) -> {
			this.odo = odo;
			loadProjects();
		});
	}

	@Override
	public void refresh() {
		initializeOdo().thenAccept(odo -> {
			getWrapped().odo = odo;
			ApplicationExplorerUIModel.this.odo = null;
			fireChanged(this);
		});
	}

	public Odo getOdo() {
		if (odo == null && getWrapped().getOdo() != null) {
			odo = new OdoProjectDecorator(getWrapped().getOdo(), this);
		}
		return odo;
	}

	protected Config loadConfig() {
		return ConfigHelper.safeLoadKubeConfig();
	}

	/**
	 * @return the components
	 */
	public Map<String, ComponentDescriptor> getComponents() {
		return components;
	}

	private void addContextToSettings(String path, ComponentDescriptor descriptor) {
		if (!components.containsKey(path)) {
			components.put(path, descriptor);
			refresh();
		}
	}

	public void addContext(IProject project) {
		if (odo != null) {
			try {
				List<ComponentDescriptor> descriptors = odo.discover(project.getLocation().toOSString());
				descriptors.forEach(descriptor -> addContextToSettings(descriptor.getPath(), descriptor));
			} catch (IOException e) {
				OpenShiftUIActivator.statusFactory().errorStatus(e);
			}
		}
	}

	public void removeContext(IProject project) {
		if (project.getLocation() != null) {
			removeContext(project.getLocation().toOSString());
		}
	}

	public void removeContext(String path) {
		if (components.remove(path) != null) {
			refresh();
		}
	}

	private void internalLoadProjects() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			addContext(project);
		}
	}

	private void loadProjects() {
		if (Display.getCurrent() == null) {
			internalLoadProjects();
		} else {
			Job.createSystem("Load model", monitor -> {
				internalLoadProjects();
				refresh();
				return Status.OK_STATUS;
			}).schedule();
		}
	}

	private void startWatcher(IProgressMonitor monitor) {
		new ConfigWatcher(Paths.get(ConfigHelper.getKubeConfigPath()), this).run();
	}

	@Override
	public void onUpdate(ConfigWatcher source, Config config) {
		if (hasContextChanged(config, this.config)) {
			refresh();
		}
		this.config = config;
	}

	private boolean hasContextChanged(Config newConfig, Config currentConfig) {
		NamedContext currentContext = KubeConfigUtils.getCurrentContext(currentConfig);
		NamedContext newContext = KubeConfigUtils.getCurrentContext(newConfig);
		return hasServerChanged(newContext, currentContext)
				|| hasNewToken(newContext, newConfig, currentContext, currentConfig);
	}

	private boolean hasServerChanged(NamedContext newContext, NamedContext currentContext) {
		return newContext == null || currentContext == null
				|| !StringUtils.equals(currentContext.getContext().getCluster(), newContext.getContext().getCluster())
				|| !StringUtils.equals(currentContext.getContext().getUser(), newContext.getContext().getUser())
				|| !StringUtils.equals(currentContext.getContext().getNamespace(),
						newContext.getContext().getNamespace());
	}

	private boolean hasNewToken(NamedContext newContext, Config newConfig, NamedContext currentContext,
			Config currentConfig) {
		if (newContext == null) {
			return false;
		}
		if (currentContext == null) {
			return true;
		}
		String newToken = KubeConfigUtils.getUserToken(newConfig, newContext.getContext());
		if (newToken == null) {
			// logout, do not refresh, LogoutAction already refreshes
			return false;
		}
		String currentToken = KubeConfigUtils.getUserToken(currentConfig, currentContext.getContext());
		return !StringUtils.equals(newToken, currentToken);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this, IResource.NONE);
		} catch (CoreException e) {
			OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		if (resource == null) {
			return false;
		}
		if (resource.getType() == IResource.ROOT) {
			return true;
		} else if (resource.getType() == IResource.PROJECT) {
			IProject project = (IProject) resource;
			if (delta.getKind() == IResourceDelta.ADDED) {
				addContext(project);
			} else if (delta.getKind() == IResourceDelta.REMOVED) {
				removeContext(project);
			} else if (delta.getKind() == IResourceDelta.CHANGED) {
				if (project.isAccessible()) {
					addContext(project);
				} else {
					removeContext(project);
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public DevfileRegistriesElement getRegistriesElement() {
		if (registries == null) {
			registries = new DevfileRegistriesElement(this);
		}
		return registries;
	}

}
