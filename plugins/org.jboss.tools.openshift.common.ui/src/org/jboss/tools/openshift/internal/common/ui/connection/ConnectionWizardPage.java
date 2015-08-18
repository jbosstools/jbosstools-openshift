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
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
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
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotEmptyString2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @contributor Nick Boldt
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	private static final String COL_SLASHES = "://";
	private final ConnectionWizardPageModel pageModel;
	private ConnectionEditorsStackedView connectionEditors;

	public ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel) {
		this(wizard, wizardModel, true);
	}

	protected ConnectionWizardPage(IWizard wizard, IConnectionAware<IConnection> wizardModel, boolean allowConnectionChange) {
		super("Sign in to OpenShift", "Please sign in to your OpenShift server.", "Server Connection",
				wizard);
		this.pageModel = new ConnectionWizardPageModel(wizardModel.getConnection(), ConnectionsRegistrySingleton.getInstance().getAll(), allowConnectionChange, wizardModel);
		/*
		 * JBIDE-12999: ensure EclipseAuthenticator is installed and overrides
		 * NetAuthenticator
		 */
		EGitUIUtils.ensureEgitUIIsStarted();
	}

	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);

		// signup link
		Link signupLink = new Link(parent, SWT.WRAP);
		signupLink.setText("If you do not have an account on OpenShift, please <a>sign up here</a>.");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(signupLink);
		IObservableValue signupUrlObservable = BeanProperties
				.value(ConnectionWizardPageModel.PROPERTY_SIGNUPURL).observe(pageModel);
		signupLink.addSelectionListener(onSignupLinkClicked(signupUrlObservable));
		ValueBindingBuilder
			.bind(WidgetProperties.visible().observe(signupLink))
			.notUpdatingParticipant()
			.to(signupUrlObservable)
			.converting(new IsNotEmptyString2BooleanConverter())
			.in(dbc);

		// userdoc link (JBIDE-20401)
		final Link userdocLink = new Link(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(userdocLink);
		IObservableValue userdocUrlObservable = BeanProperties
				.value(ConnectionWizardPageModel.PROPERTY_USERDOCURL).observe(pageModel);
		userdocLink.addSelectionListener(onUserdocLinkClicked(userdocUrlObservable));
		ValueBindingBuilder
			.bind(WidgetProperties.visible().observe(userdocLink))
			.notUpdatingParticipant()
			.to(userdocUrlObservable)
			.converting(new IsNotEmptyString2BooleanConverter())
			.in(dbc);

		IObservableValue connectionFactoryObservable =
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY).observe(pageModel);

		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(userdocLink))
			.notUpdatingParticipant()
			.to(connectionFactoryObservable)
			.converting(new LinkConverter())
			.in(dbc);

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
		useDefaultServerCheckbox.setText("Use default Server");
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).applyTo(useDefaultServerCheckbox);
		ValueBindingBuilder
		.bind(WidgetProperties.selection().observe(useDefaultServerCheckbox))
		.to(BeanProperties.value(
				ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST, IConnection.class).observe(pageModel))
		.in(dbc);

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
		serversCombo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String value = (String)serverUrlObservable.getValue();
				if (value == null) {
					return;
				}
				String url = value;
				if (!url.startsWith(UrlUtils.SCHEME_HTTP) && !url.contains(":")) {
					url = UrlUtils.ensureStartsWithScheme(value, UrlUtils.SCHEME_HTTPS);
				}
				if (!url.endsWith(COL_SLASHES)) {
					url = StringUtils.removeTrailingSlashes(url);
				}
				if (!url.equals(value)){
					serverUrlObservable.setValue(url);
				}
			}
		});
		Binding serverUrlBinding = ValueBindingBuilder
				.bind(serverUrlObservable)
				.converting(new TrimTrailingSlashConverter())
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof String)
								|| StringUtils.isEmpty((String) value)) {
							return ValidationStatus.cancel("Please provide an OpenShift server url.");
						} else if (!UrlUtils.isValid((String) value)) {
							return ValidationStatus.error("Please provide a valid OpenShift server url.");
						}
						return ValidationStatus.ok();
					}
				})
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HOST).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(serverUrlBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

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
						.value(ConnectionWizardPageModel.PROPERTY_CONNECT_ERROR, IStatus.class)
						.observe(pageModel).getValue();
			}
		});

		// connection editors
		final Group connectionEditorsContainer = new Group(parent, SWT.NONE);
		connectionEditorsContainer.setText("Authentication");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).span(3,1).applyTo(connectionEditorsContainer);
		this.connectionEditors = new ConnectionEditorsStackedView(
				connectionFactoryObservable
				, this
				, connectionEditorsContainer
				, dbc);
		connectionEditors.createControls();
	}

	protected SelectionAdapter onSignupLinkClicked(final IObservableValue signupUrlObservable) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String signupUrl = (String) signupUrlObservable.getValue();
				if (StringUtils.isEmpty(signupUrl)) {
					return;
				}
				new BrowserUtility().checkedCreateInternalBrowser(
						signupUrl,
						signupUrl,
						OpenShiftCommonUIActivator.PLUGIN_ID,
						OpenShiftCommonUIActivator.getDefault().getLog());
				WizardUtils.close(getWizard());;
			}
		};
	}

	protected SelectionAdapter onUserdocLinkClicked(final IObservableValue userdocUrlObservable) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String userdocUrl = (String) userdocUrlObservable.getValue();
				if (StringUtils.isEmpty(userdocUrl)) {
					return;
				}
				new BrowserUtility().checkedCreateExternalBrowser(
						userdocUrl,
						OpenShiftCommonUIActivator.PLUGIN_ID,
						OpenShiftCommonUIActivator.getDefault().getLog());
			}
		};
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
		event.doit = connect();
		if (!event.doit) {
//			setInitialFocus();
		}
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

	private static class LinkConverter extends Converter {

		public LinkConverter() {
			super(IConnectionFactory.class, String.class);
		}

		@Override
		public Object convert(Object fromObject) {
			return NLS.bind("New to {0}? Explore the <a>getting started documentation</a>.", ((IConnectionFactory)fromObject).getName());
		}

	}
}
