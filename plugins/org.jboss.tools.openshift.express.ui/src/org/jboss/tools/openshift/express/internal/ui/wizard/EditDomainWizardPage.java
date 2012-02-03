/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IDomain;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class EditDomainWizardPage extends AbstractOpenShiftWizardPage {

	private EditDomainWizardPageModel model;

	public EditDomainWizardPage(EditDomainWizardPageModel model, IWizard wizard) {
		super("Domain", "Create a new domain", "New Domain", wizard);
		this.model = model;
	}

	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		createDomainGroup(container, dbc);
	}
	
	private void createDomainGroup(Composite container, DataBindingContext dbc) {
		Group domainGroup = new Group(container, SWT.BORDER);
		domainGroup.setText("Domain");
		GridDataFactory.fillDefaults()
				.grab(true, false).align(SWT.FILL, SWT.TOP).span(3, 1).applyTo(domainGroup);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(4).applyTo(domainGroup);
		Label namespaceLabel = new Label(domainGroup, SWT.NONE);
		namespaceLabel.setText("&Domain name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(namespaceLabel);
		Text namespaceText = new Text(domainGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(namespaceText);
		Binding namespaceBinding = DataBindingUtils.bindMandatoryTextField(
				namespaceText, "Domain", ApplicationWizardPageModel.PROPERTY_NAMESPACE, model, dbc);
		Button createRenameButton = new Button(domainGroup, SWT.PUSH);
		DataBindingUtils.bindEnablementToValidationStatus(createRenameButton, IStatus.OK, dbc, namespaceBinding);
		dbc.bindValue(WidgetProperties.text().observe(createRenameButton)
				, BeanProperties.value(ApplicationWizardPageModel.PROPERTY_DOMAIN).observe(model)
				, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER)
				, new UpdateValueStrategy().setConverter(new Converter(IDomain.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof IDomain) {
							return "&Rename";
						} else {
							return "&Create";
						}
					}
				}));
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).hint(80, SWT.DEFAULT).applyTo(createRenameButton);
		createRenameButton.addSelectionListener(onCreateRenameDomain(dbc));
	}
	
	private SelectionListener onCreateRenameDomain(DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model.hasDomain()) {
					renameDomain();
				} else {
					try {
						createDomain();
					} catch (OpenShiftException ex) {
						IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, ex.getMessage(), ex);
						OpenShiftUIActivator.getDefault().getLog().log(status);
						ErrorDialog.openError(getShell(), "Error creating domain",
								"An error occurred while creating the domain.", status);
					}
				}
			}
		};
	}

	private void createDomain() throws OpenShiftException {
//		if (WizardUtils.openWizardDialog(
//				new NewDomainDialog(model.getNamespace(), wizardModel), getContainer().getShell()) == Dialog.OK) {
//			model.loadDomain();
//		}
	}

	private void renameDomain() {
		try {
			WizardUtils.runInWizard(
					new Job("Renaming domain...") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								model.renameDomain();
								return Status.OK_STATUS;
							} catch (Exception e) {
								return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
										"Could not rename domain", e);
							}
						}
					}, getContainer(), getDataBindingContext());
		} catch (Exception ex) {
			// ignore
		}
	}


	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR, this, dbc);
	}

}
