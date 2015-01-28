/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.jobs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.deployment.DeploymentWizardContext;

import com.openshift.internal.kube.Resource;
import com.openshift.internal.kube.builders.ImageDeploymentBuilder;
import com.openshift.internal.kube.builders.SourceDeploymentBuilder;
import com.openshift.kube.ResourceKind;

/**
 * @author Jeff Cantrill
 */
public class CreateDeploymentJob extends AbstractDelegatingMonitorJob {

	private String sourceUrl;
	private DeploymentWizardContext context;

	public CreateDeploymentJob(String sourceUrl, DeploymentWizardContext context) {
		super("Create Deployment Job");
		this.context = context;
		this.sourceUrl = sourceUrl;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			List<Resource> resources;
			if (context.includeBuildConfig()) {
				SourceDeploymentBuilder builder = new SourceDeploymentBuilder(context.getNamespace(), this.sourceUrl,
						context.getUserName(), context.getImage().getImageUri(), context.getRepositoryUri());
				resources = builder.build();
			} else {
				ImageDeploymentBuilder builder = new ImageDeploymentBuilder(context.getNamespace(), context.getImage()
						.getImageUri(), 27017);
				resources = builder.build();
			}

			List<com.openshift.kube.Status> errors = new ArrayList<com.openshift.kube.Status>();
			for (Resource r : resources) {
				createResource(errors, r);
			}
			// trigger a deployment here? maybe post a build? post a deployment?

			if (!errors.isEmpty()) {
				// TODO fix me
				return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, IStatus.ERROR, errors.toString(), null);
			} else {
				return new Status(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, OK, "Complete", null);
			}
			// catch here?
		} catch (Exception e) {
			return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, IStatus.ERROR, "", e);
		}
	}
	
	private void createResource(List<com.openshift.kube.Status> errors, Resource resource) {
		Resource response = context.getClient().create(resource);
		if(ResourceKind.Status == response.getKind()){
			errors.add((com.openshift.kube.Status) response);
		}
	}

}
