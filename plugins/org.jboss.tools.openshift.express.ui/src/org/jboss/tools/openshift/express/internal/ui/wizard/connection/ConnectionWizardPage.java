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
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.express.internal.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.express.internal.ui.explorer.AbstractLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUpdatingJob;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.NewConnectionAwareConnectionComparer;
import org.jboss.tools.openshift.express.internal.ui.viewer.NewConnectionMarker;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private final ConnectionWizardPageModel pageModel;
	private Text connectionCompositeUsernameText;
	private Text connectionCompositePasswordText;
	private Text passwordCompositePasswordText;

	public ConnectionWizardPage(IWizard wizard, IConnectionAwareModel wizardModel) {
		super("Sign in to OpenShift", "Please provide your OpenShift credentials.", "Server Connection",
				wizard);
		this.pageModel = new ConnectionWizardPageModel(wizardModel);
		/*
		 * JBIDE-12999: ensure EclipseAuthenticator is installed and overrides
		 * NetAuthenticator
		 */
		EGitUIUtils.ensureEgitUIIsStarted();
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

		Label connectionLabel = new Label(container, SWT.NONE);
		connectionLabel.setText("Connection:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(connectionLabel);
		Combo connectionCombo = new Combo(container, SWT.DEFAULT);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(connectionCombo);
		final ComboViewer connectionComboViewer = new ComboViewer(connectionCombo);
		connectionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		connectionComboViewer.setLabelProvider(new ConnectionColumLabelProvider());
		connectionComboViewer.setInput(pageModel.getConnections());
		connectionComboViewer.setComparer(new NewConnectionAwareConnectionComparer());

		Binding selectedConnectionBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(connectionComboViewer))
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (value == null) {
							return ValidationStatus.cancel("You have to select or create a new connection.");
						}
						return ValidationStatus.ok();
					}
				})
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION, Connection.class).observe(
						pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(selectedConnectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// stack with connection widgets / password widget
		Composite connectionWidgetsContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).applyTo(connectionWidgetsContainer);
		StackLayout stackLayout = new StackLayout();
		connectionWidgetsContainer.setLayout(stackLayout);
		Composite connectionWidgets = createNewConnectionComposite(connectionWidgetsContainer, dbc);
		Composite passwordWidgets = createExistingConnectionComposite(connectionWidgetsContainer, dbc);

		showConnectionWidgets(pageModel.isCreateNewConnection(), passwordWidgets, connectionWidgets, stackLayout,
				connectionWidgetsContainer);

		BeanProperties
				.value(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION)
				.observe(pageModel)
				.addValueChangeListener(
						onNewConnectionSelected(passwordWidgets, connectionWidgets, stackLayout,
								connectionWidgetsContainer));
	}

	private IValueChangeListener onNewConnectionSelected(final Composite passwordWidget,
			final Composite connectionWidgets, final StackLayout stackLayout, final Composite container) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				Connection selectedConnection = pageModel.getSelectedConnection();
				boolean isNewConnection = selectedConnection instanceof NewConnectionMarker;
				showConnectionWidgets(isNewConnection, passwordWidget, connectionWidgets, stackLayout, container);
			}
		};
	}

	private void showConnectionWidgets(boolean isNewConnection, Composite passwordWidgets,
			Composite connectionsWidgets,
			StackLayout stackLayout, Composite container) {
		Control topControl = null;
		if (isNewConnection) {
			topControl = connectionsWidgets;
		} else {
			topControl = passwordWidgets;
		}
		stackLayout.topControl = topControl;
		container.layout();
	}

	private Composite createExistingConnectionComposite(Composite container, DataBindingContext dbc) {
		Composite passwordWidgets = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).applyTo(passwordWidgets);

		// password
		Label passwordLabel = new Label(passwordWidgets, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(passwordLabel);
		this.passwordCompositePasswordText = new Text(passwordWidgets, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordCompositePasswordText);
		Binding passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordCompositePasswordText))
				.validatingAfterGet(new RequiredStringValidator("password"))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_PASSWORD).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// remember password
		Button rememberPasswordCheckBox = new Button(passwordWidgets, SWT.CHECK);
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

		return passwordWidgets;
	}

	private Composite createNewConnectionComposite(Composite container, DataBindingContext dbc) {
		Composite connectionWidgets = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).applyTo(connectionWidgets);

		// use default server
		Button defaultServerCheckbox = new Button(connectionWidgets, SWT.CHECK);
		defaultServerCheckbox.setText("Use default server");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).applyTo(defaultServerCheckbox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(defaultServerCheckbox))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULTSERVER).observe(pageModel))
				.in(dbc);

		// host
		Label serverLabel = new Label(connectionWidgets, SWT.NONE);
		serverLabel.setText("&Server:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(serverLabel);
		Combo serversCombo = new Combo(connectionWidgets, SWT.BORDER);
		Binding serverBinding = ValueBindingBuilder
				.bind(WidgetProperties.text().observe(serversCombo))
				.validatingAfterGet(new RequiredStringValidator("server"))
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HOST).observe(pageModel))
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
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HOST).observe(pageModel))
				.in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(serversCombo))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULTSERVER).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		// username
		Label rhLoginLabel = new Label(connectionWidgets, SWT.NONE);
		rhLoginLabel.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(rhLoginLabel);
		connectionCompositeUsernameText = new Text(connectionWidgets, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(connectionCompositeUsernameText);
				IObservableValue usernameObservable = WidgetProperties.text(SWT.Modify).observe(connectionCompositeUsernameText);
				ValueBindingBuilder
						.bind(usernameObservable)
		 				.converting(new TrimmingStringConverter())
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USERNAME).observe(pageModel))
				.in(dbc);
		ValidationStatusProvider usernameValidation = 
				new RequiredStringValidationProvider(usernameObservable, "username");
		dbc.addValidationStatusProvider(usernameValidation);
		ControlDecorationSupport
				.create(usernameValidation, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		Label passwordLabel = new Label(connectionWidgets, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		this.connectionCompositePasswordText = new Text(connectionWidgets, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(connectionCompositePasswordText);
		IObservableValue passwordObservable = WidgetProperties.text(SWT.Modify).observe(connectionCompositePasswordText);
		ValueBindingBuilder
				.bind(passwordObservable)
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_PASSWORD).observe(pageModel))
				.in(dbc);
		ValidationStatusProvider passwordValidation =
				new RequiredStringValidationProvider(passwordObservable, "password");
		dbc.addValidationStatusProvider(passwordValidation);
		ControlDecorationSupport
				.create(passwordValidation, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Button rememberPasswordCheckBox = new Button(connectionWidgets, SWT.CHECK);
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

		return connectionWidgets;
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
		setInitialFocus();
	}

	@Override
	protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.BACKWARDS) {
			return;
		}
		event.doit = connect();
		if (!event.doit) {
			setInitialFocus();
		}
	}
	
	private void setInitialFocus() {
		if (pageModel.isCreateNewConnection()) {
			if (connectionCompositeUsernameText.getText().isEmpty()) {
				connectionCompositeUsernameText.setFocus();
			} else {
				connectionCompositePasswordText.setFocus();
				connectionCompositePasswordText.selectAll();
			}
		} else {
			passwordCompositePasswordText.setFocus();
			passwordCompositePasswordText.selectAll();
		}
	}

	public boolean connect() {
		try {
			WizardUtils.runInWizard(
					new ConnectJob(), new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
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

	class RequiredStringValidationProvider extends MultiValidator {

		private IObservableValue observableValue;
		private String name;

		public RequiredStringValidationProvider(IObservableValue value, String name) {
			this.observableValue = value;
			this.name = name;
		}

		@Override
		protected IStatus validate() {
			if (!(observableValue.getValue() instanceof String)) {
				return ValidationStatus.cancel("You have to provide a " + name);
			}
			String string = (String) observableValue.getValue();
			if (string.isEmpty()) {
				return ValidationStatus.cancel("You have to provide a " + name);
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			IObservableList targets = new WritableList();
			targets.add(observableValue);
			return targets;
		}

	}		

	public Connection getConnection() {
		return pageModel.getConnection();
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
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
			pageModel.createOrUpdateConnection();

			return Status.OK_STATUS;
		}
	}
}
