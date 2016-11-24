/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.TableCellMouseAdapter;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;

import com.openshift.restclient.model.IServicePort;

/**
 * Page to configure OpenShift services and routes
 * 
 * @author jeff.cantrill
 * @author Jeff Maury
 *
 */
public class ServicesAndRoutingPage extends AbstractOpenShiftWizardPage  {
	private static final String PAGE_NAME = "Services && Routing Settings Page";
	private static final String PAGE_TITLE = "Services && Routing Settings";
	private static final String PAGE_DESCRIPTION = "";
	private static final int ROUTE_PORT_COLUMN_INDEX = 3;
	private IServiceAndRoutingPageModel model;

	TableViewer portsViewer;
	
	public ServicesAndRoutingPage(IWizard wizard, IServiceAndRoutingPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard);
		this.model = model;
	}
	
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);
		createExposedPortsControl(parent, dbc);
		
	    GridDataFactory
	        .fillDefaults()
	        .align(SWT.FILL, SWT.BEGINNING)
	        .grab(true, false)
	        .applyTo(new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL));

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
		btnAddRoute.setToolTipText("Adding a route to the service will make the image accessible\noutside of the OpenShift cluster on all the available service ports. \nYou can target a specific port by editing the route later.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, false).span(2, 1).applyTo(btnAddRoute);
		final IObservableValue<Boolean> addRouteModelObservable = 
				BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE).observe(model);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(btnAddRoute))
			.to(addRouteModelObservable)
			.in(dbc);
		
		Label labelRouteHostname = new Label(routingContainer, SWT.NONE);
		labelRouteHostname.setText("Hostname:");
	    GridDataFactory.fillDefaults()
	    	.align(SWT.FILL, SWT.CENTER)
	    	.applyTo(labelRouteHostname);

		Text textRouteHostname = new Text(routingContainer, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(textRouteHostname);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(textRouteHostname))
        	.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE)
        	.observe(model))
        	.in(dbc);
		final IObservableValue<String> routeHostnameObservable = 
				WidgetProperties.text(SWT.Modify).observe(textRouteHostname);
		ValueBindingBuilder
			.bind(routeHostnameObservable)
			.converting(new TrimmingStringConverter())
			.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ROUTE_HOSTNAME).observe(model))
			.in(dbc);
        
        MultiValidator validator = new MultiValidator() {

			@Override
			protected IStatus validate() {
				IStatus status = ValidationStatus.ok();
				boolean isAddRoute = addRouteModelObservable.getValue();
				String hostName = routeHostnameObservable.getValue();
				final IObservableList<IServicePort> portsObservable = BeanProperties.list(
		                IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS).observe(model);
		        final IServicePort routingPort = 
		                (IServicePort) BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ROUTING_PORT).observe(model).getValue();

				if (isAddRoute) {
					if (StringUtils.isBlank(hostName)) {
						status = ValidationStatus
								.info(NLS.bind(OpenShiftUIMessages.EmptyHostNameErrorMessage, hostName));
					} else if (!DomainValidator.getInstance(true).isValid(hostName)) {
						status = ValidationStatus
								.error(NLS.bind(OpenShiftUIMessages.InvalidHostNameErrorMessage, hostName));
					}
					if (!status.matches(IStatus.ERROR) && isAddRoute && (portsObservable.size() > 1) && (routingPort == null)) {
					    if (status.matches(IStatus.INFO)) {
	                        status = ValidationStatus.info(status.getMessage() + "\n " + OpenShiftUIMessages.RoundRobinRoutingMessage);
					    } else {
					        status = ValidationStatus.info(OpenShiftUIMessages.RoundRobinRoutingMessage);
					    }
					}
				}
				return status;
			}
		};
        dbc.addValidationStatusProvider(validator);
        ControlDecorationSupport.create(validator, SWT.LEFT | SWT.TOP);
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
		
        IObservableList<IServicePort> portsObservable = BeanProperties.list(
                IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS).observe(model);
		portsViewer = createTable(tableContainer);

		ObservableListContentProvider contentProvider = new ObservableListContentProvider();
		portsViewer.setContentProvider(contentProvider);
		ObservableMapLabelProvider labelProvider = 
		        new ObservableMapLabelProvider(
		                Properties.observeEach(contentProvider.getKnownElements(),
		                                       BeanProperties.values(ServicePortAdapter.NAME,
		                                                             ServicePortAdapter.PORT,
		                                                             ServicePortAdapter.TARGET_PORT,
		                       /* ROUTE_PORT_COLUMN_INDEX = 3 */     ServicePortAdapter.ROUTE_PORT))) {
		    @Override
		    public Image getColumnImage(Object element, int columnIndex) {
		        if (columnIndex == ROUTE_PORT_COLUMN_INDEX) {
		        	Object selected = attributeMaps[columnIndex].get(element);
		        	return selected != null && (boolean)selected ? OpenShiftImages.CHECKED_IMG : OpenShiftImages.UNCHECKED_IMG;
		        }
		        return null;
		    }

		    @Override
		    public String getColumnText(Object element, int columnIndex) {
		        if (columnIndex != ROUTE_PORT_COLUMN_INDEX) {
		            Object result = attributeMaps[columnIndex].get(element);
		            return result == null ? "" : result.toString(); //$NON-NLS-1$
		        }
		        return null;
		    }		    
		};
		portsViewer.setLabelProvider(labelProvider);
		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(portsViewer))
				.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_SELECTED_SERVICE_PORT).observe(model))
				.in(dbc);
		
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

		portsViewer.getTable().addMouseListener(onTableCellClicked());
	
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
		UIUtils.setDefaultButtonWidth(btnEdit);

		Button btnReset = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnReset);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Resets the list of ports to the exposed ports of the image.");
		btnReset.addSelectionListener(onReset());
		UIUtils.setDefaultButtonWidth(btnReset);
		
	}

	private MouseListener onTableCellClicked() {
		return new TableCellMouseAdapter(ROUTE_PORT_COLUMN_INDEX) {
			
			@Override
			public void mouseUpCell(MouseEvent event) {
				IServicePort port = model.getSelectedServicePort();
				ServicePortAdapter target = new ServicePortAdapter((ServicePortAdapter)port);
				target.setRoutePort(!target.isRoutePort());
				target.setName(NLS.bind("{0}-tcp", target.getPort()));
				model.updateServicePort(port, target);
				model.setSelectedServicePort(target);
				Display.getDefault().asyncExec(() -> {
					if(portsViewer != null && portsViewer.getTable() != null && !portsViewer.getTable().isDisposed()) {
						portsViewer.refresh();
					}
				});
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
			final ServicePortAdapter target = new ServicePortAdapter((ServicePortAdapter)port);
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
				.column("Name").align(SWT.LEFT).weight(2).minWidth(50).buildColumn()
				.column("Service Port").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
				.column("Pod Port").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
				.column(new ColumnLabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        boolean selected = ((ServicePortAdapter)element).isRoutePort();
                        return selected?OpenShiftImages.CHECKED_IMG:OpenShiftImages.UNCHECKED_IMG;
                    }

                    @Override
                    public String getText(Object element) {
                        return null;
                    }
				    
				})
				.name("Used by route").align(SWT.LEFT).weight(1).buildColumn()
				.buildViewer();
		viewer.addDoubleClickListener(new EditHandler());
		/*
		 * required because otherwise values are cached and causes the ObservableMapLabelProvider
		 * not to be updated because remove are not propagated.
		 */
        viewer.setComparer(new IElementComparer() {

            @Override
            public int hashCode(Object element) {
                return System.identityHashCode(element);
            }

            @Override
            public boolean equals(Object a, Object b) {
                return a == b;
            }
        });
		return viewer;
	}

	private SelectionListener onReset() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), "Reset ports", "Are you sure you want to reset the serviced ports to those exposed by the image?")) {
					model.resetServicePorts();
				}
			}
			
		};
	}

	/**
	 * Allow Finish for info statuses.
	 */
    @Override
    protected void setupWizardPageSupport(DataBindingContext dbc) {
        ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
    }

}
