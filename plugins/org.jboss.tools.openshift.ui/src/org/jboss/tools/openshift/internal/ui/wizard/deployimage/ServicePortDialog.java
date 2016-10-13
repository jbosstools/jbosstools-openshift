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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;

import com.openshift.restclient.model.IServicePort;

/**
 * Dialog to configure the port mapping for a Docker Image to be deployed.
 */
public class ServicePortDialog extends AbstractOpenShiftWizardPage {
	static final String UNIQUE_ERROR = "The {0} port number must be unique among all the other ports exposed by this OpenShift service.";

	static final String PROPERTY_SERVICE_PORT = "port";
	static final String PROPERTY_POD_PORT = "targetPort";
	
	private final ServicePortAdapter model;
	private final List<IServicePort> ports;

	/**
	 * Constructor
	 * @param model
	 * @param message
	 * @param ports
	 */
	public ServicePortDialog(final ServicePortAdapter model, final String message, final List<IServicePort> ports) {
		super("Configure Service Ports", message, "", null);
		this.model = model;
		this.ports = ports;
	}
	
	@Override
	protected void doCreateControls(final Composite parent, final DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.margins(1, 1).applyTo(parent);
		final Composite dialogArea = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
		GridLayoutFactory.fillDefaults()
			.numColumns(2)
			.applyTo(dialogArea);
		
		//service port
		final Label servicePortLabel = new Label(dialogArea, SWT.NONE);
		servicePortLabel.setText("Service port:");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER)
			.applyTo(servicePortLabel);
		
		final Spinner servicePortSpinner = new Spinner(dialogArea, SWT.BORDER);
		servicePortSpinner.setMinimum(1);
		servicePortSpinner.setMaximum(65535);
		servicePortSpinner.setToolTipText("The port exposed by the service that will route to the pod.");

		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(servicePortSpinner);
		final Binding servicePortBinding = ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(servicePortSpinner))
				.validatingAfterConvert(new ServicePortValidator(model.getPort(), this.ports))
				.to(BeanProperties.value(PROPERTY_SERVICE_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
			servicePortBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		// Pod port
		final Label podPortLabel = new Label(dialogArea, SWT.NONE);
		podPortLabel.setText("Pod port:");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(podPortLabel);

		final Text podPortText = new Text(dialogArea, SWT.BORDER);
		podPortText.setToolTipText("The port exposed by the pod which will accept traffic.\nIt must be an integer or the name of a port in the backend Pods.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(podPortText);
		
		final Binding podPortBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(podPortText))
				.validatingAfterConvert(new PodPortValidator(this.model.getTargetPort(), this.ports))
				.to(BeanProperties.value(PROPERTY_POD_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				podPortBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		final Button routePortButton = new Button(dialogArea, SWT.CHECK);
		routePortButton.setText("Used by route");
		GridDataFactory.fillDefaults()
		    .align(SWT.FILL, SWT.CENTER)
		    .grab(true, false)
		    .span(2, 1)
		    .applyTo(routePortButton);
		ValueBindingBuilder
		    .bind(WidgetProperties.selection().observe(routePortButton))
		    .to(BeanProperties.value(ServicePortAdapter.ROUTE_PORT).observe(model))
		    .in(dbc);
	}
	
	/**
	 * Validates the Service Port
	 */
	static class ServicePortValidator implements IValidator{

		private static final IStatus SERVICE_PORT_ERROR = ValidationStatus.error(NLS.bind(UNIQUE_ERROR, "service"));
		
		private final int servicePort;
		
		private final List<? extends IServicePort> ports;
		
		public ServicePortValidator(final int servicePort, final List<? extends IServicePort> ports) {
			this.servicePort = servicePort;
			this.ports = ports;
		}
		
		@Override
		public IStatus validate(Object value) {
			Integer newPort = (Integer) value;
			if(servicePort != newPort) {
				for (IServicePort port : ports) {
					if(newPort.intValue() == port.getPort()) {
						return SERVICE_PORT_ERROR;
					}
				}
			}
			return ValidationStatus.OK_STATUS;
		}
	}
		
	/**
	 * Opens this dialog.
	 * 
	 * @return the return code, ie, the value of the button that the user
	 *         clicked to close the dialog.
	 */
	public int open() {
		final IWizardPage page = this;
		Wizard wizard = new Wizard() {
			
			@Override
			public boolean performFinish() {
				return true;
			}
			
			@Override
			public void addPages() {
				addPage(page);
			}
			
		};
		wizard.setNeedsProgressMonitor(true);
		wizard.setWindowTitle("Service Ports");
		this.setWizard(wizard);
		OkCancelButtonWizardDialog dialog =
				new OkCancelButtonWizardDialog(getShell(), wizard);
		return dialog.open();
	}
}
