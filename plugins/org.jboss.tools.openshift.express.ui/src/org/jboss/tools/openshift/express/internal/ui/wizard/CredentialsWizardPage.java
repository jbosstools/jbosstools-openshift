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
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class CredentialsWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private final CredentialsWizardPageModel model;

	private Text rhLoginText = null;
	private Text passwordText = null;

	public CredentialsWizardPage(IWizard wizard, AbstractOpenShiftApplicationWizardModel wizardModel) {
		super("Server connection", "Please provide your OpenShift Express user credentials, then click 'next'.", "Server Connection",
				wizard);
		this.model = new CredentialsWizardPageModel(wizardModel);
	}

	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(container);

		// This wizard page status and navigation controls are bound to the credentials validity status:
		// 1 - No error message is displayed if the credentials validity status is "OK"
		// 2 - when the user changes the 'rhLogin' or 'password' field values, the credentials validity status is set to
		// "Cancel", thus no error message should appear on the wizard page
		// 3 - when the 'rhLogin' and the 'password' fields are not null/empty, the 'next' control button is enabled
		// 4 - if the credentials validation fails (ie, invalid credentials), the 'next' button is disabled and an error
		// message is displayed at the top of the wizard page until the 'rhLogin' and/or the 'password' field values
		// have been changed. Then back to step 2.

		Label rhLoginLabel = new Label(container, SWT.NONE);
		rhLoginLabel.setText("&Username");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(rhLoginLabel);
		rhLoginText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(rhLoginText);
		UIUtils.selectAllOnFocus(rhLoginText);
		final IObservableValue rhLoginObservable = BeanProperties.value(CredentialsWizardPageModel.PROPERTY_RHLOGIN)
				.observe(model);
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(rhLoginText), rhLoginObservable);

		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("&Password");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(passwordText);
		UIUtils.selectAllOnFocus(passwordText);
		final IObservableValue passwordModelObservable = BeanProperties.value(CredentialsWizardPageModel.PROPERTY_PASSWORD)
				.observe(model);
		final ISWTObservableValue passwordTextObservable = WidgetProperties.text(SWT.Modify).observe(passwordText);
		dbc.bindValue(passwordTextObservable, passwordModelObservable);
		
		IObservableValue credentialsStatusObservable = BeanProperties.value(
				CredentialsWizardPageModel.PROPERTY_CREDENTIALS_STATUS).observe(model);
		dbc.addValidationStatusProvider(new CredentialsInputValidator(rhLoginObservable,
				passwordModelObservable));
		final CredentialsStatusValidator credentialsStatusValidator = new CredentialsStatusValidator(credentialsStatusObservable, passwordTextObservable);
		dbc.addValidationStatusProvider(credentialsStatusValidator);
		ControlDecorationSupport.create(credentialsStatusValidator, SWT.LEFT | SWT.TOP);

		Link signupLink = new Link(container, SWT.WRAP);
		signupLink.setText("If you don't have an account on OpenShift, please sign up <a>here</a>.");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).hint(SWT.DEFAULT, 30).applyTo(signupLink);
		signupLink.addSelectionListener(onSignupLinkClicked());
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
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		super.onPageActivated(dbc);
		if (model.getRhLogin() == null || model.getRhLogin().isEmpty() && rhLoginText != null) {
			rhLoginText.setFocus();
		} else if (passwordText != null) {
			passwordText.setFocus();
		}
	}

	@Override
	protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.BACKWARDS) {
			return;
		}

		if (!model.areCredentialsValid()) {
			try {
				final ArrayBlockingQueue<IStatus> queue = new ArrayBlockingQueue<IStatus>(1);
				WizardUtils.runInWizard(new Job("Verifying user credentials...") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						IStatus status = model.validateCredentials();
						queue.offer(status);
						monitor.done();
						return Status.OK_STATUS;
					}
				}, new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
				queue.poll(10, TimeUnit.SECONDS);
				event.doit = model.areCredentialsValid();
				if(!event.doit) {
					passwordText.setFocus();
					passwordText.selectAll();
				}
			} catch (Exception ex) {
				event.doit = false;
			} finally {
			}

		}
	}

	class CredentialsInputValidator extends MultiValidator {

		private final IObservableValue rhLoginObservable;

		private final IObservableValue passwordObservable;

		public CredentialsInputValidator(IObservableValue rhLoginObservable,
				IObservableValue passwordObservable) {
			this.rhLoginObservable = rhLoginObservable;
			this.passwordObservable = passwordObservable;
		}

		@Override
		protected IStatus validate() {
			// set the wizard page status to OK as soon as a user types something..
			final String rhLoginValue = (String) rhLoginObservable.getValue();
			final String passwordValue = (String) passwordObservable.getValue();
			if (rhLoginValue != null && !rhLoginValue.isEmpty() && passwordValue != null && !passwordValue.isEmpty()) {
				return ValidationStatus.ok();
			}
			return ValidationStatus.cancel("Please provide your OpenShift Express user credentials");
		}
	}

	class CredentialsStatusValidator extends MultiValidator {

		private final IObservableValue credentialsStatusObservable;

		private final IObservableValue passwordTextObservable;
		
		public CredentialsStatusValidator(IObservableValue credentialsStatusObservable, IObservableValue passwordTextObservable) {
			this.credentialsStatusObservable = credentialsStatusObservable;
			this.passwordTextObservable = passwordTextObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus credentialsValidityStatus = (IStatus) credentialsStatusObservable.getValue();

			if (credentialsValidityStatus != null) {
				final IStatus credentialsValidity = model.getCredentialsStatus();
				return credentialsValidity;
			}
			return ValidationStatus.ok();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.databinding.validation.MultiValidator#getTargets()
		 */
		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(passwordTextObservable);
			return targets;
		}
		
		
	}

}
