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

import static org.jboss.tools.openshift.core.OpenShiftCoreConstants.ODO_CONFIG_YAML;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.openshift.core.odo.LocalConfig;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.odo.OdoCli;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 *
 */
public class ApplicationExplorerUIModel extends AbstractOpenshiftUIModel<ApplicationExplorerUIModel.ClusterInfo, ApplicationExplorerUIModel> {

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

	}
	
    public static class ComponentDescriptor {
        private final String path;
        private final String project;
        private final String application;
        private final String name;

        ComponentDescriptor(String project, String application, String path, String name) {
            this.project = project;
            this.application = application;
            this.path = path;
            this.name = name;
        }

        public String getProject() {
            return project;
        }

        public String getApplication() {
            return application;
        }

        public String getName() {
            return name;
        }

        public LocalConfig.ComponentSettings getSettings() throws IOException {
            return LocalConfig.load(new File(path, ODO_CONFIG_YAML).toURI().toURL()).getComponentSettings();
        }
    }

    private final Map<String, ComponentDescriptor> components = new HashMap<>();

    private Odo odo;
	
	private ApplicationExplorerUIModel() {
		super(null, new ClusterInfo());
		loadProjects();
	}


	@Override
	public void refresh() {
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

	/**
	 * @return the components
	 */
	public Map<String, ComponentDescriptor> getComponents() {
		return components;
	}
	
    private void addContextToSettings(String path, LocalConfig.ComponentSettings componentSettings) {
        if (!components.containsKey(path)) {
            components.put(path, new ComponentDescriptor(componentSettings.getProject(), componentSettings.getApplication(), path, componentSettings.getName()));
            refresh();
        }
    }

    private void addContext(IProject project) {
        try {
            IFile file = project.getFile(new Path(ODO_CONFIG_YAML));
            if (file != null && file.isAccessible()) {
                LocalConfig config = LocalConfig.load(file.getLocationURI().toURL());
                addContextToSettings(project.getLocation().toOSString(), config.getComponentSettings());
            }
        } catch (IOException e) { }
    }

	/**
	 * 
	 */
	private void loadProjects() {
		for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			addContext(project);
		}
	}
}
