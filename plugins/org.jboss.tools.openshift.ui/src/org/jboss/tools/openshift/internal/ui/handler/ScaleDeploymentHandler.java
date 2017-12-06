/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.stream.Collectors;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;
import org.jboss.tools.openshift.internal.ui.utils.ResourceWrapperUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.api.capabilities.IScalable;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * Handle for scaling deployments
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class ScaleDeploymentHandler extends AbstractHandler {

	public static final String REPLICA_DIFF = "org.jboss.tools.openshift.ui.command.deployment.scale.replicadiff";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDeploymentConfig dc = getDeploymentConfig(getSelectedElement(event, IResourceWrapper.class));
		if (dc == null) {
			IResource resource = ResourceWrapperUtils
					.getResource(UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event)));
			return OpenShiftUIActivator.statusFactory()
					.errorStatus(NLS.bind("Could not scale {0}: Could not find deployment config",
							resource == null ? "" : resource.getName()));
		}
		scaleUsing(event, dc, dc.getName());
		return null;
	}

	protected <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
		ISelection selection = UIUtils.getCurrentSelection(event);
		return UIUtils.getFirstElement(selection, klass);
	}

	private IDeploymentConfig getDeploymentConfig(IResourceWrapper<?, ?> wrapper) {
		if (wrapper == null) {
			return null;
		}

		IDeploymentConfig dc = null;
		IResource wrapped = wrapper.getWrapped();
		if (wrapper instanceof IServiceWrapper) {
			// service selected
			dc = getDeploymentConfig((IServiceWrapper) wrapper);
		} else if (wrapped instanceof IPod) {
			// pod selected
			dc = getDeploymentConfig((IPod) wrapped, wrapper);
		} else if (wrapped instanceof IDeploymentConfig) {
			// deployment config selected
			// has to be tested before IReplicationController, IDeploymentConfig extends IReplicationController
			dc = (IDeploymentConfig) wrapped;
		} else if (wrapped instanceof IReplicationController) {
			// replication controller selected (deployment tab in properties)
			// has to be tested after IDeploymentConfig, IDeploymentConfig extends IReplicationController
			dc = getDeploymentConfig((IReplicationController) wrapped, wrapper);
		}
		return dc;
	}

	private IDeploymentConfig getDeploymentConfig(IReplicationController rc, IResourceWrapper<?, ?> wrapper) {
		IDeploymentConfig dc = null;
		IServiceWrapper service = ResourceWrapperUtils.getServiceWrapperFor(wrapper,
				serviceWrapper -> ResourceUtils.areRelated(rc, (IService) serviceWrapper.getWrapped()));
		if (service != null) {
			dc = ResourceUtils.getDeploymentConfigFor(rc,
					ResourceWrapperUtils.getResources(service.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG)));
		}
		return dc;
	}

	private IDeploymentConfig getDeploymentConfig(IPod pod, IResourceWrapper<?, ?> wrapper) {
		IDeploymentConfig dc = null;
		if (!ResourceUtils.isBuildPod(pod)) {
			IServiceWrapper service = ResourceWrapperUtils.getServiceWrapperFor(wrapper,
					serviceWrapper -> ResourceUtils.areRelated(pod, (IService) serviceWrapper.getWrapped()));
			dc = getDeploymentConfig(service);
		}
		return dc;
	}

	private IDeploymentConfig getDeploymentConfig(IServiceWrapper service) {
		return ResourceUtils.getLatestResourceVersion(service.getResourcesOfKind(ResourceKind.DEPLOYMENT_CONFIG)
				.stream().map(wrapper -> (IDeploymentConfig) wrapper.getWrapped())
				.collect(Collectors.<IDeploymentConfig>toList()));
	}

	protected void scaleUsing(ExecutionEvent event, IReplicationController rc, String name) {
		final int requestedReplicas = getRequestedReplicas(rc, name, event);
		final int currentReplicas = rc.getCurrentReplicaCount();
		if (requestedReplicas != -1 && currentReplicas != requestedReplicas) {
			if (requestedReplicas == 0 && !showStopDeploymentWarning(name, HandlerUtil.getActiveShell(event))) {
				return;
			}
			scaleDeployment(event, name, rc, requestedReplicas);
		}
	}

	protected boolean showStopDeploymentWarning(String name, Shell shell) {
		MessageDialog dialog = new MessageDialog(shell, "Stop all deployments?",
				OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_ICON_IMG,
				NLS.bind(
						"Are you sure you want to scale {0} to 0 replicas?\nThis will stop all pods for the deployment.",
						name),
				MessageDialog.WARNING, 1, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL });
		return dialog.open() == Dialog.OK;
	}

	protected void scaleDeployment(ExecutionEvent event, String name, IReplicationController rc, int replicas) {
		if (replicas >= 0) {
			new Job(NLS.bind("Scaling {0} deployment to {1}...", name, replicas)) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						return rc.accept(new CapabilityVisitor<IScalable, IStatus>() {

							@Override
							public IStatus visit(IScalable capability) {
								capability.scaleTo(replicas);
								return Status.OK_STATUS;
							}

						}, new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Scaling is not supported for this resource"));
					} catch (Exception e) {
						String message = NLS.bind("Unable to scale {0}", name);
						OpenShiftUIActivator.getDefault().getLogger().logError(message, e);
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, e);
					}
				}

			}.schedule();
		}
	}

	private int getRequestedReplicas(IReplicationController rc, String name, ExecutionEvent event) {
		String diff = event.getParameter(REPLICA_DIFF);
		int currentReplicas = rc.getCurrentReplicaCount();
		if (NumberUtils.isNumber(diff)) {
			return currentReplicas + Integer.parseInt(diff);
		} else {
			return showScaleReplicasDialog(name, currentReplicas, HandlerUtil.getActiveShell(event));
		}
	}

	protected int showScaleReplicasDialog(String name, int currentReplicas, Shell shell) {
		ScaleReplicasDialog dialog = new ScaleReplicasDialog(currentReplicas, name, shell);
		if (dialog.open() == Dialog.OK) {
			return dialog.getRequestedReplicas();
		}
		return -1;
	}

	public class ScaleReplicasDialog extends TitleAreaDialog {

		private int currentReplicas;
		private IObservableValue<Integer> requestedReplicas;
		private String name;

		public ScaleReplicasDialog(int currentReplicas, String name, Shell parentShell) {
			super(parentShell);
			this.currentReplicas = currentReplicas;
			this.requestedReplicas = new WritableValue<Integer>(currentReplicas, Integer.class);
			this.name = name;
		}

		@Override
		protected Control createContents(Composite parent) {
			Control control = super.createContents(parent);
			setupDialog(name, parent.getShell());
			return control;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			DataBindingContext dbc = new DataBindingContext();

			Label titleSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(titleSeparator);

			final Composite dialogArea = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 20).spacing(20, SWT.DEFAULT).applyTo(dialogArea);

			// scale label
			Label scaleLabel = new Label(dialogArea, SWT.NONE);
			scaleLabel.setText("Scale to number of replicas:");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(scaleLabel);

			// scale spinner
			Spinner scaleSpinner = new Spinner(dialogArea, SWT.BORDER);
			scaleSpinner.setMinimum(0);
			scaleSpinner.setSelection(currentReplicas);
			scaleSpinner.setPageIncrement(1);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(scaleSpinner);

			ValueBindingBuilder.bind(WidgetProperties.selection().observe(scaleSpinner))
					.validatingAfterConvert(new IValidator() {

						@Override
						public IStatus validate(Object value) {
							if (!(value instanceof Integer)) {
								return ValidationStatus.error(NLS.bind(
										"You need to provide a positive number of replicas for deployment {0}", name));
							}
							int requestedReplicas = (int) value;
							if (requestedReplicas == currentReplicas) {
								return ValidationStatus.cancel("");
							}
							if (requestedReplicas == 0) {
								return ValidationStatus
										.warning(NLS.bind("Scaling to 0 replicas will stop all pods.", name));
							}
							return ValidationStatus.ok();
						}
					}).to(requestedReplicas).in(dbc);

			new FormPresenterSupport(new FormPresenterSupport.IFormPresenter() {

				@Override
				public void setMessage(String message, int type) {
					ScaleReplicasDialog.this.setMessage(message, type);
				}

				@Override
				public void setComplete(boolean complete) {
					Button button = ScaleReplicasDialog.this.getButton(IDialogConstants.OK_ID);
					if (!DisposeUtils.isDisposed(button)) {
						button.setEnabled(complete);
					}
				}

				@Override
				public Control getControl() {
					return dialogArea;
				}
			}, dbc);

			return dialogArea;
		}

		private void setupDialog(String name, Shell shell) {
			shell.setText("Scale Deployments");
			setTitle(NLS.bind("Enter the desired number of replicas for deployment {0}", name));
			setTitleImage(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
			setHelpAvailable(false);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			okButton.setEnabled(false); // initially disable ok since scaling set to current replicas 
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}

		/**
		 * Returns the requested number of replicas. Returns -1 if the user cancelled the dialog.
		 * @return
		 */
		public int getRequestedReplicas() {
			if (Dialog.OK == getReturnCode()) {
				return requestedReplicas.getValue();
			} else {
				return -1;
			}
		}

	}

}
