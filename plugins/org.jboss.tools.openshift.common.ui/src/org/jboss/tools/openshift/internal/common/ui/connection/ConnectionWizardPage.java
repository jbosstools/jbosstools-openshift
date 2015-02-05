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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.StatusSeverity2BooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.viewer.AbstractLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPage extends AbstractOpenShiftWizardPage {

	protected static final String OPENSHIFT_EXPRESS_SIGNUP_URL = "https://openshift.redhat.com/app/user/new/express"; //$NON-NLS-1$

	private final ConnectionWizardPageModel pageModel;

	public ConnectionWizardPage(IWizard wizard, IConnectionAwareModel wizardModel) {
		this(wizard, wizardModel, true);
	}

	protected ConnectionWizardPage(IWizard wizard, IConnectionAwareModel wizardModel, boolean allowConnectionChange) {
		super("Sign in to OpenShift", "Please provide your OpenShift credentials.", "Server Connection",
				wizard);
		this.pageModel = new ConnectionWizardPageModel(wizardModel, allowConnectionChange);
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
		connectionComboViewer.setInput(pageModel.getConnections());
		Binding selectedConnectionBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(connectionComboViewer))
				.validatingAfterGet(
						new IsNotNullValidator(
								ValidationStatus.cancel("You have to select or create a new connection.")))
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_CONNECTION, IConnection.class)
						.observe(pageModel))
				.converting(new Converter(IConnection.class, IConnection.class) {

					@Override
					public Object convert(Object fromObject) {
						if (!pageModel.getConnections().contains(fromObject)) {
							// return <New Connection> if new connection is selected in model
							return NewConnectionMarker.getInstance();
						} else {
							return fromObject;
						}
					}})
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
		connectionFactoriesViewer.setInput(pageModel.getConnectionFactories());
		ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(connectionFactoriesViewer))
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_CONNECTION_FACTORY, IConnection.class)
						.observe(pageModel))
				.in(dbc);
		
		// use default server
		Button useDefaultServerCheckbox = new Button(parent, SWT.CHECK);
		useDefaultServerCheckbox.setText("Use default Server");
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).applyTo(useDefaultServerCheckbox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(useDefaultServerCheckbox))
				.to(BeanProperties.value(
						ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST, IConnection.class).observe(pageModel))
				.in(dbc);
		
		// server
		Label serverLabel = new Label(parent, SWT.NONE);
		serverLabel.setText("Server:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(serverLabel);
		Text serverText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(serverText);
		Binding serverBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(serverText))
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
				.create(serverBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(serverText))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ConnectionWizardPageModel.PROPERTY_USE_DEFAULT_HOST).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);
		
		// test connection
		Button testConnectionButton = new Button(parent, SWT.PUSH);
		testConnectionButton.setText("Test Connection");
		testConnectionButton.addSelectionListener(onTestConnection());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(140, SWT.DEFAULT).applyTo(testConnectionButton);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(testConnectionButton))
				.notUpdatingParticipant()
				.to(serverBinding.getValidationStatus())
				.converting(new StatusSeverity2BooleanConverter(IStatus.OK))
				.in(dbc);		
		
		// connected status
		final IObservableValue connectedStatusObservable =
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTED, IStatus.class).observe(pageModel);
		dbc.addValidationStatusProvider(new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				IStatus connectedStatus = (IStatus) connectedStatusObservable.getValue();
				if (connectedStatus == null) {
					return ValidationStatus.cancel("Please either select an existing connection or provide a valid OpenShift host.");
				} else {
					return connectedStatus;
				}
			}
		});
		
		// connection details
		final Group connectionUIsContainer = new Group(parent, SWT.NONE);
		connectionUIsContainer.setText("Authentication");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).span(3,1).applyTo(connectionUIsContainer);
		new ConnectionUIViews(
				BeanProperties.value(ConnectionWizardPageModel.PROPERTY_CONNECTION).observe(pageModel)
				, connectionUIsContainer
				, dbc)
		.createControls();
		
	}
	
	private SelectionListener onTestConnection() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					WizardUtils.runInWizard(new CreateConnectionJob(), new DelegatingProgressMonitor(), getContainer());
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
//		event.doit = connect();
		if (!event.doit) {
//			setInitialFocus();
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

//	public boolean connect() {
//		try {
//			WizardUtils.runInWizard(
//					new CreateConnectionJob(), new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
//			return JobUtils.isOk(pageModel.getValid());
//		} catch (InterruptedException e) {
//			OpenShiftCommonUIActivator.log("Failed to authenticate on OpenShift", e);
//			return false;
//		} catch (InvocationTargetException e) {
//			OpenShiftCommonUIActivator.log("Failed to authenticate on OpenShift", e);
//			return false;
//		}
//	}

	public IConnection getConnection() {
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

	@Override
	public void dispose() {
		pageModel.dispose();
	}
	
	private class CreateConnectionJob extends AbstractDelegatingMonitorJob {

		private CreateConnectionJob() {
			super(NLS.bind("Connecting to host {0}...", pageModel.getHost()));
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			pageModel.createConnection();
			return Status.OK_STATUS;
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
