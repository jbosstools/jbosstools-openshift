package org.jboss.tools.openshift.express.internal.core.portforward;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
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
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.OpenShiftSSHOperationException;

public class ApplicationPortForwardingWizardPage extends AbstractOpenShiftWizardPage {

	private final ApplicationPortForwardingWizardModel wizardModel;

	private TableViewer viewer;

	private Button refreshButton;

	private Button startButton;

	private Button stopButton;

	public ApplicationPortForwardingWizardPage(final ApplicationPortForwardingWizardModel wizardModel,
			final ApplicationPortForwardingWizard applicationPortForwardingWizard) {
		super("Application port forward", null,
				"IApplicationPortForwardingingPage", applicationPortForwardingWizard);
		this.wizardModel = wizardModel;
		super.setDescription("Please configure port forwarding for the '" + wizardModel.getApplication().getName() + "' application");
	}

	@Override
	public boolean isPageComplete() {
		return true;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(parent);
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		Composite tableContainer = new Composite(container, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults().span(1, 3).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		// viewer.addSelectionChangedListener(onTableSelectionChanged());

		refreshButton = new Button(container, SWT.PUSH);
		refreshButton.setText("Refresh");
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefreshPorts());

		/*
		 * Button editButton = new Button(container, SWT.PUSH); editButton.setText("Edit...");
		 * GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(editButton); //
		 * checkAllButton.addSelectionListener(onCheckAll());
		 */

		startButton = new Button(container, SWT.PUSH);
		startButton.setText("Start All");
		startButton.setEnabled(true);
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(startButton);
		startButton.addSelectionListener(onStartPortForwarding());

		stopButton = new Button(container, SWT.PUSH);
		stopButton.setText("Stop All");
		stopButton.setEnabled(true);
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(stopButton);
		stopButton.addSelectionListener(onStopPortForwarding());

		// checkbox to use the default "127.0.0.1" local IP address
		final Button useLocalIpAddressButton = new Button(container, SWT.CHECK);
		useLocalIpAddressButton.setText("Use '127.0.0.1' as the local address for all Services");
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(useLocalIpAddressButton);
		final IObservableValue useLocalIpAddressObservable = BeanProperties.value(
				ApplicationPortForwardingWizardModel.PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS).observe(wizardModel);
		final IObservableValue useLocalIpAddressButtonSelection = WidgetProperties.selection().observe(
				useLocalIpAddressButton);
		dbc.bindValue(useLocalIpAddressButtonSelection, useLocalIpAddressObservable);
		useLocalIpAddressObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				refreshViewerInput();
			}
		});

		// checkbox to use the default "127.0.0.1" local IP address
		final Button findFreesPortButton = new Button(container, SWT.CHECK);
		findFreesPortButton.setText("Find free ports for all Services");
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(findFreesPortButton);
		final IObservableValue findFreePortsButtonObservable = BeanProperties.value(
				ApplicationPortForwardingWizardModel.PROPERTY_USE_FREE_PORTS).observe(wizardModel);
		final IObservableValue findFreePortsButtonSelection = WidgetProperties.selection().observe(findFreesPortButton);
		dbc.bindValue(findFreePortsButtonSelection, findFreePortsButtonObservable);
		findFreePortsButtonObservable.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				refreshViewerInput();
			}
		});

		// enabling/disabling controls
		IObservableValue portForwardingStartedObservable = BeanProperties.value(
				ApplicationPortForwardingWizardModel.PROPERTY_PORT_FORWARDING).observe(wizardModel);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(startButton))
				.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(stopButton))
				.notUpdating(portForwardingStartedObservable).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(useLocalIpAddressButton))
				.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(findFreesPortButton))
				.notUpdating(portForwardingStartedObservable).converting(new InvertingBooleanConverter()).in(dbc);

	}

	/*
	 * private ISelectionChangedListener onTableSelectionChanged() { return new ISelectionChangedListener() {
	 * @Override public void selectionChanged(SelectionChangedEvent event) { final IStructuredSelection selection =
	 * (IStructuredSelection) event.getSelection(); IApplicationPortForwarding portForward =
	 * (IApplicationPortForwarding) selection.getFirstElement(); if (portForward.isStarted()) {
	 * enableButton.setEnabled(false); disableButton.setEnabled(true); } else { enableButton.setEnabled(true);
	 * disableButton.setEnabled(false); } } }; }
	 */

	private SelectionListener onRefreshPorts() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Refreshing list of ports...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								wizardModel.verifyApplicationSSHSession();
								wizardModel.refreshForwardablePorts();
								refreshViewerInput();
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
								wizardModel.verifyApplicationSSHSession();
								wizardModel.startPortForwarding();
								refreshViewerInput();
							} catch (OpenShiftSSHOperationException e) {
								return OpenShiftUIActivator.createErrorStatus("Failed to start port-forwarding.", e);
							}
							
							try {
								if(wizardModel.getApplication().isPortFowardingStarted()) {
									List<IApplicationPortForwarding> forwardablePorts = wizardModel.getForwardablePorts();
									for (IApplicationPortForwarding portfwd : forwardablePorts) {
										if (!portfwd.isStarted(wizardModel.getApplication().getSSHSession())) {
											return OpenShiftUIActivator.createErrorStatus("Failed to start port-forwarding for one or more ports. See console for errors.");											
										}
									}
								} else {
									return OpenShiftUIActivator.createErrorStatus("Failed to start port-forwarding. See console for errors.");
								}
							} catch (OpenShiftSSHOperationException e) {
								return OpenShiftUIActivator.createErrorStatus("Problem checking port forward status during start. See console for errors.");
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
								wizardModel.verifyApplicationSSHSession();
								wizardModel.stopPortForwarding();
								refreshViewerInput();
							} catch (OpenShiftSSHOperationException e) {
								return OpenShiftUIActivator.createErrorStatus("Failed to stop port-forwarding.", e);
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

	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		TableViewer viewer = new TableViewer(table);
		// viewer.setComparer(new EqualityComparer());
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
		final Job j = new Job("Retrieving application's forwardable ports...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Checking Application SSH session...", 1);
					wizardModel.verifyApplicationSSHSession();
					monitor.worked(1);
					monitor.beginTask("Retrieving ports...", 1);
					wizardModel.loadForwardablePorts();
					refreshViewerInput();
					monitor.worked(1);
					return Status.OK_STATUS;
				} catch (OpenShiftSSHOperationException e) {
					return OpenShiftUIActivator.createErrorStatus(
							"Could not load forwardable ports for application ''{0}''", e, wizardModel.getApplication().getName());
				}
			}
		};

		getContainer().getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					IStatus status = WizardUtils.runInWizard(j, getContainer(), getDataBindingContext());
					if(!status.isOK()) {
						getWizard().getContainer().getShell().close();
					}
				} catch(Exception e) {
					// ignore
				}
			}
		});
	}

	private void refreshViewerInput() {
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

}
