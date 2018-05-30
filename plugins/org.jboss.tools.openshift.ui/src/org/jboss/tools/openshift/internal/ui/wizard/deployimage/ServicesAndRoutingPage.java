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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
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
public class ServicesAndRoutingPage extends AbstractOpenShiftWizardPage {
	private static final String PAGE_NAME = "Services && Routing Settings Page";
	private static final String PAGE_TITLE = "Services && Routing Settings";
	private static final String PAGE_DESCRIPTION = "";
	private static final int NAME_COLUMN_INDEX = 0;
	private static final int SERVICE_PORT_COLUMN_INDEX = 1;
	private static final int POD_PORT_COLUMN_INDEX = 2;
	private static final int ROUTE_PORT_COLUMN_INDEX = 3;
	private IServiceAndRoutingPageModel model;

	TableViewer portsViewer;

	public ServicesAndRoutingPage(IWizard wizard, IServiceAndRoutingPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard);
		this.model = model;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);
		createExposedPortsControl(parent, dbc);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false)
				.applyTo(new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL));

		//routing
		Composite routingContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(routingContainer);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(2).applyTo(routingContainer);

		Button btnAddRoute = new Button(routingContainer, SWT.CHECK);
		btnAddRoute.setText("Add Route");
		btnAddRoute.setToolTipText(
				"Adding a route to the service will make the image accessible\noutside of the OpenShift cluster on all the available service ports. \nYou can target a specific port by editing the route later.");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).span(2, 1).applyTo(btnAddRoute);
		final IObservableValue<Boolean> addRouteModelObservable = BeanProperties
				.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE).observe(model);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(btnAddRoute)).to(addRouteModelObservable).in(dbc);

		Label labelRouteHostname = new Label(routingContainer, SWT.NONE);
		labelRouteHostname.setText("Hostname:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(labelRouteHostname);

		Text textRouteHostname = new Text(routingContainer, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(textRouteHostname);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(textRouteHostname))
				.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ADD_ROUTE).observe(model)).in(dbc);
		final IObservableValue<String> routeHostnameObservable = WidgetProperties.text(SWT.Modify)
				.observe(textRouteHostname);
		ValueBindingBuilder.bind(routeHostnameObservable).converting(new TrimmingStringConverter())
				.to(BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ROUTE_HOSTNAME).observe(model)).in(dbc);

		final IObservableList<IServicePort> portsObservable = 
				BeanProperties.list(IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS).observe(model);
		final IObservableValue<IServicePort> routingPortObservable = 
				BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_ROUTING_PORT).observe(model);

		MultiValidator validator = new MultiValidator() {

			@Override
			protected IStatus validate() {
				IStatus status = ValidationStatus.ok();

				final boolean isAddRoute = addRouteModelObservable.getValue();
				final String hostName = routeHostnameObservable.getValue();
				final int numOfPorts = portsObservable.size();
				final IServicePort routingPort = routingPortObservable.getValue();

				if (isAddRoute) {
					status = validateHostName(status, hostName);
					if (!status.matches(IStatus.ERROR) 
							&& isAddRoute 
							&& (numOfPorts > 1)
							&& (routingPort == null)) {
						if (status.matches(IStatus.INFO)) {
							status = ValidationStatus
									.info(status.getMessage() + "\n " + OpenShiftUIMessages.RoundRobinRoutingMessage);
						} else {
							status = ValidationStatus.info(OpenShiftUIMessages.RoundRobinRoutingMessage);
						}
					}
				}
				return status;
			}

			private IStatus validateHostName(IStatus status, final String hostName) {
				if (StringUtils.isBlank(hostName)) {
					status = ValidationStatus
							.info(NLS.bind(OpenShiftUIMessages.EmptyHostNameErrorMessage, hostName));
				} else if (!DomainValidator.getInstance(true).isValid(hostName)) {
					status = ValidationStatus
							.error(NLS.bind(OpenShiftUIMessages.InvalidHostNameErrorMessage, hostName));
				}
				return status;
			}
		};
		dbc.addValidationStatusProvider(validator);
		ControlDecorationSupport.create(validator, SWT.LEFT | SWT.TOP);
	}

	private void createExposedPortsControl(Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(container);

		Label label = new Label(container, SWT.NONE);
		label.setText("Service Ports:");
		label.setToolTipText("The exposed ports of the image.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).span(2, 1)
			.applyTo(label);
		Composite tableContainer = new Composite(container, SWT.NONE);

		this.portsViewer = createTable(tableContainer);

		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(tableContainer);

		@SuppressWarnings("unchecked")
		IObservableList<IServicePort> portsObservable = 
				BeanProperties.list(IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS).observe(model);
		portsViewer.setInput(portsObservable);
		@SuppressWarnings("unchecked")
		IObservableValue<IServicePort> selectedServicePortObservable = 
				BeanProperties.value(IServiceAndRoutingPageModel.PROPERTY_SELECTED_SERVICE_PORT).observe(model);
		ValueBindingBuilder
			.bind(ViewerProperties.singleSelection().observe(portsViewer))
			.to(selectedServicePortObservable)
			.in(dbc);
		dbc.addValidationStatusProvider(new MultiValidator() {

			@Override
			protected IStatus validate() {
				if (portsObservable.isEmpty()) {
					return ValidationStatus
							.error("At least 1 port is required when generating the service for the deployed image");
				}
				return Status.OK_STATUS;
			}
		});

		portsViewer.getTable().addMouseListener(onTableCellClicked());

		Button btnEdit = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(btnEdit);
		btnEdit.setText("Edit...");
		btnEdit.setToolTipText("Edit a port to be exposed by the service.");
		btnEdit.addSelectionListener(new EditServicePortHandler());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(btnEdit))
			.notUpdatingParticipant()
			.to(selectedServicePortObservable)
			.converting(new IsNotNull2BooleanConverter())
			.in(dbc);
		UIUtils.setDefaultButtonWidth(btnEdit);

		Button btnReset = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(btnReset);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Resets the list of ports to the exposed ports of the image.");
		btnReset.addSelectionListener(onReset());
		UIUtils.setDefaultButtonWidth(btnReset);
	}

	private MouseListener onTableCellClicked() {
		return new TableCellMouseAdapter(ROUTE_PORT_COLUMN_INDEX) {

			@Override
			public void mouseUpCell(MouseEvent event) {
				ServicePortAdapter port = getClickedPort(event);
				if (port == null) {
					return;
				}
				port.setRoutePort(true);
				model.setRoutingPort(port);
				model.setSelectedServicePort(port);
			}

			private ServicePortAdapter getClickedPort(MouseEvent event) {
				TableItem item = getItem(event);
				if (item == null) {
					return null;
				}
				if (item.getData() instanceof ServicePortAdapter) {
					return (ServicePortAdapter) item.getData();
				}
				return null;
			}

			private TableItem getItem(MouseEvent event) {
				Point pt = new Point(event.x, event.y);
				if (!(event.widget instanceof Table)) {
					return null;
				}
				return ((Table) event.widget).getItem(pt);
			}
		};
	}

	class EditServicePortHandler extends SelectionAdapter implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			handleEvent();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			handleEvent();
		}

		public void handleEvent() {
			String message = "Edit the port to be exposed by the service";
			final ServicePortAdapter port = model.getSelectedServicePort();
			final ServicePortAdapter target = new ServicePortAdapter((ServicePortAdapter) port);
			ServicePortDialog dialog = new ServicePortDialog(target, message, model.getServicePorts());
			if (Window.OK == dialog.open()) {
				port.update(target);
			}
		}
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
			.column("Name")
				.align(SWT.LEFT).weight(2).minWidth(50).buildColumn()
			.column("Service Port")
				.align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
			.column("Pod Port")
				.align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
			.column("Used by route").align(SWT.LEFT).weight(1).buildColumn()
				.buildViewer();

		ObservableListContentProvider contentProvider = new ObservableListContentProvider();
        viewer.setContentProvider(contentProvider);

        viewer.setLabelProvider(createLabelProvider(contentProvider));
		viewer.setComparer(new InstanceComparer());
		viewer.addDoubleClickListener(new EditServicePortHandler());
		return viewer;
	}

	/**
	 * Creates a label provider for the service ports table.
	 * 
	 * It's using an {@link ObservableMapLabelProvider} which is able listen to
	 * changes in properties in a table item (bean). Thus there's no need to refresh
	 * the table manually (or similar workarounds that manually fire list changes
	 * when an item is updated).
	 * 
	 * <br>
	 * <br>
	 * - Warning: ObservableMapLabelProvider can only handle changes in existing
	 * items. See the map change listener (that listens to changes in properties of
	 * the table items:
	 * 
	 * <pre>
	 * 	private IMapChangeListener mapChangeListener = new IMapChangeListener() {
	 *
	 *		public void handleMapChange(MapChangeEvent event) {
	 *			Set affectedElements = event.diff.getChangedKeys();
	 *			LabelProviderChangedEvent newEvent = new LabelProviderChangedEvent(
	 *					ObservableMapLabelProvider.this, affectedElements.toArray());
	 *			fireLabelProviderChanged(newEvent);
	 *		}
	 *  }
	 * </pre>
	 * 
	 * 
	 * 
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private IBaseLabelProvider createLabelProvider(ObservableListContentProvider contentProvider) {
		IObservableSet<ServicePortAdapter> knownElements = contentProvider.getKnownElements();
		final IObservableMap name = BeanProperties.value(ServicePortAdapter.class, ServicePortAdapter.NAME)
				.observeDetail(knownElements);
		final IObservableMap port = BeanProperties.value(ServicePortAdapter.class, ServicePortAdapter.PORT)
				.observeDetail(knownElements);
		final IObservableMap targetPort = BeanProperties.value(ServicePortAdapter.class, ServicePortAdapter.TARGET_PORT)
				.observeDetail(knownElements);
		final IObservableMap routePort = BeanProperties.value(ServicePortAdapter.class, ServicePortAdapter.ROUTE_PORT)
				.observeDetail(knownElements);
		IObservableMap[] labelMaps = { name, port, targetPort, routePort };

        return new ObservableMapLabelProvider(labelMaps) {

			@Override
			public String getColumnText(Object element, int columnIndex) {
				Object label = null;
				if (columnIndex == NAME_COLUMN_INDEX) {
					label = name.get(element);
				} else if (columnIndex == SERVICE_PORT_COLUMN_INDEX) {
					label = port.get(element);
				} else if (columnIndex == POD_PORT_COLUMN_INDEX) {
					label = targetPort.get(element);
				} else if (columnIndex == ROUTE_PORT_COLUMN_INDEX) {
					// image column
				}
				return ObjectUtils.toString(label);
			}

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				if (columnIndex != ROUTE_PORT_COLUMN_INDEX) {
					// image only in routing port column
					return null;
				}
				boolean checked = Boolean.TRUE.equals(routePort.get(element));
				return checked ? OpenShiftImages.CHECKED_IMG : OpenShiftImages.UNCHECKED_IMG;
			}
			
        };
	}

	private SelectionListener onReset() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openQuestion(getShell(), "Reset ports",
						"Are you sure you want to reset the serviced ports to those exposed by the image?")) {
					model.resetServicePorts();
				}
			}

		};
	}

	/**
	 * Allow Finish for info status, disallow for error and cancel
	 */
	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}
	
	/**
	 * ElementComparer that's based on instance equality.
	 * 
	 * Viewer item identity has to be based on instance equality (not logical
	 * equality). This is because updates for existing items are misinterpreted as
	 * new items (an updated item is equal in terms of instance identity while it's
	 * not equal in terms of data within it)
	 */
	private static class InstanceComparer implements IElementComparer {

		@Override
		public boolean equals(Object thisElement, Object thatElement) {
			return thisElement == thatElement;
		}

		@Override
		public int hashCode(Object element) {
			return System.identityHashCode(element);
		}
	}

}
