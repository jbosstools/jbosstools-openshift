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

import java.lang.reflect.InvocationTargetException;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class CredentialsWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private final CredentialsWizardPageModel pageModel;

	private Text rhLoginText = null;
	private Text passwordText = null;

	public CredentialsWizardPage(IWizard wizard, IUserAwareModel wizardModel) {
		super("Sign in to OpenShift", "Please provide your OpenShift credentials.", "Server Connection",
				wizard);
		this.pageModel = new CredentialsWizardPageModel(wizardModel);
	}

	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(container);

		Link signupLink = new Link(container, SWT.WRAP);
		signupLink.setText("If you do not have an account on OpenShift, please sign up <a>here</a>.");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(signupLink);
		signupLink.addSelectionListener(onSignupLinkClicked());

		Label fillerLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).hint(SWT.DEFAULT, 6).applyTo(fillerLabel);

		Label rhLoginLabel = new Label(container, SWT.NONE);
		rhLoginLabel.setText("&Username");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(rhLoginLabel);
		rhLoginText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1).applyTo(rhLoginText);
		final IObservableValue rhLoginObservable =
				BeanProperties.value(CredentialsWizardPageModel.PROPERTY_RHLOGIN).observe(pageModel);
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(rhLoginText), rhLoginObservable);

		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("&Password");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1).applyTo(passwordText);
		final IObservableValue passwordModelObservable =
				BeanProperties.value(CredentialsWizardPageModel.PROPERTY_PASSWORD).observe(pageModel);
		final ISWTObservableValue passwordTextObservable = WidgetProperties.text(SWT.Modify).observe(passwordText);
		dbc.bindValue(passwordTextObservable, passwordModelObservable);

		IObservableValue credentialsStatusObservable =
				BeanProperties.value(CredentialsWizardPageModel.PROPERTY_CREDENTIALS_STATUS).observe(pageModel);
		dbc.addValidationStatusProvider(
				new CredentialsInputValidator(rhLoginObservable, passwordModelObservable));
		final CredentialsStatusValidator credentialsStatusValidator =
				new CredentialsStatusValidator(credentialsStatusObservable, passwordTextObservable);
		dbc.addValidationStatusProvider(credentialsStatusValidator);
		ControlDecorationSupport.create(credentialsStatusValidator, SWT.LEFT | SWT.TOP);

		new Label(container, SWT.NONE); // filler to align the checkbox under
										// the text fields
		Button rememberPasswordCheckBox = new Button(container, SWT.CHECK);
		rememberPasswordCheckBox.setText(OpenshiftUIMessages.OpenshiftWizardSavePassword);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(rememberPasswordCheckBox);
		final IObservableValue rememberPasswordModelObservable =
				BeanProperties.value(CredentialsWizardPageModel.PROPERTY_REMEMBER_PASSWORD).observe(pageModel);
		final ISWTObservableValue rememberPasswordCheckBoxObservable =
				WidgetProperties.selection().observe(rememberPasswordCheckBox);
		dbc.bindValue(rememberPasswordCheckBoxObservable, rememberPasswordModelObservable);
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
		if (StringUtils.isEmpty(pageModel.getRhLogin()) 
				&& rhLoginText != null) {
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
		boolean validCredemtials = performAuthentication();
		event.doit = validCredemtials;
		if (!event.doit) {
			passwordText.setFocus();
			passwordText.selectAll();
		}
	}

	public boolean performAuthentication() {
		try {
			WizardUtils.runInWizard(new Job("Verifying user credentials...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					pageModel.validateCredentials();
					monitor.done();
					return Status.OK_STATUS;
				}
			}, new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
		} catch (InterruptedException e) {
			Logger.error("Failed to authenticate on OpenShift", e);
			return false;
		} catch (InvocationTargetException e) {
			Logger.error("Failed to authenticate on OpenShift", e);
			return false;
		}
		return pageModel.areCredentialsValid();
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
			// set the wizard page status to OK as soon as a user types
			// something..
			final String rhLoginValue = (String) rhLoginObservable.getValue();
			final String passwordValue = (String) passwordObservable.getValue();
			if (!StringUtils.isEmpty(rhLoginValue)
					&& !StringUtils.isEmpty(passwordValue)) {
				return ValidationStatus.ok();
			}
			return ValidationStatus.cancel("Please provide your OpenShift user credentials");
		}
	}

	class CredentialsStatusValidator extends MultiValidator {

		private final IObservableValue credentialsStatusObservable;

		private final IObservableValue passwordTextObservable;

		public CredentialsStatusValidator(IObservableValue credentialsStatusObservable,
				IObservableValue passwordTextObservable) {
			this.credentialsStatusObservable = credentialsStatusObservable;
			this.passwordTextObservable = passwordTextObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus credentialsValidityStatus = (IStatus) credentialsStatusObservable.getValue();

			if (credentialsValidityStatus != null) {
				return pageModel.getCredentialsStatus();
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(passwordTextObservable);
			return targets;
		}
	}
}
