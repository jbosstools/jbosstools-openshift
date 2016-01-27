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
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;

import com.openshift.restclient.model.IServicePort;

public class ServicePortDialog extends AbstractOpenShiftWizardPage {

	static final String PROPERTY_SERVICE_PORT = "port";
	static final String PROPERTY_POD_PORT = "targetPort";
	
	private IServicePort model;
	private List<IServicePort> ports;
	private final int servicePort;
	private final String podPort;

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
			.numColumns(2)
			.margins(25, 25)
			.applyTo(dialogArea);
		
		//service port
		Label lblServicePort = new Label(dialogArea, SWT.NONE);
		lblServicePort.setText("Service port:");
		GridDataFactory.fillDefaults()
			.align(SWT.RIGHT, SWT.CENTER)
			.applyTo(lblServicePort);
		
		final Spinner servicePortSpinner = new Spinner(dialogArea, SWT.BORDER);
		servicePortSpinner.setMinimum(1);
		servicePortSpinner.setMaximum(65535);
		servicePortSpinner.setToolTipText("The port exposed by the service that will route to the pod.");

		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
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
		lblPodPort.setText("Pod port:");
		GridDataFactory.fillDefaults()
			.align(SWT.RIGHT, SWT.CENTER).applyTo(lblPodPort);

		Text txtTargetPort = new Text(dialogArea, SWT.BORDER);
		txtTargetPort.setToolTipText("The port exposed by the pod which will accept traffic.\nIt must be an integer or named port on the pod.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(txtTargetPort);
		
		Binding podPortBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(txtTargetPort))
				.validatingAfterConvert(new PodPortValidator())
				.to(BeanProperties.value(PROPERTY_POD_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				podPortBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		Label lbl = new Label(dialogArea, SWT.NONE);
		lbl.setText("Pod port is linked to service port changes");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.applyTo(lbl);
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
		
		private final int MAXLENGTH = 63;
		private final Pattern REGEXP = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])*");

		private final IStatus ERROR = ValidationStatus.error("The target port must be at most 15 characters, matching regex [a-z0-9]([a-z0-9-]*[a-z0-9])*, and hyphens cannot be adjacent to other hyphens): e.g. \"http\"");
		
		@Override
		public IStatus validate(Object value) {
			if(StringUtils.isEmpty(value)) {
				return ERROR;
			}
			String newPort = (String) value;
			if(!podPort.equals(newPort)) {
				if(newPort.length() > MAXLENGTH || !REGEXP.matcher(newPort).matches()) {
					return ERROR;
				}
				for (IServicePort port : ports) {
					if(port.getTargetPort().equals(newPort)) {
						return ERROR;
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
