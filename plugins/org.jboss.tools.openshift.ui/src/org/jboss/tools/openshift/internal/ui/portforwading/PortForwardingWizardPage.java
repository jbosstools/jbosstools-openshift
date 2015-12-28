/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.portforwading;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
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
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.resources.IPortForwardable;


/**
 * @author jeff.cantrill
 */
public class PortForwardingWizardPage extends AbstractOpenShiftWizardPage {
	
	private static final IPluginLog LOG = OpenShiftUIActivator.getDefault().getLogger();
	private final PortForwardingWizardModel wizardModel;
	private TableViewer viewer;
	private Button startButton;
	private Button stopButton;

	public PortForwardingWizardPage(final PortForwardingWizardModel wizardModel, final PortForwardingWizard portForwardingWizard) {
		super("Port forwarding", null,
				"PortForwardingWizardPage", portForwardingWizard);
		this.wizardModel = wizardModel;
		setDescription(NLS.bind("Port forwarding for the {0} pod.", wizardModel.getPodName()));
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

		startButton = new Button(container, SWT.PUSH);
		startButton.setText("Start All");
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(startButton);
		startButton.addSelectionListener(onStartPortForwarding());

		stopButton = new Button(container, SWT.PUSH);
		stopButton.setText("Stop All");
		GridDataFactory.fillDefaults().hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(stopButton);
		stopButton.addSelectionListener(onStopPortForwarding());

		final Button findFreesPortButton = new Button(container, SWT.CHECK);
		findFreesPortButton.setText("Find free local ports for remote ports");
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

		IObservableValue portForwardingAllowedObservable = BeanProperties.value(
				PortForwardingWizardModel.PROPERTY_PORT_FORWARDING_ALLOWED).observe(wizardModel);

		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(startButton))
			.notUpdating(portForwardingAllowedObservable).in(dbc);

		
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(stopButton))
				.notUpdating(portForwardingStartedObservable).in(dbc);
		
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(findFreesPortButton))
				.notUpdating(portForwardingAllowedObservable).in(dbc);

	}


	private SelectionListener onStartPortForwarding() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Starting port forwarding...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							wizardModel.startPortForwarding();
							refreshViewerInput(wizardModel.getForwardablePorts());
							return Status.OK_STATUS;
						}
					}, getContainer(), getDataBindingContext());
				} catch (OpenShiftException | InvocationTargetException | InterruptedException e1) {
					LOG.logError(e1);
					MessageDialog.openError(getShell(), "Error starting port forwarding", e1.getMessage());
				}
			}
		};
	}

	private SelectionListener onStopPortForwarding() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(new Job("Stopping port forwarding...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							wizardModel.stopPortForwarding();
							refreshViewerInput(wizardModel.getForwardablePorts());
							return Status.OK_STATUS;
						}
					}, getContainer(), getDataBindingContext());
				} catch (OpenShiftException | InvocationTargetException | InterruptedException e1) {
					LOG.logError(e1);
					MessageDialog.openError(getShell(), "Error stopping port forwarding", e1.getMessage());
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

		createTableColumn("Name", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IPortForwardable.PortPair port = (IPortForwardable.PortPair) cell.getElement();
				cell.setText(StringUtils.defaultIfBlank(port.getName(), ""));
			}

		}, viewer, tableLayout);

		createTableColumn("Local Port", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IPortForwardable.PortPair port = (IPortForwardable.PortPair) cell.getElement();
				cell.setText(Integer.toString(port.getLocalPort()));
			}
		}, viewer, tableLayout);

		createTableColumn("Remote Port", 2, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IPortForwardable.PortPair port = (IPortForwardable.PortPair) cell.getElement();
				cell.setText(Integer.toString(port.getRemotePort()));
			}
		}, viewer, tableLayout);

		createTableColumn("Status", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
					final boolean started = wizardModel.getPortForwarding();
					cell.setText(started ? "Started" : "Stopped");
			}
		}, viewer, tableLayout);

		IObservableValue forwardablePortsModelObservable =
				BeanProperties.value(PortForwardingWizardModel.PROPERTY_FORWARDABLE_PORTS).observe(wizardModel);

		final ForwardablePortListValidator validator =
				new ForwardablePortListValidator(forwardablePortsModelObservable);
		dbc.addValidationStatusProvider(validator);
		
		viewer.setInput(wizardModel.getForwardablePorts());
//		
		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	private void refreshViewerInput(Collection<IPortForwardable.PortPair> ports) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					viewer.setInput(wizardModel.getForwardablePorts());
				} catch (Exception e) {
					LOG.logError("Failed to refresh table content with list of ports for selected pod", e);
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
			final Collection<IPortForwardable.PortPair> ports = (Collection<IPortForwardable.PortPair>) viewerObservable.getValue();
			if(ports == null || ports.isEmpty()) {
				return ValidationStatus.error(
						NLS.bind("There are no available ports to forward to {0}.\nYour pod may not be running or does not expose any ports.", 
								wizardModel.getPodName()));
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
