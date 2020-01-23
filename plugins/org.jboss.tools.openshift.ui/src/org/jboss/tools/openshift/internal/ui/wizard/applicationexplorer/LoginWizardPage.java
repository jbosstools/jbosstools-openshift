/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;

/**
 * @author Red Hat Developers
 *
 */
public class LoginWizardPage extends AbstractOpenShiftWizardPage {

	private LoginModel model;

	/**
	 * @param wizard the parent wizard
	 * @param model the model
	 */
	protected LoginWizardPage(IWizard wizard, LoginModel model) {
		super("Sign in to OpenShift", "Please sign in to your OpenShift server.", "Server Connection", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);

		Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setText("URL:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(urlLabel);
		Text txtURL = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtURL);

		ISWTObservableValue<String> urlObservable = WidgetProperties.text(SWT.Modify).observe(txtURL);
		Binding urlBinding = ValueBindingBuilder.bind(urlObservable)
		        .validatingAfterConvert(new URLValidator("url", true)).converting(new TrimTrailingSlashConverter())
		        .to(BeanProperties.value(LoginModel.PROPERTY_URL).observe(model)).in(dbc);
		ControlDecorationSupport.create(urlBinding, SWT.LEFT | SWT.TOP);

		Label userLabel = new Label(parent, SWT.NONE);
		userLabel.setText("Username:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(userLabel);
		Text txtUsername = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtUsername);

		ISWTObservableValue<String> userObservable = WidgetProperties.text(SWT.Modify).observe(txtUsername);
		Binding userBinding = ValueBindingBuilder.bind(userObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_USERNAME).observe(model)).in(dbc);
		ControlDecorationSupport.create(userBinding, SWT.LEFT | SWT.TOP);

		Label passwordLabel = new Label(parent, SWT.NONE);
		passwordLabel.setText("Password:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(passwordLabel);
		Text txtPassword = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtPassword);

		ISWTObservableValue<String> passwordObservable = WidgetProperties.text(SWT.Modify).observe(txtPassword);
		Binding passwordBinding = ValueBindingBuilder.bind(passwordObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_PASSWORD).observe(model)).in(dbc);
		ControlDecorationSupport.create(passwordBinding, SWT.LEFT | SWT.TOP);

		Label tokenLabel = new Label(parent, SWT.NONE);
		tokenLabel.setText("Token:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(tokenLabel);
		Text txtToken = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtToken);

		ISWTObservableValue<String> tokenObservable = WidgetProperties.text(SWT.Modify).observe(txtToken);
		Binding tokenBinding = ValueBindingBuilder.bind(tokenObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_TOKEN).observe(model)).in(dbc);
		ControlDecorationSupport.create(tokenBinding, SWT.LEFT | SWT.TOP);

		dbc.addValidationStatusProvider(new MultiValidator() {

			@Override
			protected IStatus validate() {
				String user = userObservable.getValue();
				String password = passwordObservable.getValue();
				String token = tokenObservable.getValue();
				if (!token.isEmpty() && (!user.isEmpty() || !password.isEmpty())) {
					return ValidationStatus.error("Can't use token authentication with user or password");
				}
				if (!user.isEmpty() && (password.isEmpty() || !token.isEmpty())) {
					return ValidationStatus.error("Can't use user authentication without a password or with a token");
				}
				if (user.isEmpty() && password.isEmpty() && token.isEmpty()) {
					return ValidationStatus.error("User and password or token must be provided");
				}
				return ValidationStatus.ok();
			}
		});
	}
}
