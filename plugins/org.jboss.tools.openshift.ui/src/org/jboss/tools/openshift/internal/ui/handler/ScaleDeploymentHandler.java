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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;

import com.openshift.restclient.ResourceKind;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.ScalingComponent;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;

/**
 * Handle for scaling deployments
 * @author jeff.cantrill
 *
 */
public class ScaleDeploymentHandler extends AbstractHandler{

	public static final String REPLICA_DIFF = "org.jboss.tools.openshift.ui.command.deployment.scale.replicadiff";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResourceWrapper<?, ?> selected = getSelectedElement(event, IResourceWrapper.class);
		if(selected == null) {
			return null;
		}

		String name = null; //Show in job name and dialog title.
		IServiceWrapper deployment = null;
		IReplicationController rc = null;

		if(selected instanceof IServiceWrapper) {
			deployment = (IServiceWrapper)selected;
			rc = findReplicationController(deployment);
			name = deployment.getWrapped().getName();
		} else if(ResourceKind.REPLICATION_CONTROLLER.equals(selected.getWrapped().getKind())
				|| ResourceKind.DEPLOYMENT_CONFIG.equals(selected.getWrapped().getKind())) {
			rc = (IReplicationController)selected.getWrapped();
			name = rc.getName();
			Object parent = selected.getParent();
			if(parent instanceof IServiceWrapper) {
				deployment = (IServiceWrapper)parent;
			} else if(parent instanceof IProjectWrapper) {
				deployment = findDeployment((IProjectWrapper)parent, rc);
			}
		}

		if(rc == null) {
			return null;
		}

		String diff = event.getParameter(REPLICA_DIFF);
		if(!NumberUtils.isNumber(diff)) {
			IDeploymentConfig dc = null;
			if(!(ResourceKind.DEPLOYMENT_CONFIG.equals(rc.getKind()))) {
				//if rc is deployment config, change will be permanent, we do not need a checkbox.
				String dcName = rc.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
				if(StringUtils.isNotBlank(dcName)) {
					dc = findDeploymentConfig(deployment, dcName);
				}
			}
			scaleUsing(event, name, rc, deployment, dc);
		} else {
			scaleDeployment(name, rc, rc.getDesiredReplicaCount() + Integer.parseInt(diff), null);
		}

