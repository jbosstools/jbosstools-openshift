/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @contributor Nick Boldt
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	private final ConnectionWizardPageModel pageModel;
	private ConnectionEditorsStackedView connectionEditors;
	private StyledText userdocLink;
	private StyledText signupLink;

	public ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel) {
		this(wizard, wizardModel, true);
	}

	public ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel, Class<? extends IConnection> connectionType) {
		this(wizard, wizardModel, connectionType, true);
	}

	protected ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel, boolean allowConnectionChange) {
		this(wizard, wizardModel, null, allowConnectionChange);
	}

	protected ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel, Class<? extends IConnection> connectionType, 
			boolean allowConnectionChange) {
		super("Sign in to OpenShift", "Please sign in to your OpenShift server.", "Server Connection", wizard);
		this.pageModel = new ConnectionWizardPageModel(
				wizardModel.getConnection(), 
				ConnectionsRegistrySingleton.getInstance().getAll(), 
				connectionType, 
				allowConnectionChange, 
				wizardModel);

		/*
		 * JBIDE-12999: ensure EclipseAuthenticator is installed and overrides
		 * NetAuthenticator
		 */
		EGitUIUtils.ensureEgitUIIsStarted();
	}

	protected void doCreateControls(final Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);

		//JBIDE-20361 signup link doesn't work on OSX in some situations. 
		//Link signupLink = new Link(parent, SWT.WRAP);
		//signupLink.setText("If you do not have an account on OpenShift, please <a>sign up here</a>.");
		this.signupLink = new StyledText(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(signupLink);;
		showHideSignupLink();
		StyledTextUtils.emulateLinkAction(signupLink, r->onSignupLinkClicked());
		IObservableValue signupUrlObservable = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SIGNUPURL).observe(pageModel);
		signupUrlObservable.addValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				showHideSignupLink();
			}
		});

		// userdoc link (JBIDE-20401)
		this.userdocLink = new StyledText(parent, SWT.WRAP); // text set in #showHideUserdocLink
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(userdocLink);
		showHideUserdocLink();
		IObservableValue userdocUrlObservable = BeanProperties
				.value(ConnectionWizardPageModel.PROPERTY_USERDOCURL).observe(pageModel);
		StyledTextUtils.emulateLinkAction(userdocLink, r->onUserdocLinkClicked(userdocUrlObservable));
		userdocUrlObservable.addValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				showHideUserdocLink();
			}
		});
		
		IObservableValue connectionFactoryObservable =
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY).observe(pageModel);
		
		// filler
		Label fillerLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(3, 3).hint(SWT.DEFAULT, 6).applyTo(fillerLabel);

		// existing connections combo
		Label connectionLabel = new Label(parent, SWT.NONE);
		connectionLabel.setText("Connection:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(connectionLabel);
		Combo connectionCombo = new Combo(parent, SWT.DEFAULT);
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(connectionCombo);
		ComboViewer connectionComboViewer = new ComboViewer(connectionCombo);
		connectionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		connectionComboViewer.setLabelProvider(new ConnectionColumLabelProvider());
		connectionComboViewer.setInput(pageModel.getAllConnections());
		Binding selectedConnectionBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(connectionComboViewer))
				.validatingAfterGet(
						new IsNotNullValidator(
								ValidationStatus.cancel("You have to select or create a new connection.")))
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION, IConnection.class)
						.observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(selectedConnectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// server type
		Label connectionFactoryLabel = new Label(parent, SWT.NONE);
		connectionFactoryLabel.setText("Server type:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(connectionFactoryLabel);
		Combo connectionFactoryCombo = new Combo(parent, SWT.DEFAULT);
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(connectionFactoryCombo);
		ComboViewer connectionFactoriesViewer = new ComboViewer(connectionFactoryCombo);
		connectionFactoriesViewer.setContentProvider(ArrayContentProvider.getInstance());
		connectionFactoriesViewer.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (!(element instanceof IConnectionFactory)) {
					return element.toString();
				} else {
					return ((IConnectionFactory) element).getName();
				}
			}
		});
		connectionFactoriesViewer.setInput(pageModel.getAllConnectionFactories());
		final IViewerObservableValue selectedServerType = ViewerProperties.singleSelection().observe(connectionFactoriesViewer);
		ValueBindingBuilder
				.bind(selectedServerType)
				.to(connectionFactoryObservable)
				.in(dbc);

		// server
		Button useDefaultServerCheckbox = new Button(parent, SWT.CHECK);
		useDefaultServerCheckbox.setText("Use default server");
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).applyTo(useDefaultServerCheckbox);
		ValueBindingBuilder
		.bind(WidgetProperties.selection().observe(useDefaultServerCheckbox))
		.to(BeanProperties.value(
				ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST, IConnection.class).observe(pageModel))
		.in(dbc);
		
		IObservableValue hasDefaultHostObservable =
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HAS_DEFAULT_HOST).observe(pageModel);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(useDefaultServerCheckbox))
			.notUpdating(hasDefaultHostObservable).in(dbc);

		Label serverLabel = new Label(parent, SWT.NONE);
		serverLabel.setText("Server:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(serverLabel);
		Combo serversCombo = new Combo(parent, SWT.BORDER);
		ComboViewer serversViewer = new ComboViewer(serversCombo);
		serversViewer.setContentProvider(new ObservableListContentProvider());
		serversViewer.setInput(BeanProperties.list(ConnectionWizardPageModel.PROPERTY_ALL_HOSTS).observe(pageModel));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(serversCombo);
		final IObservableValue serverUrlObservable = WidgetProperties.text().observe(serversCombo);
		serversCombo.addFocusListener(onServerFocusLost(serverUrlObservable));
		ValueBindingBuilder.bind(serverUrlObservable)
				.converting(new TrimTrailingSlashConverter())
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HOST).observe(pageModel))
				.in(dbc);
		
		MultiValidator serverUrlValidator = new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				Object value = serverUrlObservable.getValue();
				if (!(value instanceof String)
						|| StringUtils.isEmpty((String) value)) {
					return ValidationStatus.cancel("Please provide an OpenShift server url.");
				} else if (!UrlUtils.isValid((String) value)) {
					return ValidationStatus.error("Please provide a valid OpenShift server url.");
				}
				return ValidationStatus.ok();
			}
		};
		ControlDecorationSupport
			.create(serverUrlValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		dbc.addValidationStatusProvider(serverUrlValidator);

		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(serversCombo))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		// connect error
		dbc.addValidationStatusProvider(new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				return (IStatus) BeanProperties
						.value(ConnectionWizardPageModel.PROPERTY_CONNECTED_STATUS, IStatus.class)
						.observe(pageModel).getValue();
			}
		});

		// connection editors
		Group authenticationDetailsGroup = new Group(parent, SWT.NONE);
		authenticationDetailsGroup.setText("Authentication");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).span(3,1).applyTo(authenticationDetailsGroup);
		GridLayoutFactory.fillDefaults()
			.margins(0, 0).applyTo(authenticationDetailsGroup);
		// additional nesting required because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
		Composite authenticationDetailsContainer = new Composite(authenticationDetailsGroup, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(authenticationDetailsContainer);
		this.connectionEditors = new ConnectionEditorsStackedView(
				connectionFactoryObservable
				, this
				, authenticationDetailsContainer
				, dbc);
		connectionEditors.createControls();
	}

	private void showHideSignupLink() {
		boolean userdocExists = !StringUtils.isEmpty(pageModel.getSignupUrl());
		IConnectionFactory factory = pageModel.getConnectionFactory();
		if (factory != null) {
			StyledTextUtils.emulateLinkWidget(NLS.bind("If you do not have an account on {0}, please <a>sign up here</a>.", factory.getName()), signupLink);
		}
		UIUtils.setVisibleAndExclude(userdocExists, signupLink);
	}

	private void showHideUserdocLink() {
		boolean signupUrlExists = !StringUtils.isEmpty(pageModel.getUserdocUrl());
		if (signupUrlExists) {
			IConnectionFactory factory = pageModel.getConnectionFactory();
			if (factory != null) {
				StyledTextUtils.emulateLinkWidget(NLS.bind("New to OpenShift {0}? Explore the <a>getting started documentation</a>.", factory.getName()), userdocLink);
			}
		}
		UIUtils.setVisibleAndExclude(signupUrlExists, userdocLink);
	}

	private FocusAdapter onServerFocusLost(final IObservableValue serverUrlObservable) {
		return new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String value = (String) serverUrlObservable.getValue();
				if (StringUtils.isEmpty(value)) {
					return;
				}
				String url = value;
				if (!url.startsWith(UrlUtils.SCHEME_HTTP) 
						&& !url.contains(UrlUtils.SCHEME_TERMINATOR)) {
					url = UrlUtils.ensureStartsWithScheme(value, UrlUtils.SCHEME_HTTPS);
				}
				if (!url.endsWith(UrlUtils.SCHEME_SEPARATOR)) {
					url = StringUtils.removeTrailingSlashes(url);
				}
				url = org.apache.commons.lang.StringUtils.removeEnd(url, "/console");
				if (!url.equals(value)) {
					serverUrlObservable.setValue(url);
				}
			}
		};
	}

	protected void onSignupLinkClicked() {
		String signupUrl = pageModel.getSignupUrl();
		if (StringUtils.isEmpty(signupUrl)) {
			return;
		}
		new BrowserUtility().checkedCreateInternalBrowser(
				signupUrl,
				signupUrl,
				OpenShiftCommonUIActivator.PLUGIN_ID,
				OpenShiftCommonUIActivator.getDefault().getLog());
		WizardUtils.close(getWizard());
	}

	protected void onUserdocLinkClicked(final IObservableValue userdocUrlObservable) {
		String userdocUrl = (String) userdocUrlObservable.getValue();
		if (StringUtils.isEmpty(userdocUrl)) {
			return;
		}
		new BrowserUtility().checkedCreateExternalBrowser(
				userdocUrl,
				OpenShiftCommonUIActivator.PLUGIN_ID,
				OpenShiftCommonUIActivator.getDefault().getLog());
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		super.onPageActivated(dbc);
//		setInitialFocus();
	}

	@Override
	protected void onPageDeactivated(DataBindingContext dbc) {
		pageModel.saveRecentConnection();
	}

	@Override
	protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.BACKWARDS) {
			return;
		}
		if (!isConnected()) {
			event.doit = connect();
		}
		if (!event.doit) {
//			setInitialFocus();
		}
	}

	public boolean isConnected() {
		return getModel().isConnected();
	}
	
	public boolean connect() {
		try {
			ConnectJob connectJob = new ConnectJob();
			WizardUtils.runInWizard(
					connectJob, new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
			boolean connected = JobUtils.isOk(connectJob.getConnectionStatus());
			if (connected) {
				return pageModel.saveConnection();
			} else {
				return false;
			}
		} catch (InterruptedException e) {
			OpenShiftCommonUIActivator.log(NLS.bind("Failed to authenticate user on server at {1}", pageModel.getHost()), e);
			return false;
		} catch (InvocationTargetException e) {
			OpenShiftCommonUIActivator.log(NLS.bind("Failed to authenticate user on server at {1}", pageModel.getHost()), e);
			return false;
		}
	}
	
//	private void setInitialFocus() {
//		if (pageModel.isCreateNewConnection()) {
//			if (connectionCompositeUsernameText.getText().isEmpty()) {
//				connectionCompositeUsernameText.setFocus();
//			} else {
//				connectionCompositePasswordText.setFocus();
//				connectionCompositePasswordText.selectAll();
//			}
//		} else {
//			passwordCompositePasswordText.setFocus();
//			passwordCompositePasswordText.selectAll();
//		}
//	}

	public IConnection getConnection() {
		return pageModel.getConnection();
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	@Override
	public void dispose() {
		pageModel.dispose();
	}

	public ConnectionWizardPageModel getModel() {
		return pageModel;
	}
	
	private class ConnectJob extends AbstractDelegatingMonitorJob {

		private IStatus connectionStatus;

		private ConnectJob() {
			super("Verifying user credentials...");
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			connectionStatus = pageModel.connect();
			monitor.done();
			return Status.OK_STATUS;
		}

		public IStatus getConnectionStatus() {
			return connectionStatus;
		}
	}
}
