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

import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;

import com.openshift.restclient.model.IServicePort;

public class ServicePortDialog extends AbstractOpenShiftWizardPage {

	static final String PROPERTY_SERVICE_PORT = "port";
	static final String PROPERTY_POD_PORT = "targetPort";
	
	private IServicePort model;
	private List<IServicePort> ports;
	private final int servicePort;
	private final int podPort;

	public ServicePortDialog(IServicePort model, String message, List<IServicePort> ports) {
		super("Configure Service Ports", message, "", null);
		this.model = model;
		this.ports = ports;
		this.servicePort = model.getPort();
		this.podPort = model.getTargetPort();
	}
	
	
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.margins(1, 1).applyTo(parent);
		
		Composite dialogArea = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
		GridLayoutFactory.fillDefaults()
			.numColumns(3)
			.margins(25, 25)
			.applyTo(dialogArea);
		
		//service port
		Label lblServicePort = new Label(dialogArea, SWT.NONE);
		lblServicePort.setText("Service Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.RIGHT, SWT.CENTER)
			.applyTo(lblServicePort);
		
		final Spinner servicePortSpinner = new Spinner(dialogArea, SWT.BORDER);
		servicePortSpinner.setMinimum(1);
		servicePortSpinner.setMaximum(65535);
		servicePortSpinner.setToolTipText("The port exposed by the service that will route to the pod.");

		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.applyTo(servicePortSpinner);
		Binding servicePortBinding = ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(servicePortSpinner))
				.validatingAfterConvert(new ServicePortValidator())
				.to(BeanProperties.value(PROPERTY_SERVICE_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
			servicePortBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		//pod port
		Label lblPodPort = new Label(dialogArea, SWT.NONE);
		lblPodPort.setText("Pod Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.RIGHT, SWT.CENTER).applyTo(lblPodPort);

		//to be replaced by txtbox when supporting named ports
		final Spinner podPortSpinner = new Spinner(dialogArea, SWT.BORDER);
		podPortSpinner.setMinimum(1);
		podPortSpinner.setMaximum(65535);
		podPortSpinner.setToolTipText("The port exposed by the pod which will accept traffic");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.applyTo(podPortSpinner);
		Binding podPortBinding = ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(podPortSpinner))
				.validatingAfterConvert(new PodPortValidator())
				.to(BeanProperties.value(PROPERTY_POD_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				podPortBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		servicePortSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				podPortSpinner.setSelection(servicePortSpinner.getSelection());
			}
		});
		
	}
	
	class ServicePortValidator implements IValidator{

		@Override
		public IStatus validate(Object value) {
			Integer newPort = (Integer) value;
			if(servicePort != newPort) {
				for (IServicePort port : ports) {
					if(newPort.intValue() == port.getPort()) {
						return ValidationStatus.error("The service port number must be unique among all the other ports exposed by this OpenShift service.");
					}
				}
			}
			return ValidationStatus.OK_STATUS;
		}
		
	}
	class PodPortValidator implements IValidator{
		
		@Override
		public IStatus validate(Object value) {
			Integer newPort = (Integer) value;
			if(newPort != podPort) {
				for (IServicePort port : ports) {
					if(newPort.intValue() == port.getTargetPort()) {
						return ValidationStatus.error("The pod port number must be unique among all the other pod ports exposed by this OpenShift service.");
					}
				}
			}
			return ValidationStatus.OK_STATUS;
		}
		
	}
	
	public int open() {
		final IWizardPage page = this;
		Wizard wizard = new Wizard() {
			
			@Override
			public boolean performFinish() {
				return true;
			}
			
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
