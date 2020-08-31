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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.utils.ConfigHelper;
import org.jboss.tools.openshift.core.odo.utils.ConfigWatcher;
import org.jboss.tools.openshift.core.odo.utils.ConfigWatcher.Listener;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.odo.OdoCli;

import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 *
 */
public class ApplicationExplorerUIModel extends AbstractOpenshiftUIModel<ApplicationExplorerUIModel.ClusterInfo, ApplicationExplorerUIModel> implements Listener, IResourceChangeListener, IResourceDeltaVisitor {

	private static ApplicationExplorerUIModel INSTANCE;
	
	public static ApplicationExplorerUIModel getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ApplicationExplorerUIModel();
		}
		return INSTANCE;
	}
	
	static class ClusterInfo {
		private OpenShiftClient client = loadClient();
		
		Odo getOdo() throws IOException {
			return OdoCli.get();
		}

		/**
		 * @return
		 */
		public OpenShiftClient getClient() {
			return client;
		}
		
		private OpenShiftClient loadClient() {
		    return new DefaultOpenShiftClient(new ConfigBuilder().build());
		}

		/**
		 * 
		 */
		public void reload() {
			client = loadClient();
		}
	}
	
    private final Map<String, ComponentDescriptor> components = new HashMap<>();

    private Odo odo;
    
    private Job watcherJob;
    
    private Config config;
	
	private ApplicationExplorerUIModel() {
		super(null, new ClusterInfo());
		loadProjects();
		watcherJob = Job.createSystem("Watching kubeconfig", this::startWatcher);
		watcherJob.schedule();
		this.config = loadConfig();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}


	@Override
	public void refresh() {
		getWrapped().reload();
		fireChanged(this);
	}
	
	public Odo getOdo() throws IOException {
		if (odo == null) {
			odo = new OdoProjectDecorator(getWrapped().getOdo(), this);
		}
		return odo;
	}
	
	public OpenShiftClient getClient() {
		return getWrapped().getClient();
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
        try {
        	  List<ComponentDescriptor> descriptors = getOdo().discover(project.getLocation().toOSString());
        	  descriptors.forEach(descriptor -> addContextToSettings(descriptor.getPath(), descriptor));
        } catch (IOException e) {}
    }

	/**
	 * @param path
	 */
	public void removeContext(String path) {
		if (components.remove(path) != null) {
			refresh();
		}
	}
	
	/**
	 * 
	 */
	private void loadProjects() {
		for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			addContext(project);
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
        Context currentContext = KubeConfigUtils.getCurrentContext(currentConfig);
        Context newContext = KubeConfigUtils.getCurrentContext(newConfig);
        return hasServerChanged(newContext, currentContext)
                || hasNewToken(newContext, newConfig, currentContext, currentConfig);
    }

    private boolean hasServerChanged(Context newContext, Context currentContext) {
        return newContext == null
                || currentContext == null
                || !StringUtils.equals(currentContext.getCluster(), newContext.getCluster())
                || !StringUtils.equals(currentContext.getUser(), newContext.getUser());
    }

	private boolean hasNewToken(Context newContext, Config newConfig, Context currentContext, Config currentConfig) {
		if (newContext == null) {
			return false;
		}
		if (currentContext == null) {
			return true;
		}
		String newToken = KubeConfigUtils.getUserToken(newConfig, newContext);
		if (newToken == null) {
			// logout, do not refresh, LogoutAction already refreshes
			return false;
		}
		String currentToken = KubeConfigUtils.getUserToken(currentConfig, currentContext);
		return !StringUtils.equals(newToken, currentToken);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this, IResource.NONE);
		} catch (CoreException e) {
			OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
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
			if (delta.getKind() == IResourceDelta.ADDED) {
				addContext((IProject) resource);
			} else if (delta.getKind() == IResourceDelta.REMOVED) {
				removeContext(resource.getLocation().toOSString());
			} else if (delta.getKind() == IResourceDelta.CHANGED) {
				if (resource.isAccessible()) {
					addContext((IProject) resource);
				} else {
					removeContext(resource.getLocation().toOSString());
				}
			}
		}
		return false;
	}
}
