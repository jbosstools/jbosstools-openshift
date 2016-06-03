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
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.comparators.CreationTimestampComparator;
import org.jboss.tools.openshift.internal.ui.models2.AbstractResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models2.ServiceWrapper;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IDeployCapability;
import com.openshift.restclient.model.IDeploymentConfig;

/**
 * Trigger the deployment of OpenShift resources 
 * based on a DeploymentConfig
 * 
 * @author jeff.cantrill
 *
 */
public class TriggerDeploymentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IDeploymentConfig resource = retrieveDeploymentConfig(selection);
		if(resource == null) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Trigger Deployment", "A deployment or deployment config must be selected in order to trigger a new deployment.");
			return null;
		}
		
		scheduleJob(resource);
		return null;
	}

	private IDeploymentConfig retrieveDeploymentConfig(ISelection selection) {
		ServiceWrapper deployment = UIUtils.getFirstElement(selection, ServiceWrapper.class);
		if(deployment != null) {
			Collection<AbstractResourceWrapper<?, ?>> configs = deployment.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG);
			if(!configs.isEmpty()) {
				if(configs.size() == 1) {
					return (IDeploymentConfig) configs.iterator().next().getResource();
				}else {
					return (IDeploymentConfig) configs
							.stream()
							.sorted(new CreationTimestampComparator())
							.collect(Collectors.toList()).get(0);
				}
			}
			return null;
		}
		return UIUtils.getFirstElement(selection, IDeploymentConfig.class);
	}

	private void scheduleJob(final IDeploymentConfig resource) {
		new AbstractDelegatingMonitorJob("Starting new OpenShift deployment...") {

			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				return resource.accept(new CapabilityVisitor<IDeployCapability, IStatus>() {

					@Override
					public IStatus visit(IDeployCapability cap) {
						try {
							cap.deploy();
						}catch(OpenShiftException e) {
							return new Status(IStatus.ERROR, 
									OpenShiftUIActivator.PLUGIN_ID, 
									NLS.bind("Unable to perform a deployment for config {0}", resource.getName())
									, e);
						}
						return new Status(IStatus.OK, 
								OpenShiftUIActivator.PLUGIN_ID,
								NLS.bind("Deployment triggered for config {0}", resource.getName()));
					}
				}, Status.OK_STATUS);
			}
			
		}.schedule();
	}

}
