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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceFactoryException;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
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
		implements IResourceDetailsModel, ITemplateListPageModel, ITemplateParametersPageModel{

	private Connection connection;
	private IProject project;
	private List<ObservableTreeItem> projectItems = new ArrayList<>();
	private List<ObservableTreeItem> projectTemplates = new ArrayList<>();
	private ITemplate selectedTemplate;
	private ITemplate localTemplate;
	private ITemplate serverTemplate;
	private List<IParameter> parameters = new ArrayList<IParameter>();
	private IParameter selectedParameter;
	private Map<String, String> originalValueMap;
	private Collection<IResource> items = new ArrayList<IResource>(); 
	private boolean useLocalTemplate = true;
	private String localTemplateFilename;
	private IResourceFactory resourceFactory;
	private org.eclipse.core.resources.IProject eclipseProject;

	private void update(boolean useLocalTemplate, IProject selectedProject, List<ObservableTreeItem> projectItems, ITemplate serverTemplate, String localTemplateFilename) {
		firePropertyChange(PROPERTY_USE_LOCAL_TEMPLATE, this.useLocalTemplate, this.useLocalTemplate = useLocalTemplate);
		updateProjectItems(projectItems);
		firePropertyChange(PROPERTY_PROJECT, this.project, this.project = selectedProject = getProjectOrDefault(selectedProject, projectItems));
		firePropertyChange(PROPERTY_TEMPLATES, this.projectTemplates, this.projectTemplates = getProjectTemplates(selectedProject, projectItems) );
		updateSelectedTemplate(useLocalTemplate, serverTemplate, localTemplate, localTemplateFilename);
		updateTemplateParameters(selectedTemplate);
	}

	private void updateSelectedTemplate(boolean useLocalTemplate, ITemplate serverTemplate, ITemplate localTemplate, String localTemplateFilename) {
		ITemplate template = null;
		if (useLocalTemplate) {
			if (!ObjectUtils.equals(localTemplateFilename, this.localTemplateFilename)) {
				template = this.localTemplate = getLocalTemplate(localTemplateFilename);
				firePropertyChange(PROPERTY_LOCAL_TEMPLATE_FILENAME, this.localTemplateFilename, this.localTemplateFilename = localTemplateFilename);
			} else {
				template = localTemplate;
			}
		} else {
			template = this.serverTemplate = serverTemplate;
		}
		firePropertyChange(PROPERTY_SELECTED_TEMPLATE, this.selectedTemplate, this.selectedTemplate = template);
	}
	
	private ITemplate getLocalTemplate(String filename) {
		if (StringUtils.isBlank(filename)) {
			return null;
		}
		ITemplate template = null;
		filename = VariablesHelper.replaceVariables(filename);
		try {
			if (!Files.isRegularFile(Paths.get(filename))) {
				return null;
			}
			template = resourceFactory.create(createInputStream(filename));
		} catch (FileNotFoundException e) {
			throw new OpenShiftException(e, NLS.bind("Could not find the file \"{0}\" to upload", filename));
		} catch (ResourceFactoryException | ClassCastException e) {
			throw e;
		}
		return template;
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
	public Collection<IResource> getItems() {
		return items;
	}

	private void setItems(Collection<IResource> items) {
		firePropertyChange(PROPERTY_ITEMS, this.items, this.items = items);
	}

	@Override
	public void setServerTemplate(ITemplate serverTemplate) {
		update(false, this.project, this.projectItems, serverTemplate, localTemplateFilename);
	}

	@Override
	public ITemplate getServerTemplate() {
		return serverTemplate;
	}

	@Override
	public ITemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	private void updateTemplateParameters(ITemplate template) {
		if (template == null) {
			return;
		}
		setParameters(new ArrayList<IParameter>(template.getParameters().values()));
		setItems(template.getItems());
		setLabels(template.getObjectLabels());
	}

	private IProject getProjectOrDefault(IProject project, List<ObservableTreeItem> projects) {
		if (project == null) {
			project = getDefaultProject(projects);
		}
		return project;
	}

	@Override
	public void setProject(IProject project) {
		update(this.useLocalTemplate, project, this.projectItems, this.serverTemplate, localTemplateFilename);
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
		ObservableTreeItem connectionItem = TemplateTreeItems.INSTANCE.create(connection);
		connectionItem.load();
		List<ObservableTreeItem> projects = connectionItem.getChildren();
		setProjectItems(projects);
	}

	@Override
	public List<IParameter> getParameters() {
		return parameters;
	}

	@Override
	public void setParameters(List<IParameter> parameters) {
		firePropertyChange(PROPERTY_PARAMETERS, this.parameters, this.parameters = injectProjectParameters(getEclipseProject(), parameters));
		Map<String, String> paramsMap = new HashMap<>();
		if (parameters != null) {
		  parameters.forEach(p -> paramsMap.put(p.getName(), p.getValue()));
		}
		originalValueMap = paramsMap;
	}

	private static List<IParameter> injectProjectParameters(org.eclipse.core.resources.IProject project, List<IParameter> originalParameters) {
		if (originalParameters == null || originalParameters.isEmpty()) {
			return originalParameters;
		}
		Map<String, String> projectParams = getProjectParameters(project);

		List<IParameter> newParameters = originalParameters.stream().map(p -> { 
			IParameter clone = p.clone();
			String value = projectParams.get(clone.getName());
			if (value != null) {
				clone.setValue(value);
			}
			return clone;
		}).collect(Collectors.toList());

		return newParameters;
	}

	private static Map<String, String> getProjectParameters(org.eclipse.core.resources.IProject project) {
		if(project == null) {
			return Collections.emptyMap();
		}
		Map<String,String> projectParams = new HashMap<>();
		String gitRepo = null;
		try {
			gitRepo = StringUtils.defaultString(EGitUtils.getDefaultRemoteRepo(project));
		} catch (CoreException e) {
			throw new OpenShiftException(e, NLS.bind("Could not determine the default remote Git repository for \"{0}\"", project.getName()));
		}
		if (gitRepo != null) {
			projectParams.put("SOURCE_REPOSITORY_URL", gitRepo);
			projectParams.put("GIT_URI", gitRepo);//legacy key
			
			String branch;
			try {
				branch = StringUtils.defaultString(EGitUtils.getCurrentBranch(project));
			} catch (CoreException e) {
				throw new OpenShiftException(e, NLS.bind("Could not determine the default Git branch for \"{0}\"", project.getName()));
			}
			projectParams.put("SOURCE_REPOSITORY_REF", branch);
			projectParams.put("GIT_REF", branch);//legacy key
			
			//Setting the context dir is a really bad idea if we're dealing with a multi module project
			//Better let the user do it manually if needed.
			//String contextDir = getDefaultContextDir(project);
			String contextDir = StringUtils.EMPTY;
			projectParams.put("CONTEXT_DIR", contextDir);
			projectParams.put("GIT_CONTEXT_DIR", contextDir);//legacy key
		}
		return projectParams;
	}

	@Override
	public IParameter getSelectedParameter() {
		return this.selectedParameter;
	}

	@Override
	public void setSelectedParameter(IParameter parameter) {
		firePropertyChange(PROPERTY_SELECTED_PARAMETER, this.selectedParameter, this.selectedParameter = parameter);
	}

	@Override
	public void updateParameterValue(IParameter param, String value) {
		param.setValue(value);
	}

	@Override
	public void resetParameter(IParameter param) {
		updateParameterValue(param, originalValueMap.get(param.getName()));
	}

	private void setLabels(Map<String, String> labelMap) {
		if(labelMap == null) return;
		List<Label> labels =  new ArrayList<Label>(labelMap.size());
		for (Entry<String,String> entry : labelMap.entrySet()) {
			labels.add(new Label(entry.getKey(), entry.getValue()));
		}
		setLabels(labels);
	}

	@Override
	public void setUseLocalTemplate(boolean useLocalTemplate) {
		update(useLocalTemplate, this.project, this.projectItems, this.serverTemplate, this.localTemplateFilename);
	}

	@Override
	public boolean isUseLocalTemplate() {
		return useLocalTemplate;
	}

	public InputStream createInputStream(String fileName) throws FileNotFoundException {
		return new FileInputStream(fileName);
	}

	@Override
	public void setLocalTemplateFileName(String filename) {
		update(true, this.project, this.projectItems, serverTemplate, filename);
	}

	@Override
	public String getLocalTemplateFileName() {
		return this.localTemplateFilename;
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
		update(this.useLocalTemplate, null, null, null, null);
	}

	private IProject getDefaultProject(List<ObservableTreeItem> projects) {
		if (projects == null 
				|| projects.size() == 0) {
			return null;
		}
		return (IProject) projects.get(0).getModel();
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
		update(useLocalTemplate, findProject(this.project, projects), projects, serverTemplate, localTemplateFilename);
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
	public List<ObservableTreeItem> getTemplates() {
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
		updateTemplateParameters(selectedTemplate);
	}

	@Override
	public org.eclipse.core.resources.IProject getEclipseProject() {
		return eclipseProject;
	}
	
}
