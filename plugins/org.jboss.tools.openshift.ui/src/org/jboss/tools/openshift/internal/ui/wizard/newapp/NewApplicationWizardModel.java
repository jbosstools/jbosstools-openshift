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
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

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
		extends ObservablePojo 
		implements IResourceDetailsModel, ITemplateListPageModel, ITemplateParametersPageModel, IResourceLabelsPageModel, IConnectionAware<Connection> {

	private Connection connection;
	private IProject project;
	private List<ObservableTreeItem> projects = new ArrayList<>();
	private ITemplate template;
	private ITemplate uploadedTemplate;
	private List<IParameter> parameters = new ArrayList<IParameter>();
	private IParameter selectedParameter;
	private HashMap<String, String> originalValueMap;
	private Collection<String> readonlyLabels = Arrays.asList(new String [] {"template"});
	private List<Label> labels;
	private Label selectedLabel;
	private Collection<IResource> items = new ArrayList<IResource>(); 
	private boolean uploadTemplate = true;
	private String templateFilename;
	private IResourceFactory resourceFactory;

	private void update(boolean useUploadTemplate, IProject selectedProject, List<ObservableTreeItem> projects, ITemplate selectedTemplate) {
		firePropertyChange(PROPERTY_USE_UPLOAD_TEMPLATE, this.uploadTemplate, this.uploadTemplate = useUploadTemplate);
		firePropertyChange(PROPERTY_PROJECTS, Collections.emptyList(), Collections.unmodifiableList(this.projects));
		firePropertyChange(PROPERTY_PROJECT, this.project, this.project = getProjectOrDefault(selectedProject, projects));
		firePropertyChange(PROPERTY_TEMPLATE, this.template, this.template = selectedTemplate);
		initTemplateParameters(selectedTemplate);
	}
	
	@Override
	public Collection<IResource> getItems() {
		return items;
	}

	private void setItems(Collection<IResource> items) {
		firePropertyChange(PROPERTY_ITEMS, this.items, this.items = items);
	}

	@Override
	public void setTemplate(ITemplate template) {
		update(this.uploadTemplate, this.project, this.projects, template);
	}

	private void initTemplateParameters(ITemplate template) {
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
		update(this.uploadTemplate, project, this.projects, this.template);
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public ITemplate getTemplate() {
		return template;
	}

	@Override
	public void loadResources() {
		if (connection == null) {
			return;
		}
		ObservableTreeItem connectionItem = TemplateTreeItems.INSTANCE.create(connection);
		connectionItem.load();
		List<ObservableTreeItem> projects = connectionItem.getChildren();
		setProjects(projects);
	}

	@Override
	public List<IParameter> getParameters() {
		return parameters;
	}

	@Override
	public void setParameters(List<IParameter> parameters) {
		firePropertyChange(PROPERTY_PARAMETERS, this.parameters, this.parameters = parameters);
		this.originalValueMap = new HashMap<String, String>(parameters.size());
		for (IParameter param : parameters) {
			originalValueMap.put(param.getName(), param.getValue());
		}
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

	@Override
	public List<Label> getLabels() {
		return this.labels;
	}

	@Override
	public void setLabels(List<Label> labels) {
		firePropertyChange(PROPERTY_LABELS, this.labels, this.labels = labels);
	}

	@Override
	public Collection<String> getReadOnlyLabels() {
		return readonlyLabels;
	}

	@Override
	public void setSelectedLabel(Label label) {
		firePropertyChange(PROPERTY_SELECTED_LABEL, this.selectedLabel, this.selectedLabel = label);
	}

	@Override
	public Label getSelectedLabel() {
		return this.selectedLabel;
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
	public void removeLabel(Label label) {
		List<Label> old = new ArrayList<Label>(this.labels);
		final int index = labels.indexOf(label);
		this.labels.remove(label);
		fireIndexedPropertyChange(PROPERTY_LABELS, index, old, Collections.unmodifiableList(labels));
	}

	@Override
	public void updateLabel(Label label, String key, String value) {
		List<Label> old = new ArrayList<Label>(this.labels);
		final int index = labels.indexOf(label);
		labels.set(index, new Label(key, value));
		fireIndexedPropertyChange(PROPERTY_LABELS, index, old, Collections.unmodifiableList(labels));
	}

	@Override
	public void addLabel(String key, String value) {
		List<Label> old = new ArrayList<Label>(this.labels);
		this.labels.add(new Label(key, value));
		fireIndexedPropertyChange(PROPERTY_LABELS, this.labels.size(), old, Collections.unmodifiableList(labels));
	}

	@Override
	public void setUseUploadTemplate(boolean uploadTemplate) {
		update(uploadTemplate, this.project, this.projects, this.template);
	}

	@Override
	public boolean isUseUploadTemplate() {
		return this.uploadTemplate;
	}

	@Override
	public void setTemplateFileName(String name) {
		try {
			uploadedTemplate = resourceFactory.create(createInputStream(name));
			setTemplate(uploadedTemplate);
		} catch (FileNotFoundException e) {
			name = "";
			setTemplate(null);
			throw new OpenShiftException(e, "Unable to find the file to upload");
		} catch (ResourceFactoryException | ClassCastException e) {
			name = "";
			setTemplate(null);
			throw e;
		} finally {
			firePropertyChange(PROPERTY_TEMPLATE_FILENAME, this.templateFilename, this.templateFilename = name);
		}
	}
	
	public InputStream createInputStream(String fileName) throws FileNotFoundException {
		return new FileInputStream(fileName);
	}

	@Override
	public String getTemplateFileName() {
		return this.templateFilename;
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
	public Connection setConnection(Connection connection) {
		if (ObjectUtils.equals(connection, this.connection)) {
			return this.connection;
		}

		setResourceFactory(connection);
		reset();
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		return connection;
	}

	private void reset() {
		update(this.uploadTemplate, null, null, null);
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
	
	private void setProjects(List<ObservableTreeItem> projects) {
		this.projects.clear();
		if (projects != null) {
			this.projects.addAll(projects);
		}
		update(this.uploadTemplate, this.project, this.projects, this.template);
	}

	@Override
	public List<ObservableTreeItem> getProjects() {
		return this.projects;
	}
	
	@Override
	public boolean hasProjects() {
		return projects != null 
				&& projects.size() > 0;
	}

	@Override
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}


}