		return null;
	}

	protected IReplicationController findReplicationController(IServiceWrapper deployment) {
		Collection<IResourceWrapper<?,?>> rcs = deployment.getResourcesOfKind(ResourceKind.REPLICATION_CONTROLLER);
		if(!rcs.isEmpty()) {
			//there should be only 1 per deployment, we'll assume this is true
			return (IReplicationController) rcs.iterator().next().getWrapped();
		}
		return null;
	}

	protected IDeploymentConfig findDeploymentConfig(IServiceWrapper deployment, String name) {
		return deployment.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG).stream()
				.map(c -> (IDeploymentConfig)c.getWrapped())
				.filter(dc -> name.equals(dc.getName())).findFirst().orElse(null);			
	}

	protected IServiceWrapper findDeployment(IProjectWrapper project, IReplicationController rc) {
		return project.getResourcesOfType(IServiceWrapper.class).stream()
				.filter(d -> contains(d, rc))
				.findFirst().orElse(null);
	}

	protected boolean contains(IServiceWrapper deployment, IReplicationController rc) {
		return deployment.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG).stream()
				.filter(cm -> rc.equals(cm.getWrapped()))
				.findFirst().orElse(null) != null;
	}

	protected void scaleUsing(ExecutionEvent event, String name, IReplicationController rc, IServiceWrapper deployment, IDeploymentConfig dc) {
		DialogResult result = showInputDialog(name, rc.getCurrentReplicaCount(), dc != null, event);
		if(result != null) {
			int replicas = result.getValue();
			if(!result.isMakePermanentOn()) {
				dc = null;
			}
			scaleDeployment(name, rc, replicas, dc);
		}
	}
	
	/**
	 * 
	 * @param name - the name of resource on which action is called
	 * @param rc - IReplicationController to modify
	 * @param replicas - new value
	 * @param dc - IDeploymentConfig is passed to make change permanent
	 */
	protected void scaleDeployment(final String name, final IReplicationController rc, final int replicas, final IDeploymentConfig dc) {
		if(replicas >= 0) {
			new Job(NLS.bind("Scaling {0} deployment ...", name)) {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						Connection conn = ConnectionsRegistryUtil.getConnectionFor(rc);
						if(dc != null) {
							//make change permanent
							dc.setDesiredReplicaCount(replicas);
//							conn.updateResource(dc);
						}
						if(dc == null || !hasConfigChangeTrigger(dc)) {
							rc.setDesiredReplicaCount(replicas);
						}
//						conn.updateResource(rc);
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

	public boolean hasConfigChangeTrigger(IDeploymentConfig dc) {
		return dc.getTriggers().stream()
				.anyMatch(t -> DeploymentTriggerType.CONFIG_CHANGE.equals(t.getType()));
	}

	protected <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		return UIUtils.getFirstElement(selection, klass);
	}
	
	protected DialogResult showInputDialog(String name, int current, boolean showCheckbox, ExecutionEvent event) {
		Shell shell = HandlerUtil.getActivePart(event).getSite().getShell();
		ScaleDeploymentDialog dialog = new ScaleDeploymentDialog(shell, name, current, showCheckbox);
		int result = dialog.open();
		if(Dialog.OK == result) {
			return new DialogResult(dialog.getValue(), showCheckbox && dialog.isMakePermanentOn());
		}
		return null;
	}

	public static class DialogResult {
		private int value = 1;
		private boolean makePermanent;

		public DialogResult(int value, boolean makePermanent) {
			this.value = value;
			this.makePermanent = makePermanent;
		}
	
		public int getValue() {
			return value;
		}

		public boolean isMakePermanentOn() {
			return makePermanent;
		}
	}
}

class ScaleDeploymentDialog extends TitleAreaDialog {
	static final String TITLE = "Scale deployment";
	static final String CHECKBOX_LABEL = "Make the change permanent";
	static final String SUB_TITLE = "Scale deployment {0}";
	static final String MESSAGE_1 = "Modify replicas number";
	static final String MESSAGE_2 = MESSAGE_1 + " and optionally select the change to be saved in deployment config";

	private String title;
	private boolean showCheckbox;

	private int initialValue;
	private int value = 1;
	private boolean makePermanent;

	public ScaleDeploymentDialog(Shell parentShell, String name, int initialValue, boolean showCheckbox) {
		super(parentShell);
		title = NLS.bind(SUB_TITLE, name);
		this.initialValue = value = initialValue;
		this.showCheckbox = showCheckbox;
		if(showCheckbox) {
			makePermanent = true; //defaults to true
		}
		setTitleImage(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
	}

	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		getButton(OK).setEnabled(false);
		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		((GridLayout)container.getLayout()).marginLeft = 10;

		parent.getShell().setText(TITLE);
		setMessage(showCheckbox ? MESSAGE_2 : MESSAGE_1);
		setTitle(title);

		addReplicas(container);

		if(showCheckbox) {
			addMakePermanent(container);
		}

		return container;
	}

	private void addReplicas(Composite container) {
		final Spinner replicas = new ScalingComponent().inline(true)
				.create(container).getSpinner();
		replicas.setSelection(value);

		replicas.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				value = replicas.getSelection();
				getButton(OK).setEnabled(value != initialValue);
			}
		});
	}

	private void addMakePermanent(Composite container) {
		final Button permanentCheckbox = new Button(container, SWT.CHECK);
		permanentCheckbox.setText(CHECKBOX_LABEL);
		permanentCheckbox.setSelection(makePermanent);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(permanentCheckbox);

		permanentCheckbox.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				makePermanent = permanentCheckbox.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	public int getValue() {
		return value;
	}

	public boolean isMakePermanentOn() {
		return makePermanent;
	}
}
