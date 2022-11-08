/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationUpdater;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationDetails;

/**
 * @author jeff.cantrill
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class OAuthDetailView extends BaseDetailsView implements IConnectionEditorDetailView {

	private static final String MSG_TOKEN = "Enter a token or <a>retrieve</a> a new one.";

	private IObservableValue<String> tokenObservable;
	private Binding tokenBinding;
	private Text tokenText;
	private IValueChangeListener<?> changeListener;
	private IObservableValue<Boolean> rememberTokenObservable;
	private IObservableValue<String> authSchemeObservable;
	private IAuthorizationDetails authDetails;
	private ConnectionWizardPageModel pageModel;
	IObservableValue<String> urlObservable;
	private Button rememberTokenCheckbox;
	private Binding rememberTokenBinding;

	private MultiValidator connectionValidator;

	private IWizard wizard;

	public OAuthDetailView(IWizard wizard, ConnectionWizardPageModel pageModel, IValueChangeListener<?> changeListener,
			Object context, IObservableValue<String> authSchemeObservable) {
		this.wizard = wizard;
		this.pageModel = pageModel;
		this.urlObservable = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_HOST, String.class).observe(pageModel);
		this.tokenObservable = new WritableValue<String>(null, String.class);
		this.rememberTokenObservable = new WritableValue<Boolean>(Boolean.FALSE, Boolean.class);
		this.connectionValidator = ConnectionValidatorFactory.createOAuthAuthenticationValidator(pageModel,
				tokenObservable, urlObservable);
		this.authSchemeObservable = authSchemeObservable;
		this.changeListener = changeListener;
		if (context instanceof IAuthorizationDetails) {
			this.authDetails = (IAuthorizationDetails) context;
		}
	}

	IObservableValue<Boolean> getRememberTokenObservable() {
		return rememberTokenObservable;
	}

	public final Text getTokenTextControl() {
		return tokenText;
	}

	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 10).applyTo(composite);

		StyledText tokenRequestLink = StyledTextUtils.emulateLinkWidget(MSG_TOKEN, new StyledText(composite, SWT.WRAP));
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(tokenRequestLink);
		if (authDetails != null) {
			authDetails.getRequestTokenLink();
		}
		StyledTextUtils.emulateLinkAction(tokenRequestLink,
				r -> onRetrieveLinkClicked(tokenRequestLink.getShell(), dbc));
		tokenRequestLink.setCursor(new Cursor(tokenRequestLink.getShell().getDisplay(), SWT.CURSOR_HAND));

		//token
		Label authTypeLabel = new Label(composite, SWT.NONE);
		authTypeLabel.setText("Token");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(authTypeLabel);
		this.tokenText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(tokenText);
		ControlDecorationSupport.create(connectionValidator, SWT.LEFT | SWT.TOP, null, new ControlDecorationUpdater());

		this.rememberTokenCheckbox = new Button(composite, SWT.CHECK);
		rememberTokenCheckbox.setText("&Save token (could trigger secure storage login)");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(rememberTokenCheckbox);

		return composite;
	}

	@Override
	public void onVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
		dbc.addValidationStatusProvider(connectionValidator);
		bindWidgetsToInternalModel(dbc);
		this.rememberTokenBinding = ValueBindingBuilder
				.bind(WidgetProperties.buttonSelection().observe(rememberTokenCheckbox)).to(rememberTokenObservable).in(dbc);
	}

	@Override
	public void onInVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
		dispose();
		DataBindingUtils.dispose(rememberTokenBinding);
		dbc.removeValidationStatusProvider(connectionValidator);
	}

	@Override
	public void dispose() {
		DataBindingUtils.dispose(tokenBinding);
	}

	private void bindWidgetsToInternalModel(DataBindingContext dbc) {
		IValidator validator = new RequiredStringValidator("token");
		this.tokenBinding = ValueBindingBuilder.bind(WidgetProperties.text(SWT.Modify).observe(tokenText))
				.converting(new TrimmingStringConverter()).to(tokenObservable).validatingBeforeSet(validator).in(dbc);
		ControlDecorationSupport.create(tokenBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		org.jboss.tools.common.ui.databinding.DataBindingUtils.addDisposableValueChangeListener(changeListener,
				tokenObservable, tokenText);
	}

	@Override
	public void setSelectedConnection(IConnection selectedConnection) {
		if (selectedConnection instanceof Connection) {
			Connection connection = (Connection) selectedConnection;
			tokenObservable.setValue(connection.getToken());
			rememberTokenObservable.setValue(connection.isRememberToken());
		} else if (selectedConnection instanceof NewConnectionMarker) {
			tokenObservable.setValue(null);
			rememberTokenObservable.setValue(Boolean.FALSE);
		}
	}

	@Override
	public IConnectionAuthenticationProvider getConnectionAuthenticationProvider() {
		return new BearTokenAuthenticationProvider();
	}

	@Override
	public boolean isViewFor(Object object) {
		return object == this;
	}

	@Override
	public String toString() {
		return IAuthorizationContext.AUTHSCHEME_OAUTH;
	}

	private void onRetrieveLinkClicked(final Shell shell, final DataBindingContext dbc) {
		if (StringUtils.isBlank(pageModel.getHost())) {
			return;
		}
		try {
			WizardUtils.runInWizard(createAuthDetailsJob(shell), wizard.getContainer(), dbc);
		} catch (InvocationTargetException | InterruptedException ex) {
			showErrorDialog(shell, ex);
		}
	}

	protected Job createAuthDetailsJob(Shell shell) {
		Job job = new AuthDetailsJob(pageModel.getHost());
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getJob() instanceof AuthDetailsJob) {
					shell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							AuthDetailsJob job = (AuthDetailsJob) event.getJob();
							final IAuthorizationDetails details = job.getDetails();
							if (details != null) {
								//TODO fix this to handle other authschemes
								if (IAuthorizationContext.AUTHSCHEME_BASIC.equals(details.getScheme())) {
									MessageDialog.openError(shell, "Authorization Information", NLS.bind(
											"This server utilizes {0} authorization protocol", details.getScheme()));
									authSchemeObservable.setValue(details.getScheme());
								} else {
									OAuthDialog dialog = new OAuthDialog(shell, details.getRequestTokenLink());
									dialog.open();
									String token = dialog.getToken();
									if (StringUtils.isNotBlank(token)) {
										tokenObservable.setValue(token);
									}
								}
							}
						}
					});
				}
			}
		});
		return job;
	}

	private void showErrorDialog(final Shell shell, final Throwable e) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				ErrorDialog.openError(shell, "Request Authentication Details Error",
						"Unable to retrieve authentication details", ValidationStatus.error(e.getMessage(), e));

			}
		});
	}

	private class AuthDetailsJob extends Job {

		private IAuthorizationDetails details;
		private String host;

		public AuthDetailsJob(String host) {
			super(NLS.bind("Retrieve authorization details from {0}...", host));
			this.host = host;
		}

		public IAuthorizationDetails getDetails() {
			return details;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				IClient client = new ClientBuilder(host)
						.sslCertificateCallback(OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback())
						.build();
				details = client.getAuthorizationContext().getAuthorizationDetails();
				return ValidationStatus.OK_STATUS;
			} catch (Exception e) {
				if (e.getCause() instanceof ConnectTimeoutException) {
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							"Timed out waiting for a response for authorization details.\nThis server might be unavailable or may not support OAuth.",
							e);
				} else {
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							"Unable to retrieve the authentication details", e);
				}
			}
		}

	}

	private class BearTokenAuthenticationProvider implements IConnectionAuthenticationProvider {

		@Override
		public IConnection update(IConnection conn) {
			Assert.isLegal(conn instanceof Connection);

			final Connection connection = (Connection) conn;
			// might be called from job, switch to display thread to access observables
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					connection.setAuthScheme(IAuthorizationContext.AUTHSCHEME_OAUTH);
					connection.setToken(tokenObservable.getValue());
					connection.setRememberToken(rememberTokenObservable.getValue());
				}
			});
			return connection;
		}

	}

}
