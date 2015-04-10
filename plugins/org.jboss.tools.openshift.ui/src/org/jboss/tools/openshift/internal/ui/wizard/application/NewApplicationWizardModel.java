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
package org.jboss.tools.openshift.internal.ui.wizard.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IProjectTemplateList;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * The New application wizard model that supports instantiation of
 * a template
 * @author jeff.cantrill
 *
 */
public class NewApplicationWizardModel 
		extends ObservableUIPojo implements ITemplateListPageModel, ITemplateParametersPageModel, IResourceLabelsPageModel {

	private IProject project;
	private ITemplate template;
	private List<IParameter> parameters = new ArrayList<IParameter>();
	private IParameter selectedParameter;
	private HashMap<String, String> originalValueMap;
	private Collection<String> readonlyLabels = Arrays.asList(new String [] {"template"});
	private List<Label> labels;
	private Label selectedLabel;

	public NewApplicationWizardModel(IProject project) {
		this.project = project;
	}

	@Override
	public void setTemplate(ITemplate template) {
		firePropertyChange(PROPERTY_TEMPLATE, this.template,this.template = template);
		if(template == null) return;
		setParameters(new ArrayList<IParameter>(template.getParameters().values()));
		
		setLabels(template.getLabels());
	}
	
	public IProject getProject() {
		return this.project;
	}
	
	@Override
	public ITemplate getTemplate() {
		return this.template;
	}

	@Override
	public Collection<ITemplate> getTemplates() {
		
		return project.accept(new CapabilityVisitor<IProjectTemplateList,  Collection<ITemplate>>() {

			@Override
			public  Collection<ITemplate> visit(IProjectTemplateList capability) {
				return capability.getTemplates();
			}
		}, new ArrayList<ITemplate>());
		
	}

	@Override
	public List<IParameter> getParameters() {
		return parameters;
	}
	
	@Override
	public void setParameters(List<IParameter> parameters) {
		firePropertyChange(PROPERTY_PARAMETERS, this.parameters, this.parameters = parameters);
		this.originalValueMap = new HashMap<String, String>(this.parameters.size());
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
	
	

}
