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
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.jboss.tools.openshift.common.core.connection.AutomaticConnectionFactoryMarker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.HttpsPrefixingAdapter;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

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

		Link signupLink = new Link(parent, SWT.WRAP);
		signupLink.setText("If you do not have an account on OpenShift, please sign up <a>here</a>.");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(signupLink);
		signupLink.addSelectionListener(onSignupLinkClicked());

		Label fillerLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(3, 4).hint(SWT.DEFAULT, 6).applyTo(fillerLabel);

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
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY, IConnection.class)
						.observe(pageModel))
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
		new HttpsPrefixingAdapter().addTo(serversCombo);
		ComboViewer serversViewer = new ComboViewer(serversCombo);
		serversViewer.setContentProvider(new ObservableListContentProvider());
		serversViewer.setInput(BeanProperties.list(ConnectionWizardPageModel.PROPERTY_ALL_HOSTS).observe(pageModel));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(serversCombo);
		final IObservableValue serverUrlObservable = WidgetProperties.text().observe(serversCombo);
		Binding serverUrlBinding = ValueBindingBuilder
				.bind(serverUrlObservable)
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof String)
								|| StringUtils.isEmpty((String) value)) {
							return ValidationStatus.cancel("Please provide an url to an OpenShift server.");
						} else if (!UrlUtils.isValid((String) value)) {
							return ValidationStatus.error("Please provide a valid url to an OpenShift server.");
						}
						return ValidationStatus.ok();
					}
				})
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_HOST, IConnection.class).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport
				.create(serverUrlBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// check server type enablement
		final IObservableValue connectionFactory = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY).observe(pageModel);
		final IObservableValue serverValidity = serverUrlBinding.getValidationStatus();
		IObservableValue checkServerTypeEnabled = new ComputedValue(Boolean.class) {
			
			@Override
			protected Object calculate() {
				return connectionFactory.getValue() instanceof AutomaticConnectionFactoryMarker
						&& serverValidity.getValue() instanceof IStatus
							&& ((IStatus) serverValidity.getValue()).isOK();
			}
		};

		serversCombo.addKeyListener(onSeverEnterPressed(checkServerTypeEnabled));
		
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(serversCombo))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		// check server type
		Button checkServerTypeButton = new Button(parent, SWT.PUSH);
		checkServerTypeButton.setText("Check Server Type");
		checkServerTypeButton.addSelectionListener(onCheckServerType());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(160, SWT.DEFAULT).applyTo(checkServerTypeButton);
		final ISWTObservableValue checkServerTypeButtonEnablement = WidgetProperties.enabled().observe(checkServerTypeButton);
		ValueBindingBuilder
				.bind(checkServerTypeButtonEnablement)
				.notUpdatingParticipant()
				.to(checkServerTypeEnabled)
				.in(dbc);

		// check server type requirement
		MultiValidator serverTypeValidator = new MultiValidator() {

			@Override
			protected IStatus validate() {
				if (selectedServerType.getValue() == null
						|| selectedServerType.getValue() instanceof AutomaticConnectionFactoryMarker) {
					return ValidationStatus.cancel("You have to provide a valid url to an OpenShift server and check its type.");
				}
				return ValidationStatus.ok();
			}

			@Override
			public IObservableList getTargets() {
				return Observables.staticObservableList(Arrays.asList(selectedServerType, checkServerTypeButtonEnablement));
			}
		};
		dbc.addValidationStatusProvider(serverTypeValidator);
		ControlDecorationSupport
				.create(serverTypeValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		// connect error
		dbc.addValidationStatusProvider(new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				return (IStatus) BeanProperties
						.value(ConnectionWizardPageModel.PROPERTY_CONNECT_ERROR, IStatus.class)
						.observe(pageModel).getValue();
			}
		});

		// check server type error
		MultiValidator connectionFactoryValidator = new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				return (IStatus) BeanProperties
						.value(ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY_ERROR, IStatus.class)
						.observe(pageModel).getValue();
			}

			@Override
			public IObservableList getTargets() {
				return Observables.staticObservableList(
						Collections.<IObservableValue> singletonList(serverUrlObservable));
			}
		};
		dbc.addValidationStatusProvider(connectionFactoryValidator);
		ControlDecorationSupport
				.create(connectionFactoryValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// connection editors
		final Group connectionEditorsContainer = new Group(parent, SWT.NONE);
		connectionEditorsContainer.setText("Authentication");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).span(3,1).applyTo(connectionEditorsContainer);
		this.connectionEditors = new ConnectionEditorsStackedView(
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY).observe(pageModel)
				, pageModel
				, connectionEditorsContainer
				, dbc);
		connectionEditors.createControls();
	}

	private KeyListener onSeverEnterPressed(final IObservableValue checkServerTypeEnabled) {
		return new KeyAdapter() {
			
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.character == SWT.CR 
						&& Boolean.TRUE.equals(checkServerTypeEnabled.getValue())) {
					try {
						WizardUtils.runInWizard(new CreateConnectionFactoryJob(), new DelegatingProgressMonitor(), getContainer());
					} catch (InvocationTargetException e) {
						// intentional catch
					} catch (InterruptedException e) {
						// intentional catch
					}
				}
			}
		};
	}

	private SelectionListener onCheckServerType() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					WizardUtils.runInWizard(new CreateConnectionFactoryJob(), new DelegatingProgressMonitor(), getContainer());
				} catch (InvocationTargetException e) {
					// intentional catch
				} catch (InterruptedException e) {
					// intentional catch
				}
			}
		};
	}

	protected SelectionAdapter onSignupLinkClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				new BrowserUtility().checkedCreateInternalBrowser(
						OPENSHIFT_EXPRESS_SIGNUP_URL, 
						OPENSHIFT_EXPRESS_SIGNUP_URL, 
						OpenShiftCommonUIActivator.PLUGIN_ID, 
						OpenShiftCommonUIActivator.getDefault().getLog());
				org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils.close(getWizard());;
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

	protected ConnectionWizardPageModel getModel() {
		return pageModel;
	}
	
	private class CreateConnectionFactoryJob extends AbstractDelegatingMonitorJob {

		private CreateConnectionFactoryJob() {
			super(NLS.bind("Connecting to host {0}...", pageModel.getHost()));
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			pageModel.createConnectionFactory();
			return Status.OK_STATUS;
		}
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
