/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.job;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.application.IResourceLabelsPageModel.Label;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IProjectTemplateProcessing;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A Job to create an application from a template
 * 
 * @author jeff.cantrill
 */
public class CreateApplicationFromTemplateJob extends AbstractDelegatingMonitorJob implements IResourcesModel {

	private IProject project;
	private ITemplate template;
	private Collection<Label> labels;
	private Collection<IParameter> parameters;
	private Collection<IResource> resources;

	public CreateApplicationFromTemplateJob(IProject project, ITemplate template, Collection<IParameter> parameters, Collection<Label> labels) {
		super("Create Application From Template Job");
		this.project = project;
		this.template = template;
		this.labels = labels;
		this.parameters = parameters;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		template.updateParameterValues(parameters);
		for (Label label : labels) {
			template.addObjectLabel(label.getName(), label.getValue());
		}
		
		IStatus status = project.accept(new CapabilityVisitor<IProjectTemplateProcessing, IStatus>() {

			@Override
			public IStatus visit(IProjectTemplateProcessing capability) {
				
				try {
					ITemplate processed = capability.process(template);
					parameters = processed.getParameters().values();
					
					resources = capability.apply(processed);
					return handleResponse(resources);
				}catch(OpenShiftException e) {
					String message = e.getMessage();
					if(e.getStatus() != null) {
						message = e.getStatus().getMessage();
					}
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, -1, message, e);
				}
			}
		},
		new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Template processing is unsupported for this client and server combination.", null));
		
		
		return status;
	}
	
	/**
	 * Get the list of parameters for this Job.  
	 * The values
	 * will have changed if the parameter is generated and 
	 * has been processed by the server.
	 * @return
	 */
	public Collection<IParameter> getParameters(){
		return parameters;
	}
	
	
	@Override
	public Collection<IResource> getResources(){
		return resources;
	}
	
	private IStatus handleResponse(Collection<IResource> resources) {
		int severity = IStatus.OK;
		for (IResource resource : resources) {
			if(resource.getKind() == ResourceKind.STATUS) {
				severity = IStatus.WARNING;
				break;
			}
		}
		return new Status(severity, OpenShiftUIActivator.PLUGIN_ID, OK,"Resources created from template.",null);
	}

}
