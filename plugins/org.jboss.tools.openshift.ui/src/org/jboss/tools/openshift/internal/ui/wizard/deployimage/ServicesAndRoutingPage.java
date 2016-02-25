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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.restclient.model.IServicePort;

/**
 * Page to configure OpenShift services and routes
 * 
 * @author jeff.cantrill
 *
 */
public class ServicesAndRoutingPage extends AbstractOpenShiftWizardPage  {
	private static final String PAGE_NAME = "Services && Routing Settings Page";
	private static final String PAGE_TITLE = "Services && Routing Settings";
	private static final String PAGE_DESCRIPTION = "";
	private IServiceAndRoutingPageModel model;

	TableViewer portsViewer;
	
	protected ServicesAndRoutingPage(IWizard wizard, IServiceAndRoutingPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard);
		this.model = model;
	}
	
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);
		createExposedPortsControl(parent, dbc);
		
		//routing
		Composite routingContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.grab(true, false)
			.applyTo(routingContainer);
		GridLayoutFactory.fillDefaults()
			.margins(6, 6)
			.numColumns(2)
			.applyTo(routingContainer);
		
		Button btnAddRoute = new Button(routingContainer, SWT.CHECK);
		btnAddRoute.setText("Add Route");
		btnAddRoute.setToolTipText("Adding a route to the service will make the image accessible\noutside of the OpenShift cluster.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(btnAddRoute);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(btnAddRoute))
			.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE)
			.observe(model))
			.in(dbc);

	}

	private void createExposedPortsControl(Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Service Ports:");
		label.setToolTipText("The exposed ports of the image.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,1)
			.applyTo(label);
		Composite tableContainer = new Composite(container, SWT.NONE);
		
		portsViewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(portsViewer))
				.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_SELECTED_SERVICE_PORT).observe(model))
				.in(dbc);
		portsViewer.setContentProvider(new ObservableListContentProvider());
		IObservableList portsObservable = BeanProperties.list(
				IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS).observe(model);
		portsViewer.setInput(portsObservable);
		dbc.addValidationStatusProvider(new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				if(portsObservable.isEmpty()) {
					return ValidationStatus.error("At least 1 port is required when generating the service for the deployed image");
				}
				return Status.OK_STATUS;
			}
		});
	
		Button btnAdd = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnAdd);
		btnAdd.setText("Add...");
		btnAdd.setToolTipText("Add a port to be exposed by the service which is not explicilty declared by the image.");
		btnAdd.addSelectionListener(onAdd());

		Button btnEdit = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnEdit);
		btnEdit.setText("Edit...");
		btnEdit.setToolTipText("Edit a port to be exposed by the service.");
		btnEdit.addSelectionListener(new EditHandler());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(btnEdit))
			.notUpdatingParticipant()
			.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_SELECTED_SERVICE_PORT).observe(model))
			.converting(new IsNotNull2BooleanConverter())
			.in(dbc);

		Button removeButton = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.setToolTipText("Remove a port that will be exposed by the service.");
		removeButton.addSelectionListener(onRemove());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(removeButton))
			.notUpdatingParticipant()
			.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_SELECTED_SERVICE_PORT).observe(model))
			.converting(new IsNotNull2BooleanConverter())
			.in(dbc);

		Button btnReset = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnReset);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Resets the list of ports to the exposed ports of the image.");
		btnReset.addSelectionListener(onReset());
		
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String message = "Add a port to be exposed by the service";
				ServicePortAdapter port = new ServicePortAdapter();
				List<IServicePort> servicePorts = model.getServicePorts();
				if(!servicePorts.isEmpty()) {
					port.setPort(Collections.max(servicePorts, new Comparator<IServicePort>() {
						
						@Override
						public int compare(IServicePort first, IServicePort second) {
							return Integer.compare(first.getPort(), second.getPort());
						}
						
					}).getPort() + 1);
					port.setTargetPort(Collections.max(servicePorts, new Comparator<IServicePort>() {
						
						@Override
						public int compare(IServicePort first, IServicePort second) {
							return first.getTargetPort().compareTo(second.getTargetPort());
						}
						
					}).getPort() + 1);
				}
				ServicePortDialog dialog = new ServicePortDialog(port, message, servicePorts);
				if(Window.OK == dialog.open()) {
					port.setName(NLS.bind("{0}-tcp", port.getPort()));
					model.addServicePort(port);
				}
			}
			
		};
	}
	
	class EditHandler extends SelectionAdapter implements IDoubleClickListener{
		
		
		@Override
		public void doubleClick(DoubleClickEvent event) {
			handleEvent();
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			handleEvent();
		}
		
		public void handleEvent(){
			String message = "Edit the port to be exposed by the service";
			final IServicePort port = model.getSelectedServicePort();
			final ServicePortAdapter target = new ServicePortAdapter(port);
			ServicePortDialog dialog = new ServicePortDialog(target, message, model.getServicePorts());
			if(Window.OK == dialog.open()) {
				target.setName(NLS.bind("{0}-tcp", target.getPort()));
				model.updateServicePort(port, target);
				model.setSelectedServicePort(target);
			}
		}
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IServicePort>() {
					@Override
					public String getValue(IServicePort port) {
						return port.getName();
					}
				})
				.name("Name").align(SWT.LEFT).weight(2).minWidth(50).buildColumn()
				.column(new IColumnLabelProvider<IServicePort>() {
					@Override
					public String getValue(IServicePort port) {
						return "" + port.getPort();
					}
				})
				.name("Service Port").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
				.column(new IColumnLabelProvider<IServicePort>() {
					@Override
					public String getValue(IServicePort port) {
						return port.getTargetPort();
					}
				})
				.name("Pod Port").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
				.buildViewer();
		viewer.addDoubleClickListener(new EditHandler());
		return viewer;
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IServicePort port = UIUtils.getFirstElement(portsViewer.getSelection(), IServicePort.class);
				if(MessageDialog.openQuestion(getShell(), "Remove port", NLS.bind("Are you sure you want to delete the port {0}?", port.getPort()))) {
					model.removeServicePort(port);
					portsViewer.refresh();
				}
			}
			
		};
	}

	private SelectionListener onReset() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), "Reset ports", "Are you sure you want to reset the serviced ports to those exposed by the image?")) {
					model.resetServicePorts();
					portsViewer.refresh();
				}
			}
			
		};
	}

}
