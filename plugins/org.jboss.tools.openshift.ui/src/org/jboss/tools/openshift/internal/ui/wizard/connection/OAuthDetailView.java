/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
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

import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationDetails;

/**
 * @author jeff.cantrill
 */
public class OAuthDetailView extends BaseDetailsView implements IConnectionEditorDetailView {

	private static final String MSG1_TOKEN = "Enter a token or";
	private static final String LINK_TOKEN = "<a>retrieve</a>";
	private static final String MSG2_TOKEN = "a new one.";
	
	private IObservableValue tokenObservable;
	private Binding tokenBinding;
	private Text txtToken;
	private IValueChangeListener changeListener;
	private IObservableValue rememberTokenObservable;
	private IObservableValue authSchemeObservable;
	private IAuthorizationDetails authDetails;
	private ConnectionWizardPageModel pageModel;
	private Button chkRememberToken;
	
	final Cursor cursor = new Cursor(null, SWT.CURSOR_HAND);

	private IWizard wizard;



	public OAuthDetailView(IWizard wizard, ConnectionWizardPageModel pageModel, IValueChangeListener changeListener, Object context, IObservableValue rememberTokenObservable, Button chkRememberToken, IObservableValue authSchemeObservable) {
		this.wizard = wizard;
		this.pageModel = pageModel;
		this.rememberTokenObservable = rememberTokenObservable;
		this.authSchemeObservable = authSchemeObservable;
		this.changeListener = changeListener;
		this.chkRememberToken = chkRememberToken;
		if(context instanceof IAuthorizationDetails) {
			this.authDetails = (IAuthorizationDetails)context;
		}
	}
	
	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).spacing(10, 10).applyTo(composite);
		
		Composite textContainer = new Composite(composite, SWT.None);
		
		GridLayoutFactory.fillDefaults()
		.numColumns(3).spacing(1, 1).applyTo(textContainer);
		
		StyledText token1 = new StyledText(textContainer, SWT.WRAP);
		
		StyledTextUtils.setTransparent(token1);
		StyledTextUtils.setLinkText(MSG1_TOKEN, token1);
		token1.setEditable(false);
		
		StyledText tokenRequestLink = new StyledText(textContainer, SWT.WRAP);
		StyledTextUtils.setTransparent(tokenRequestLink);
		StyledTextUtils.setLinkText(LINK_TOKEN, tokenRequestLink);
		tokenRequestLink.setEditable(false);
		tokenRequestLink.setCursor(cursor);
		
		StyledText token2 = new StyledText(textContainer, SWT.WRAP);
		StyledTextUtils.setTransparent(token2);
		StyledTextUtils.setLinkText(MSG2_TOKEN, token2);
		token2.setEditable(false);
		
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(textContainer);
		if(authDetails != null) {
			authDetails.getRequestTokenLink();
		}
		tokenRequestLink.addListener(SWT.MouseDown, onRetrieveLinkClicked(parent.getShell()));
		
		//token
		Label lblAuthType = new Label(composite, SWT.NONE);
		lblAuthType.setText("Token");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(lblAuthType);
		txtToken = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtToken);
		tokenObservable = new WritableValue(null, String.class);
		
		return composite;
	}

	
	@Override
	public void onVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
		bindWidgetsToInternalModel(dbc);
		chkRememberToken.setText("&Save Token (could trigger secure storage login)");
	}
	
	@Override
	public void onInVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
		dispose();
	}
	
	@Override
	public void dispose() {
		DataBindingUtils.dispose(tokenBinding);
	}

	private void bindWidgetsToInternalModel(DataBindingContext dbc) {
		this.tokenBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(txtToken))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("token"))
				.to(tokenObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				tokenBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		tokenObservable.addValueChangeListener(changeListener);
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
	
	private Listener onRetrieveLinkClicked(final Shell shell) {
		return new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (StringUtils.isBlank(pageModel.getHost())) {
					return;
				}
				
				Job job = new AuthDetailsJob(pageModel.getHost());
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						if(event.getJob() instanceof AuthDetailsJob) {
							shell.getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									AuthDetailsJob job = (AuthDetailsJob)event.getJob();
									final IAuthorizationDetails details = job.getDetails();
									if(details != null) {
										//TODO fix this to handle other authschemes
										if(IAuthorizationContext.AUTHSCHEME_BASIC.equals(details.getScheme())) {
											MessageDialog.openError(shell, "Authorization Information", NLS.bind("This server utilizes {0} authorization protocol", details.getScheme()));
											authSchemeObservable.setValue(details.getScheme());
										}else {
										    OAuthDialog dialog = new OAuthDialog(shell, details.getRequestTokenLink());
										    job.addJobChangeListener(dialog);
										    dialog.open();
										}
									}
								}
							});
						}
					}
				});
				try {
					WizardUtils.runInWizard(job, wizard.getContainer());
				} catch (InvocationTargetException | InterruptedException ex) {
					showErrorDialog(shell,ex);
				}				
			}
			
		};
	}
	
	private void showErrorDialog(final Shell shell, final Throwable e) {
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				ErrorDialog.openError(shell, 
						"Request Authentication Details Error", 
						"Unable to retrieve authentication details", ValidationStatus.error(e.getMessage(),e));
				
			}
		});
	}
	
	private class AuthDetailsJob extends Job{
		
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
				IClient client = new ClientFactory().create(host, OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback());
				details = client.getAuthorizationDetails(host);
			}catch(Exception e) {
				if (e.getCause() instanceof ConnectTimeoutException) {
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Timed out waiting for a response for authorization details.\nThis server might be unavailable or may not support OAuth.", e);
				}
				return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to retrieve the authentication details", e);
			}
			return ValidationStatus.OK_STATUS;
		}
		
	}
	
	private class BearTokenAuthenticationProvider implements IConnectionAuthenticationProvider{

		@Override
		public IConnection update(IConnection conn) {
			Assert.isLegal(conn instanceof Connection);
			final Connection connection = (Connection) conn;
			// might be called from job, switch to display thread to access observables
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					connection.setAuthScheme(IAuthorizationContext.AUTHSCHEME_OAUTH);
					connection.setToken((String)tokenObservable.getValue());
					connection.setRememberToken((boolean)rememberTokenObservable.getValue());
				}
			});
			return connection;
		}
		
	}
	
	/*
	 * Leaving for now as we may need this if we are ever able
	 * to progammatically get the token
	 */
	private class OAuthDialog extends Dialog implements IJobChangeListener{
		
		private String loadingHtml;
		private String url;
		private Browser browser;
		
		OAuthDialog(Shell parentShell, String url) {
			super(parentShell);
			this.url = url;
			try {
				loadingHtml = IOUtils.toString(OpenShiftUIActivator.getDefault().getPluginFile("html/spinner.html"));
			} catch (Exception e) {
				loadingHtml = "Loading...";
			}
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, "Close", true);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}


		@Override
		protected Point getInitialSize() {
			return new Point(500, 700);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			container.setLayout(new GridLayout());
			
			GridData data = new GridData();
			data.horizontalAlignment = GridData.FILL;
			data.verticalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			container.setLayoutData(data);
			
			browser = new Browser(container, SWT.BORDER);
			browser.setText(loadingHtml);
			browser.setLayoutData(data);
			
			final ProgressBar progressBar = new ProgressBar(container, SWT.NONE);
			data = new GridData();
			data.horizontalAlignment = GridData.FILL;
			progressBar.setLayoutData(data);
			
			ProgressListener progressListener = new ProgressListener() {
				public void changed(ProgressEvent event) {
					if (event.total <= 0) return;                            
					int ratio = event.current * 100 / event.total;
					progressBar.setSelection(ratio);;
				}

				public void completed(ProgressEvent event) {
					progressBar.setSelection(0);
				}
			};
			browser.addProgressListener(progressListener);
			
			setURL(url);
			return container;
		}
		
		public void setURL(String url) {
			if(StringUtils.isNotBlank(url)) {
				this.url = url;
				browser.setUrl(url);
			}
		}


		@Override
		public void aboutToRun(IJobChangeEvent event) {
		}


		@Override
		public void awake(IJobChangeEvent event) {
		}


		@Override
		public void done(final IJobChangeEvent event) {
			if(event.getJob() instanceof AuthDetailsJob) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						AuthDetailsJob job = (AuthDetailsJob)event.getJob();
						final IAuthorizationDetails details = job.getDetails();
						if(details != null) {
							setURL(details.getRequestTokenLink());
						}else {
							IStatus result = job.getResult();
							showErrorDialog(getShell(),result.getException());
							close();
						}
					}
				});
			}
		}


		@Override
		public void running(IJobChangeEvent event) {
		}


		@Override
		public void scheduled(IJobChangeEvent event) {
		}


		@Override
		public void sleeping(IJobChangeEvent event) {
			
		}

	}
	
}
