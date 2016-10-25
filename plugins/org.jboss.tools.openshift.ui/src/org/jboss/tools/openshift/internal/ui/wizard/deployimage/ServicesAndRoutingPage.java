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

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;

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
		final IObservableValue<Boolean> addRouteModelObservable = BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE).observe(model);
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
        ValueBindingBuilder.bind(WidgetProperties.enabled().observe(textRouteHostname))
        .to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE)
        .observe(model))
        .in(dbc);
        final IObservableValue<String> routeHostnameObservable = WidgetProperties.text(SWT.Modify).observe(textRouteHostname);
        ValueBindingBuilder.bind(routeHostnameObservable)
        .converting(new TrimmingStringConverter())
        .to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ROUTE_HOSTNAME)
                .observe(model))
        .in(dbc);
        
        MultiValidator validator = new MultiValidator() {
            
            @Override
            protected IStatus validate() {
                IStatus status = ValidationStatus.ok();
                boolean isAddRoute = addRouteModelObservable.getValue();
                String hostName = routeHostnameObservable.getValue();
                if (isAddRoute) {
                    if (StringUtils.isBlank(hostName)) {
                        status = ValidationStatus.info(NLS.bind(OpenShiftUIMessages.EmptyHostNameErrorMessage, hostName));
                    } else if (!DomainValidator.getInstance(true).isValid(hostName)) {
                        status = ValidationStatus.error(NLS.bind(OpenShiftUIMessages.InvalidHostNameErrorMessage, hostName));
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
		
        IObservableList portsObservable = BeanProperties.list(
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
                    boolean selected = (boolean) attributeMaps[columnIndex].get(element);
                    return selected?OpenShiftImages.CHECKED_IMG:OpenShiftImages.UNCHECKED_IMG;
		        }
		        return null;
		    }

		    @Override
		    public String getColumnText(Object element, int columnIndex) {
		        if (columnIndex < attributeMaps.length - 1) {
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

		portsViewer.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				Point pt = new Point(event.x, event.y);
				TableItem item = portsViewer.getTable().getItem(pt);
				if (item != null && item.getBounds(ROUTE_PORT_COLUMN_INDEX) != null && item.getBounds(ROUTE_PORT_COLUMN_INDEX).contains(pt)) {
					IServicePort port = model.getSelectedServicePort();
					ServicePortAdapter target = new ServicePortAdapter((ServicePortAdapter)port);
					target.setRoutePort(!target.isRoutePort());
					target.setName(NLS.bind("{0}-tcp", target.getPort()));
					model.updateServicePort(port, target);
					model.setSelectedServicePort(target);
				}
			}
		});
	
		Button btnAdd = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnAdd);
		btnAdd.setText("Add...");
		btnAdd.setToolTipText("Add a port to be exposed by the service which is not explicilty declared by the image.");
		btnAdd.addSelectionListener(onAdd());
		UIUtils.setDefaultButtonWidth(btnAdd);

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
		UIUtils.setDefaultButtonWidth(removeButton);

		Button btnReset = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(btnReset);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Resets the list of ports to the exposed ports of the image.");
		btnReset.addSelectionListener(onReset());
		UIUtils.setDefaultButtonWidth(btnReset);
		
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String message = "Add a port to be exposed by the service";
				ServicePortAdapter port = new ServicePortAdapter();
				List<IServicePort> servicePorts = model.getServicePorts();
				if(!servicePorts.isEmpty()) {
					Set<Integer> ports = servicePorts.stream().map((p) -> p.getPort()).collect(Collectors.toSet());
					port.setPort(generateNewPort(ports));
					ports = servicePorts.stream().map((p) -> getIntegerPort(p.getTargetPort())).collect(Collectors.toSet());
					port.setTargetPort(generateNewPort(ports));
				}
				ServicePortDialog dialog = new ServicePortDialog(port, message, servicePorts);
				if(Window.OK == dialog.open()) {
					port.setName(NLS.bind("{0}-tcp", port.getPort()));
					model.addServicePort(port);
					model.setSelectedServicePort(port);
				}
			}
		};
	}

	private static final Random seed = new Random();

	private static int generateNewPort(Set<Integer> ports) {
		while(true) {
			int n = 1025 + seed.nextInt(65536 - 1025); // port should be in range [1025..65535]
			if(!ports.contains(n)) {
				return n;
			}
		}
	}

	/**
	 * Returns integer if string value is an integer in range [0..65535], or -1.
	 * @param stringPort
	 * @return
	 */
	private static int getIntegerPort(String stringPort) {
		if(StringUtils.isBlank(stringPort) || stringPort.length() > 5 || !StringUtils.isNumeric(stringPort)) {
			return -1;
		}
		//Now, string port has only digits, and it length is no more than 5. There may be no NumberFormatException.
		int value = Integer.parseInt(stringPort);
		return (value > 65535) ? -1 : value;
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
		return viewer;
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
			    ServicePortAdapter port = UIUtils.getFirstElement(portsViewer.getSelection(), ServicePortAdapter.class);
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

	/**
	 * Allow Finish for info statuses.
	 */
    @Override
    protected void setupWizardPageSupport(DataBindingContext dbc) {
        ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
    }

}
