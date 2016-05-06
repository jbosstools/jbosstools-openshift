/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.common.IProjectPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceFactoryException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.ITemplate;

/**
 * The New application wizard model that supports instantiation of
 * a template
 *
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class NewApplicationWizardModel 
		extends ResourceLabelsPageModel 
		implements IApplicationSourceListPageModel{

	private Connection connection;
	private IProject project;
	private List<ObservableTreeItem> projectItems = new ArrayList<>();
	private List<ObservableTreeItem> projectTemplates = new ArrayList<>();
	private IApplicationSource selectedAppSource;
	private IApplicationSource localAppSource;
	private IApplicationSource serverAppSource;
	private boolean useLocalAppSource = true;
	private String localAppSourceFilename;
	private IResourceFactory resourceFactory;
	private org.eclipse.core.resources.IProject eclipseProject;

	private void update(boolean useLocalAppSource, IProject selectedProject, List<ObservableTreeItem> projectItems, IApplicationSource appSource, String localAppSourceFilename) {
		firePropertyChange(PROPERTY_USE_LOCAL_APP_SOURCE, this.useLocalAppSource, this.useLocalAppSource = useLocalAppSource);
		updateProjectItems(projectItems);
		firePropertyChange(PROPERTY_PROJECT, this.project, this.project = selectedProject = getProjectOrDefault(selectedProject, projectItems));
		firePropertyChange(PROPERTY_APP_SOURCES, this.projectTemplates, this.projectTemplates = getProjectTemplates(selectedProject, projectItems) );
		
		updateSelectedAppSource(useLocalAppSource, appSource, localAppSource, localAppSourceFilename);
	}

	private void updateSelectedAppSource(boolean useLocalAppSource, IApplicationSource serverAppSource, IApplicationSource localAppSource, String localAppSourceFilename) {
		IApplicationSource source = null;
		if (useLocalAppSource) {
			if (!ObjectUtils.equals(localAppSourceFilename, this.localAppSourceFilename)) {
				source = this.localAppSource = getLocalAppSource(localAppSourceFilename);
				firePropertyChange(PROPERTY_LOCAL_APP_SOURCE_FILENAME, this.localAppSourceFilename, this.localAppSourceFilename = localAppSourceFilename);
			} else {
				source = localAppSource;
			}
		} else {
			source = this.serverAppSource = serverAppSource;
		}
		updateLabels(source);
		firePropertyChange(PROPERTY_SELECTED_APP_SOURCE, this.selectedAppSource, this.selectedAppSource = source);
	}
	
	private void updateLabels(IApplicationSource source) {
		if(source != null && ResourceKind.TEMPLATE.equals(source.getKind())) {
			ITemplate template = (ITemplate) source.getSource();
			setLabels(template.getObjectLabels());
			return;
		}
		setLabels(Collections.emptyMap());
	}
	
	private void setLabels(Map<String, String> labelMap) {
		if(labelMap == null) return;
		List<Label> labels =  new ArrayList<>(labelMap.size());
		for (Entry<String,String> entry : labelMap.entrySet()) {
			labels.add(new Label(entry.getKey(), entry.getValue()));
		}
		setLabels(labels);
	}
	
	private IApplicationSource getLocalAppSource(String filename) {
		if (StringUtils.isBlank(filename)) {
			return null;
		}
		IResource resource = null;
		filename = VariablesHelper.replaceVariables(filename);
		try {
			if (!OpenshiftUIConstants.URL_VALIDATOR.isValid(filename) && !Files.isRegularFile(Paths.get(filename))) {
				return null;
			}
			resource = resourceFactory.create(createInputStream(filename));
			if(resource != null && !(resource instanceof ITemplate)) {
				throw new NotATemplateException(resource.getKind());
			}
		} catch (FileNotFoundException e) {
			throw new OpenShiftException(e, NLS.bind("Could not find the file \"{0}\" to upload", filename));
		} catch (IOException e) {
            throw new OpenShiftException(e, NLS.bind("Error reading the file or URL \"{0}\" to upload", filename));
		} catch (ResourceFactoryException | ClassCastException e) {
			throw e;
		}
		switch(resource.getKind()) {
		case ResourceKind.TEMPLATE:
			return new TemplateApplicationSource((ITemplate)resource);
		}
		throw new OpenShiftException("Creating applications from local files is only allowed using a template");
	}
	
	
	private List<ObservableTreeItem> getProjectTemplates(IProject selectedProject, List<ObservableTreeItem> allProjects) {
		if (allProjects == null) {
			return null;
		}
		for (ObservableTreeItem item : allProjects) {
			if (item.getModel().equals(selectedProject)) {
				return item.getChildren();
			}
		}
		return allProjects;
	}

	private void updateProjectItems(List<ObservableTreeItem> projectItems) {
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.projectItems);
		// ensure we're not operating on the same list
		List<ObservableTreeItem> newItems = new ArrayList<>();
		if (projectItems != null) {
			newItems.addAll(projectItems);
		}
		this.projectItems.clear();
		this.projectItems.addAll(newItems);
		firePropertyChange(PROPERTY_PROJECT_ITEMS, oldItems, this.projectItems);
	}
	
	@Override
	public void setServerAppSource(IApplicationSource appSource) {
		update(false, this.project, this.projectItems, appSource, localAppSourceFilename);
	}

	@Override
	public IApplicationSource getServerAppSource() {
		return serverAppSource;
	}

	@Override
	public IApplicationSource getSelectedAppSource() {
		return selectedAppSource;
	}

	private IProject getProjectOrDefault(IProject project, List<ObservableTreeItem> projects) {
		if (project == null) {
			project = getDefaultProject(projects);
		}
		return project;
	}

	@Override
	public void setProject(IProject project) {
		update(this.useLocalAppSource, project, this.projectItems, this.serverAppSource, localAppSourceFilename);
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void loadResources() {
		if (connection == null) {
			return;
		}
		ObservableTreeItem connectionItem = ApplicationSourceTreeItems.INSTANCE.create(connection);
		connectionItem.load();
		List<ObservableTreeItem> projects = connectionItem.getChildren();
		setProjectItems(projects);
	}


	@Override
	public void setUseLocalAppSource(boolean useLocalTemplate) {
		update(useLocalTemplate, this.project, this.projectItems, this.serverAppSource, this.localAppSourceFilename);
	}

	@Override
	public boolean isUseLocalAppSource() {
		return useLocalAppSource;
	}

	public InputStream createInputStream(String fileName) throws IOException {
	    if (!OpenshiftUIConstants.URL_VALIDATOR.isValid(fileName)) {
	        return new FileInputStream(fileName);
	    } else {
	        return new URL(fileName).openStream();
	    }
	}

	@Override
	public void setLocalAppSourceFileName(String filename) {
		update(true, this.project, this.projectItems, serverAppSource, filename);
	}

	@Override
	public String getLocalAppSourceFileName() {
		return this.localAppSourceFilename;
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean hasConnection() {
		return connection != null;
	}

	@Override
	public void setConnection(Connection connection) {
		if (ObjectUtils.equals(connection, this.connection)) {
			return;
		}

		setResourceFactory(connection);
		reset();
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}

	private void reset() {
		update(this.useLocalAppSource, null, null, null, null);
	}

	Comparator<ObservableTreeItem> comparator = new ProjectViewerComparator(new OpenShiftExplorerLabelProvider()).asItemComparator();

	private IProject getDefaultProject(List<ObservableTreeItem> projects) {
		if (projects == null 
				|| projects.size() == 0) {
			return null;
		} else if(projects.size() == 1) {
			return (IProject) projects.get(0).getModel();
		}
		ObservableTreeItem[] items = projects.toArray(new ObservableTreeItem[projects.size()]);
		Arrays.sort(items, comparator);
		return (IProject) items[0].getModel();
	}

	private void setResourceFactory(Connection connection) {
		if (connection != null) {
			this.resourceFactory = connection.getResourceFactory();
		}
	}

	public void setResourceFactory(IResourceFactory factory) {
		this.resourceFactory = factory;
	}
	
	protected void setProjectItems(List<ObservableTreeItem> projects) {
		update(useLocalAppSource, findProject(this.project, projects), projects, serverAppSource, localAppSourceFilename);
	}

	private IProject findProject(final IProject project, List<ObservableTreeItem> projects) {
		if(project == null 
				|| CollectionUtils.isEmpty(projects)) {
			return null;
		}

		return (IProject) projects.stream()
			.filter(item -> {
				if(item.getModel() instanceof IProject) {
					IProject p = (IProject) item.getModel();
					if (p != null) {
						boolean equals =  ObjectUtils.equals(project, p);
						return equals;
					}
				}
				return false;
			})
			.findFirst()
			.map(item -> item.getModel())
			.orElse(null);
	}

	@Override
	public List<ObservableTreeItem> getProjectItems() {
		return this.projectItems;
	}

	@Override
	public List<ObservableTreeItem> getAppSources() {
		return this.projectTemplates;
	}
	
	@Override
	public boolean hasProjects() {
		return projectItems != null 
				&& !projectItems.isEmpty();
	}

	@Override
	public Object getContext() {
		return null;
	}

	@Override
	public void setEclipseProject(org.eclipse.core.resources.IProject eclipseProject) {
		firePropertyChange(PROPERTY_ECLIPSE_PROJECT, this.eclipseProject, this.eclipseProject = eclipseProject);
	}

	@Override
	public org.eclipse.core.resources.IProject getEclipseProject() {
		return eclipseProject;
	}

	@Override
	public void dispose() {
		super.dispose();
		connection = null;
		project = null;
		projectItems.clear();
		projectTemplates.clear();
		selectedAppSource = null;
		localAppSource = null;
		serverAppSource = null;
		resourceFactory = null;
		eclipseProject = null;
	}
}
