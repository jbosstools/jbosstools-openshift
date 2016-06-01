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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablePage;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablesPageModel;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IReplicationController;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class ManageEnvironmentVariablesHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IReplicationController rc = findReplicationController(event);
		if(rc != null) {
			ManageEnvironmentVariablesWizard wizard = new ManageEnvironmentVariablesWizard(rc);
			WizardUtils.openWizardDialog(wizard, HandlerUtil.getActiveShell(event));
		}
		return null;
	}

	/**
	 * If a replication controller is selected, method returns it.
	 * If a deployment config is selected, method returns it.
	 * If a deployment is selected, method finds a deployment config and returns it. 
	 * @param event
	 * @return
	 */
	IReplicationController findReplicationController(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IDeploymentConfig dc = UIUtils.getFirstElement(selection, IDeploymentConfig.class);
		if(dc != null) {
			return dc;
		}
		IReplicationController rc = UIUtils.getFirstElement(selection, IReplicationController.class);
		if(rc != null) {
			return rc;
		}
		Deployment deployment = UIUtils.getFirstElement(selection, Deployment.class);
		Collection<IResourceUIModel> dcs = deployment.getDeploymentConfigs();
		if(!dcs.isEmpty()) {
			dc = (IDeploymentConfig)dcs.iterator().next().getResource();
			if(dc != null) {
				return dc;
			}
		}
		return null;
	}
}

class ManageEnvironmentVariablesWizard extends Wizard {
	EnvironmentVariablesPageModel model = new EnvironmentVariablesPageModel();
	IReplicationController dc;

	public ManageEnvironmentVariablesWizard(IReplicationController dc) {
		this.dc = dc;
		List<EnvironmentVariable> vars = dc.getEnvironmentVariables().stream().map(var -> 
			new EnvironmentVariable(var.getName(), var.getValue()))
			.collect(Collectors.toList());
		model.setEnvironmentVariables(vars);
	}

	@Override
	public boolean performFinish() {
		new Job("Updating environment variables for deployment config " + dc.getName()) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					List<EnvironmentVariable> vars = model.getEnvironmentVariables();
					boolean modified = false;
					for (EnvironmentVariable var: vars) {
						if(model.isEnvironmentVariableModified(var)) {
							modified = true;
							dc.setEnvironmentVariable(var.getKey(), var.getValue());
						}
					}
					if(modified) {
						Connection conn = ConnectionsRegistryUtil.getConnectionFor(dc);
						conn.updateResource(dc);
					}
				} catch(Exception e) {
					String message = "Unable to update environment variables for deployment config " + dc.getName();
					OpenShiftUIActivator.getDefault().getLogger().logError(message,e);
					return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, e);
				}
				return Status.OK_STATUS;
			}
			
		}.schedule();
		return true;
	}

	@Override
	public void addPages() {
		EnvironmentVariablePage page = new EnvironmentVariablePage(
				"Manage Environment Variables", 
				"", 
				"EnvVars",
				this,
				model) {
			
			@Override
			protected void doCreateControls(Composite parent, DataBindingContext dbc) {
				GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);
				//Env Variables Block
				createEnvVariableControl(parent, dbc);
			}
		};
		addPage(page);
	}
}
