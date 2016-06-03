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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models2.AbstractResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models2.ServiceWrapper;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IReplicationController;

/**
 * Handle for scaling deployments
 * @author jeff.cantrill
 *
 */
public class ScaleDeploymentHandler extends AbstractHandler{

	public static final String REPLICA_DIFF = "org.jboss.tools.openshift.ui.command.deployment.scale.replicadiff";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IReplicationController rc = getSelectedElement(event, IReplicationController.class);
		if(rc != null) {
			scaleUsing(event, rc, rc.getName());
			return null;
		}
		ServiceWrapper deployment = getSelectedElement(event, ServiceWrapper.class);
		if(deployment != null) {
			Collection<AbstractResourceWrapper<?, ?>> rcs = deployment.getResourcesOfKind(ResourceKind.REPLICATION_CONTROLLER);
			if(!rcs.isEmpty()) {
				//there should be only 1 per deployment, we'll assume this is true
				rc = (IReplicationController) rcs.iterator().next().getResource();
				scaleUsing(event, rc, deployment.getResource().getName());
			}
		}
		return null;
	}
	
	protected void scaleUsing(ExecutionEvent event, IReplicationController rc, String name) {
		final int replicas = getDesiredReplicas(rc, event);
		scaleDeployment(event, name, rc, replicas);
	}
	
	protected void scaleDeployment(ExecutionEvent event, String name, IReplicationController rc, int replicas) {
		if(replicas >=0 ) {
			new Job(NLS.bind("Scaling {0} deployment ...", name)){
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						rc.setDesiredReplicaCount(replicas);
						Connection conn = ConnectionsRegistryUtil.getConnectionFor(rc);
						conn.updateResource(rc);
					}catch(Exception e) {
						String message = NLS.bind("Unable to scale {0}", name);
						OpenShiftUIActivator.getDefault().getLogger().logError(message,e);
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, e);
					}
					return Status.OK_STATUS;
				}
				
			}.schedule();
		}
		
	}

	protected <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		return UIUtils.getFirstElement(selection, klass);
	}
	
	private int getDesiredReplicas(IReplicationController rc, ExecutionEvent event) {
		String diff = event.getParameter(REPLICA_DIFF);
		int current = rc.getDesiredReplicaCount();
		if(!NumberUtils.isNumber(diff)) {
			return showInputDialog(current, event);
		}
		return current + Integer.parseInt(diff);
	}
	
	protected int showInputDialog(int current, ExecutionEvent event) {
		Shell shell = HandlerUtil.getActivePart(event).getSite().getShell();
		InputDialog dialog = new InputDialog(shell, 
				"Scale deployment", 
				"Enter the desired number of replicas for this deployment", 
				String.valueOf(current), new IInputValidator() {
			
			@Override
			public String isValid(String newText) {
				if(StringUtils.isNumeric(newText)) {
					return null;
				}
				return "Only numeric values greater than zero are allowed";
			}
		});
		int result = dialog.open();
		if(Dialog.OK == result) {
			return Integer.parseInt(dialog.getValue());
		}
		return -1;
	}
}
