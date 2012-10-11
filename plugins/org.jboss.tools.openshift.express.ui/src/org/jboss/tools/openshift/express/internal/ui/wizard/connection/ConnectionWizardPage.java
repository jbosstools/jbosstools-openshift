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
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.databinding.HostNameValidator;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.express.internal.ui.explorer.AbstractLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUpdatingJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private final ConnectionWizardPageModel pageModel;

	private Text rhLoginText = null;
	private Text passwordText = null;

	public ConnectionWizardPage(IWizard wizard, IConnectionAwareModel wizardModel) {
		super("Sign in to OpenShift", "Please provide your OpenShift credentials.", "Server Connection",
				wizard);
		this.pageModel = new ConnectionWizardPageModel(wizardModel);
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

		// use default server
		Button defaultServerCheckbox = new Button(container, SWT.CHECK);
		defaultServerCheckbox.setText("Use default server");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).applyTo(defaultServerCheckbox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(defaultServerCheckbox))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULTSERVER).observe(pageModel))
				.in(dbc);

		// server
		Label serverLabel = new Label(container, SWT.NONE);
		serverLabel.setText("&Server:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(serverLabel);
		Combo serversCombo = new Combo(container, SWT.BORDER);
		Binding serverBinding = ValueBindingBuilder
				.bind(WidgetProperties.text().observe(serversCombo))
				.validatingAfterGet(new HostNameValidator())
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SERVER).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(serverBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		ComboViewer serversComboViewer = new ComboViewer(serversCombo);
		serversComboViewer.setLabelProvider(new ServerLabelProvider());
		serversComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		serversComboViewer.setInput(pageModel.getServers());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(serversCombo);
		ValueBindingBuilder
				.bind(ViewerProperties.singlePostSelection().observe(serversComboViewer))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SERVER).observe(pageModel))
				.in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(serversCombo))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULTSERVER).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		// username
		Label rhLoginLabel = new Label(container, SWT.NONE);
		rhLoginLabel.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(rhLoginLabel);
		rhLoginText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1).applyTo(rhLoginText);
		Binding usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(rhLoginText))
				.validatingAfterGet(new RequiredStringValidator("username"))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USERNAME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordText);
		Binding passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.validatingAfterGet(new RequiredStringValidator("password"))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_PASSWORD).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Button rememberPasswordCheckBox = new Button(container, SWT.CHECK);
		rememberPasswordCheckBox.setText(OpenshiftUIMessages.OpenshiftWizardSavePassword);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(rememberPasswordCheckBox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(rememberPasswordCheckBox))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_REMEMBER_PASSWORD).observe(pageModel))
				.in(dbc);
		// credentials status
		IObservableValue credentialsStatusObservable =
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_VALID, IStatus.class).observe(pageModel);
		final CredentialsValidator credentialsValidator =
				new CredentialsValidator(credentialsStatusObservable);
		dbc.addValidationStatusProvider(credentialsValidator);
		ControlDecorationSupport.create(credentialsValidator, SWT.LEFT | SWT.TOP);

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
	protected void onPageActivated(DataBindingContext dbc) {
		super.onPageActivated(dbc);
		if (StringUtils.isEmpty(pageModel.getUsername())
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
		event.doit = connect();
		if (!event.doit) {
			passwordText.setFocus();
			passwordText.selectAll();
		}
	}

	public boolean connect() {
		try {
			WizardUtils.runInWizard(new ConnectJob(), new DelegatingProgressMonitor(), getContainer(),
					getDatabindingContext());
			return JobUtils.isOk(pageModel.getValid());
		} catch (InterruptedException e) {
			Logger.error("Failed to authenticate on OpenShift", e);
			return false;
		} catch (InvocationTargetException e) {
			Logger.error("Failed to authenticate on OpenShift", e);
			return false;
		}
	}

	class CredentialsValidator extends MultiValidator {

		private final IObservableValue credentialsStatusObservable;

		public CredentialsValidator(IObservableValue credentialsStatusObservable) {
			this.credentialsStatusObservable = credentialsStatusObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus credentialsValidityStatus = (IStatus) credentialsStatusObservable.getValue();

			if (credentialsValidityStatus != null) {
				return pageModel.getValid();
			}
			return ValidationStatus.ok();
		}
	}

	private class ServerLabelProvider extends AbstractLabelProvider {
		@Override
		public String getText(Object element) {
			if (!(element instanceof String)) {
				return null;
			}
			return (String) element;
		}
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	private class ConnectJob extends UIUpdatingJob {

		private IStatus connectionStatus;

		private ConnectJob() {
			super("Verifying user credentials...");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			connectionStatus = pageModel.connect();
			monitor.done();
			return Status.OK_STATUS;
		}

		@Override
		protected IStatus updateUI(IProgressMonitor monitor) {
			if (!JobUtils.isOk(connectionStatus)) {
				return Status.OK_STATUS;
			}
			boolean newConnection = false;
			if (pageModel.shouldCreateNewConnection()) {
				Connection connection = pageModel.getConnection();
				if (MessageDialog.openQuestion(getShell(),
						"Create new connection?",
						NLS.bind("You changed your connection to {0} on server {1}.\n" +
								"Do you want to create a new connection?",
								connection.getUsername(), connection.getHost()))) {
					newConnection = true;
				}
			}
			pageModel.createOrUpdateConnection(newConnection);
			return Status.OK_STATUS;
		}
	}

	public Connection getConnection(){
		return pageModel.getConnection();
	}

}
