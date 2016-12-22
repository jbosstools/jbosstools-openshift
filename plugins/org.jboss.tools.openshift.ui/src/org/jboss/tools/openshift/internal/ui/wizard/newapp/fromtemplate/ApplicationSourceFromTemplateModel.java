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
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.job.IResourcesModelJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSourceListPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSourceModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IResourceDetailsModel;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A Model to handle creating a microservice application
 * based on a template
 * 
 * @author jeff.cantrill
 *
 */
public class ApplicationSourceFromTemplateModel 
	extends ResourceLabelsPageModel 
	implements IResourceDetailsModel, ITemplateParametersPageModel, IApplicationSourceModel {

	//intentionally package local, facilitates refresh of widgets in TemplateParametersPage
	static final String PROPERTY_MODIFIED_PARAMETER = "modifiedParameter";
	
	private IProject project;
	private ITemplate template;
	private List<IParameter> parameters = new ArrayList<>();
	private IParameter selectedParameter;
	private Map<String, String> originalValueMap;
	private Collection<IResource> items = new ArrayList<>(); 
	private org.eclipse.core.resources.IProject eclipseProject;

	@Override
	public IResourcesModelJob createFinishJob() {
		return new AppFromTemplateJob();
	}
	
	class AppFromTemplateJob implements IResourcesModelJob{
		
		private final CreateApplicationFromTemplateJob job;
		
		AppFromTemplateJob(){
			job = new CreateApplicationFromTemplateJob(
					project,
					template,
					getParameters(),
					getLabels());
		}
		
		@Override
		public Collection<IResource> getResources() {
			return job.getResources();
		}
		
		@Override
		public Runnable getSummaryRunnable(final Shell shell) {
			return new Runnable() {
				@Override
				public void run() {
					final String message = NLS.bind(
							"Results of creating the resources from the {0} template.", 
							template.getName());
					new NewApplicationSummaryFromTemplateDialog(shell, job, message).open();
				}
			};
		}
		
		@Override
		public Job getJob() {
			return job;
		}
		
		@Override
		public DelegatingProgressMonitor getDelegatingProgressMonitor() {
			return job.getDelegatingProgressMonitor();
		}
		
		@Override
		public void addJobChangeListener(IJobChangeListener listener) {
			job.addJobChangeListener(listener);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch(evt.getPropertyName()) {
		case IResourceLabelsPageModel.PROPERTY_LABELS:
			setLabels((List<Label>) evt.getNewValue());
			break;
		case IApplicationSourceListPageModel.PROPERTY_PROJECT:
			this.project = (IProject) evt.getNewValue();
			break;
		case IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE:
			handleSelectedAppSource(evt);
			break;
		case IApplicationSourceListPageModel.PROPERTY_ECLIPSE_PROJECT:
			handleEclipseProject(evt);
			break;
		}
	}
	
	private void handleSelectedAppSource(PropertyChangeEvent evt) {
		if(evt.getNewValue() instanceof IApplicationSource
				&& ResourceKind.TEMPLATE.equals(((IApplicationSource) evt.getNewValue()).getKind())){
			IApplicationSource source = (IApplicationSource) evt.getNewValue();
			ITemplate newTemplate = (ITemplate) source.getSource();
			if (!Objects.equals(newTemplate, this.template)) {
				this.template = newTemplate;
				updateTemplateParameters(newTemplate);
			}
		} 
	}
	private void handleEclipseProject(PropertyChangeEvent evt) {
		if(evt.getNewValue() instanceof org.eclipse.core.resources.IProject) {
			this.eclipseProject = (org.eclipse.core.resources.IProject) evt.getNewValue();
			updateTemplateParameters(template);
		} 
	}

	@Override
	public Collection<IResource> getItems() {
		return items;
	}

	private void setItems(Collection<IResource> items) {
		firePropertyChange(PROPERTY_ITEMS, this.items, this.items = items);
	}

	private void updateTemplateParameters(ITemplate template) {
		if (template == null) {
			return;
		}
		setParameters(template.getParameters().values());
		setItems(template.getObjects());
		setLabels(template.getObjectLabels());
	}

	@Override
	public List<IParameter> getParameters() {
		return parameters;
	}

	public boolean isParameterModified(IParameter param) {
		return param != null && originalValueMap != null
			&& !Objects.equals(param.getValue(), originalValueMap.get(param.getName()));
	}

	@Override
	public void setParameters(Collection<IParameter> parameters) {
		List<IParameter> oldParameters = new ArrayList<>(this.parameters);
		if (parameters == null) {
			this.parameters.clear();
		} else {
			this.originalValueMap = toMap(parameters);
			List<IParameter> newParameters = new ArrayList<>();
			newParameters.addAll(injectProjectParameters(eclipseProject, parameters));
			this.parameters.clear();
			this.parameters.addAll(newParameters);
			Collections.sort(this.parameters, new TemplateParameterViewerUtils.ParameterNameComparator());
		}
		firePropertyChange(PROPERTY_PARAMETERS, oldParameters, this.parameters);

		// update selected parameter
		setSelectedParameter(getSelectedParameterOrDefault());
	}

	private Map<String, String> toMap(Collection<IParameter> parameters) {
		Map<String, String> paramsMap = new HashMap<>();
		if (parameters != null) {
		  parameters.forEach(p -> paramsMap.put(p.getName(), p.getValue()));
		}
		return paramsMap;
	}

	private Collection<IParameter> injectProjectParameters(org.eclipse.core.resources.IProject project, Collection<IParameter> originalParameters) {
		if (originalParameters == null || originalParameters.isEmpty()) {
			return originalParameters;
		}
		Map<String, String> projectParams = getProjectParameters(project);

		originalParameters = originalParameters.stream().map(p -> { 
			String value = projectParams.get(p.getName());
			if (value != null) {
			    p = p.clone();
				p.setValue(value);
			}
			return p;
		}).collect(Collectors.toList());

		return originalParameters;
	}

	private Map<String, String> getProjectParameters(org.eclipse.core.resources.IProject project) {
		if(project == null) {
			return Collections.emptyMap();
		}

		Map<String,String> projectParams = new HashMap<>();
		String gitRepo = getGitRepo(project);
		if (gitRepo != null) {
			projectParams.put(PARAMETER_SOURCE_REPOSITORY_URL, gitRepo);
			projectParams.put(PARAMETER_GIT_URI, gitRepo);//legacy key
			
			String branch = getGitBranch(project);
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

	private String getGitBranch(org.eclipse.core.resources.IProject project) {
		try {
			return StringUtils.defaultString(EGitUtils.getCurrentBranch(project));
		} catch (CoreException e) {
			throw new OpenShiftException(e, NLS.bind("Could not determine the default Git branch for \"{0}\"", project.getName()));
		}
	}

	private String getGitRepo(org.eclipse.core.resources.IProject project) {
		try {
			return StringUtils.defaultString(EGitUtils.getDefaultRemoteRepo(project));
		} catch (CoreException e) {
			throw new OpenShiftException(e, NLS.bind("Could not determine the default remote Git repository for \"{0}\"", project.getName()));
		}
	}

	@Override
	public IParameter getSelectedParameter() {
		return this.selectedParameter;
	}

	private IParameter getSelectedParameterOrDefault() {
		if (selectedParameter == null
				|| !parameters.contains(selectedParameter)) {
			if (CollectionUtils.isEmpty(parameters)) {
				return null;
			} else {
				return parameters.get(0);
			}
		} else {
			return selectedParameter;
		}
	}

	@Override
	public void setSelectedParameter(IParameter parameter) {
		firePropertyChange(PROPERTY_SELECTED_PARAMETER, this.selectedParameter, this.selectedParameter = parameter);
	}

	@Override
	public void updateParameterValue(IParameter param, String value) {
		param.setValue(value);
		firePropertyChange(PROPERTY_MODIFIED_PARAMETER, null, param);
		//Let's flip the selection to cause refresh of the state of buttons.
		//Otherwise, we would have to introduce more properties and keep there state.
		IParameter s = selectedParameter;
		setSelectedParameter(null);
		setSelectedParameter(s);
	}

	@Override
	public void resetParameter(IParameter param) {
		if (param != null && !Objects.equals(param.getValue(), originalValueMap.get(param.getName()))) {
			updateParameterValue(param, originalValueMap.get(param.getName()));
		}
	}

	private void setLabels(Map<String, String> labelMap) {
		if(labelMap == null) return;
		List<Label> labels =  new ArrayList<>(labelMap.size());
		for (Entry<String,String> entry : labelMap.entrySet()) {
			labels.add(new Label(entry.getKey(), entry.getValue()));
		}
		setLabels(labels);
	}

	@Override
	public void dispose() {
		super.dispose();
		project = null;
		template = null;
		parameters.clear();
		selectedParameter = null;
		if(originalValueMap != null) {
			originalValueMap.clear();
		}
		items.clear();
		eclipseProject = null;
	}

}
