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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.utils.UIUtils;

/**
 * @author André Dietisheim
 */
public class CredentialsWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private Button validateButton;

	private CredentialsWizardPageModel model;

	public CredentialsWizardPage(IWizard wizard, ImportProjectWizardModel wizardModel) {
		super("Server connection", "Please provide the credentails of your user account on OpenShift Express",
				"Server Connection", wizard);
		this.model = new CredentialsWizardPageModel(wizardModel);
	}

	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(container);

		Link signupLink = new Link(container, SWT.WRAP);
		signupLink.setText("If you dont have an account on OpenShift, please sign up <a>here</a>.");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).hint(SWT.DEFAULT, 30).applyTo(signupLink);
		signupLink.addSelectionListener(onSignupLinkClicked());

		Label rhLoginLabel = new Label(container, SWT.NONE);
		rhLoginLabel.setText("&Username");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(rhLoginLabel);
		Text rhLoginText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(rhLoginText);
		UIUtils.selectAllOnFocus(rhLoginText);
		DataBindingUtils.bindMandatoryTextField(
				rhLoginText, "Username", CredentialsWizardPageModel.PROPERTY_RHLOGIN, model, dbc);
		
		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("&Password");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		Text passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(passwordText);
		UIUtils.selectAllOnFocus(passwordText);
		DataBindingUtils.bindMandatoryTextField(
				passwordText, "Password", CredentialsWizardPageModel.PROPERTY_PASSWORD, model, dbc);

		Label spacerLabel = new Label(container, SWT.None);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(spacerLabel);

		this.validateButton = new Button(container, SWT.NONE);
		validateButton.setText("&Validate");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).indent(0, 10).hint(100, SWT.DEFAULT)
				.applyTo(validateButton);
		DataBindingUtils.bindEnablementToValidationStatus(
				validateButton
				, IStatus.INFO
				, dbc);
		validateButton.addSelectionListener(onValidate(dbc));
		dbc.bindValue(
				new WritableValue(null, IStatus.class),
				BeanProperties.value(CredentialsWizardPageModel.PROPERTY_CREDENTIALS_VALIDITY).observe(model),
				new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
				new UpdateValueStrategy().setAfterGetValidator(
						new CredentialsStatusValidator()));

		setErrorMessage(null);
	}

	protected SelectionAdapter onValidate(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					WizardUtils.runInWizard(
							new Job("Testing user credentials...") {

								@Override
								protected IStatus run(IProgressMonitor monitor) {
									model.validateCredentials();
									return Status.OK_STATUS;
								}
							}, getContainer(), dbc);
				} catch (Exception ex) {
					// ignore
				}
			};
		};
	}

	protected SelectionAdapter onSignupLinkClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				BrowserUtil.checkedCreateInternalBrowser(OPENSHIFT_EXPRESS_SIGNUP_URL, OPENSHIFT_EXPRESS_SIGNUP_URL,
						OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				getContainer().getShell().close();
			}
		};
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.CANCEL, this,
				dbc);
	}

	@Override
	protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.BACKWARDS) {
			return;
		}
		
		if (!model.areCredentialsValidated()) {
			try {
				final ArrayBlockingQueue<IStatus> queue = new ArrayBlockingQueue<IStatus>(1);
				WizardUtils.runInWizard(
						new Job("Testing user credentials...") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								IStatus status = model.validateCredentials();
								queue.offer(status);
								return Status.OK_STATUS;
							}
						}, getContainer(), getDatabindingContext());
				queue.poll(10, TimeUnit.SECONDS);
				event.doit = model.areCredentialsValid();
			} catch (Exception ex) {
				event.doit = false;
			}
		}
	}

	private static class CredentialsStatusValidator implements IValidator {
		public IStatus validate(Object value) {
			if (value instanceof IStatus) {
				int currentSeverity = ((IStatus) value).getSeverity();
				if (currentSeverity < IStatus.ERROR) {
					return ValidationStatus.ok();
				}
			} else if (value == null) {
				return ValidationStatus.info("You have to validate your credentials");
			}
			return ValidationStatus.error("The given credentials are not valid");
		}
	}
}
