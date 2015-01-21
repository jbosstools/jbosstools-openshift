/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.portforward;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.internal.common.ui.AbstractOpenShiftWizardPage;

import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 */
public class PortForwardingWizardPage extends AbstractOpenShiftWizardPage {

	private final PortForwardingWizardModel wizardModel;
	private TableViewer viewer;
	private Button refreshButton;
	private Button startButton;
	private Button stopButton;

	public PortForwardingWizardPage(final PortForwardingWizardModel wizardModel,
			final PortForwardingWizard portForwardingWizard) {
		super("Application port forwarding", null,
				"IApplicationPortForwardingingPage", portForwardingWizard);
		this.wizardModel = wizardModel;
		setDescription(NLS.bind("Please configure port forwarding for the {0} application", wizardModel.getApplication().getName()));
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(parent);
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		Composite tableContainer = new Composite(container, SWT.NONE);
		this.viewer = createTable(tableContainer, dbc);
		GridDataFactory.fillDefaults().span(1, 3).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);

		refreshButton = new Button(container, SWT.PUSH);
		refreshButton.setText("Refresh");
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefreshPorts());

		startButton = new Button(container, SWT.PUSH);
		startButton.setText("Start All");
		startButton.setEnabled(wizardModel.hasForwardablePorts()); 
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(startButton);
		startButton.addSelectionListener(onStartPortForwarding());

		stopButton = new Button(container, SWT.PUSH);
		stopButton.setText("Stop All");
		stopButton.setEnabled(false);
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(stopButton);
		stopButton.addSelectionListener(onStopPortForwarding());

		// checkbox to use the default "127.0.0.1" local IP address
		final Button useLocalIpAddressButton = new Button(container, SWT.CHECK);
		useLocalIpAddressButton.setText("Use '127.0.0.1' as the local address for all Services");
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(useLocalIpAddressButton);
		final IObservableValue useLocalIpAddressObservable = BeanProperties.value(
				PortForwardingWizardModel.PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS).observe(wizardModel);
		final IObservableValue useLocalIpAddressButtonSelection = WidgetProperties.selection().observe(
				useLocalIpAddressButton);
		dbc.bindValue(useLocalIpAddressButtonSelection, useLocalIpAddressObservable);
		useLocalIpAddressObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				refreshViewerInput(wizardModel.getForwardablePorts());
			}
		});

		// checkbox to use the default "127.0.0.1" local IP address
		final Button findFreesPortButton = new Button(container, SWT.CHECK);
		findFreesPortButton.setText("Find free ports for all Services");
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(findFreesPortButton);
		final IObservableValue findFreePortsButtonObservable = BeanProperties.value(
				PortForwardingWizardModel.PROPERTY_USE_FREE_PORTS).observe(wizardModel);
		final IObservableValue findFreePortsButtonSelection = WidgetProperties.selection().observe(findFreesPortButton);
		dbc.bindValue(findFreePortsButtonSelection, findFreePortsButtonObservable);
		findFreePortsButtonObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				refreshViewerInput(wizardModel.getForwardablePorts());
			}
		});

		// enabling/disabling controls
		IObservableValue portForwardingStartedObservable = BeanProperties.value(
				PortForwardingWizardModel.PROPERTY_PORT_FORWARDING).observe(wizardModel);
		
		IObservableValue forwardablePortsExistObservable = BeanProperties.value(
				PortForwardingWizardModel.PROPERTY_FORWARDABLE_PORTS).observe(wizardModel);
		
		
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(startButton))
			.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);

		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(startButton))
				.notUpdating(forwardablePortsExistObservable).converting(new Converter(List.class, Boolean.class) {
					
					@Override
					public Object convert(Object fromObject) {
						if(fromObject instanceof List<?>) {
							return !((List<?>)fromObject).isEmpty();
						}
						return Boolean.FALSE;
					}
				}).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(stopButton))
				.notUpdating(portForwardingStartedObservable).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(useLocalIpAddressButton))
				.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(findFreesPortButton))
				.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);

	}

	private SelectionListener onRefreshPorts() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Refreshing list of ports...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								if(!wizardModel.verifyApplicationSSHSession()) {
									return Status.CANCEL_STATUS;
								}
								wizardModel.refreshForwardablePorts();
								refreshViewerInput(wizardModel.getForwardablePorts());
							} catch (OpenShiftSSHOperationException e) {
								Logger.error("Failed to refresh list of ports", e);
							}
							return Status.OK_STATUS;
						}
					}, getContainer(), getDataBindingContext());
				} catch (Exception exception) {
					// ignore
				}
			}
		};
	}

	private SelectionListener onStartPortForwarding() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Starting Port-forwarding...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								if(!wizardModel.verifyApplicationSSHSession()) {
									return Status.CANCEL_STATUS;
								}

								wizardModel.startPortForwarding();
								refreshViewerInput(wizardModel.getForwardablePorts());
							} catch (OpenShiftSSHOperationException e) {
								return ExpressUIActivator.createErrorStatus("Failed to start port-forwarding.", e);
							}
							
							try {
								if(wizardModel.getApplication().isPortFowardingStarted()) {
									List<IApplicationPortForwarding> forwardablePorts = wizardModel.getForwardablePorts();
									for (IApplicationPortForwarding portfwd : forwardablePorts) {
										if (!portfwd.isStarted(wizardModel.getApplication().getSSHSession())) {
											return ExpressUIActivator.createErrorStatus("Failed to start port-forwarding for one or more ports. See console for errors.");											
										}
									}
								} else {
									return ExpressUIActivator.createErrorStatus("Failed to start port-forwarding. See console for errors.");
								}
							} catch (OpenShiftSSHOperationException e) {
								return ExpressUIActivator.createErrorStatus("Problem checking port forward status during start. See console for errors.");
							}
							
							return Status.OK_STATUS;
						}
					}, getContainer(), getDataBindingContext());
				} catch (Exception exception) {
					// ignore
				}
			}
		};
	}

	private SelectionListener onStopPortForwarding() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Stoppping Port-forwarding...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								if(!wizardModel.verifyApplicationSSHSession()) {
									return Status.CANCEL_STATUS;
								}

								wizardModel.stopPortForwarding();
								refreshViewerInput(wizardModel.getForwardablePorts());
							} catch (OpenShiftSSHOperationException e) {
								return ExpressUIActivator.createErrorStatus("Failed to stop port-forwarding.", e);
							}
							return Status.OK_STATUS;
						}
					}, getContainer(), getDataBindingContext());
				} catch (Exception exception) {
					// ignore
				}
			}
		};
	}

	protected TableViewer createTable(Composite tableContainer, DataBindingContext dbc) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());

		createTableColumn("Service", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				cell.setText(port.getName());
			}

		}, viewer, tableLayout);

		createTableColumn("Local Address", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				cell.setText(port.getLocalAddress());
			}
		}, viewer, tableLayout);

		createTableColumn("Local Port", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				cell.setText(Integer.toString(port.getLocalPort()));
			}
		}, viewer, tableLayout);

		createTableColumn("Remote Address", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				cell.setText(port.getRemoteAddress());
			}
		}, viewer, tableLayout);

		createTableColumn("Remote Port", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				cell.setText(Integer.toString(port.getRemotePort()));
			}
		}, viewer, tableLayout);

		createTableColumn("Status", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IApplicationPortForwarding port = (IApplicationPortForwarding) cell.getElement();
				try {
					final boolean started = port.isStarted(wizardModel.getApplication().getSSHSession());
					cell.setText(started ? "Started" : "Stopped");
				} catch (OpenShiftSSHOperationException e) {
					cell.setText("Unknown");
				}
			}
		}, viewer, tableLayout);

		IObservableValue forwardablePortsModelObservable =
				BeanProperties.value(PortForwardingWizardModel.PROPERTY_FORWARDABLE_PORTS)
						.observe(wizardModel);
		
		final ForwardablePortListValidator validator =
				new ForwardablePortListValidator(forwardablePortsModelObservable);
		dbc.addValidationStatusProvider(validator);

		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		final Job j = new Job("Loading application's forwardable ports...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Checking Application SSH session...", 1);
					wizardModel.verifyApplicationSSHSession();
					monitor.worked(1);
					monitor.beginTask("Retrieving ports...", 1);
					wizardModel.loadForwardablePorts();
					refreshViewerInput(wizardModel.getForwardablePorts());
					monitor.worked(1);
					return Status.OK_STATUS;
				} catch (OpenShiftSSHOperationException e) {
					return ExpressUIActivator.createErrorStatus(
							"Could not load forwardable ports for application ''{0}''", e, wizardModel.getApplication().getName());
				}
			}
		};

		try {
			WizardUtils.runInWizard(j, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			Logger.error("Failed to load application's forwardable ports", e);
		}
	}

	private void refreshViewerInput(List<IApplicationPortForwarding> ports) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					viewer.setInput(wizardModel.getForwardablePorts());
				} catch (Exception e) {
					Logger.error("Failed to refresh table content with list of ports for selected application", e);
				}
			}
		});
	}
	
	class ForwardablePortListValidator extends MultiValidator {

		private final IObservableValue viewerObservable;
		
		public ForwardablePortListValidator(IObservableValue viewerObservable) {
			this.viewerObservable = viewerObservable;
		}

		@Override
		protected IStatus validate() {
			@SuppressWarnings("unchecked")
			final List<IApplicationPortForwarding> ports = (List<IApplicationPortForwarding>) viewerObservable.getValue();
			if(ports == null || ports.isEmpty()) {
				return ValidationStatus.error("There are no available ports to forward for this application.\nYour application may be stopped.");
			}
			return Status.OK_STATUS;
		}
		
	}

	@Override
	public boolean isPageComplete() {
		// enable OK button even when binding validation errors exist 
		return true;
	}

}
