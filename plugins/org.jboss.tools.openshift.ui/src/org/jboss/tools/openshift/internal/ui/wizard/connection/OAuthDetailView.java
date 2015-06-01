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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationDetails;

/**
 * @author jeff.cantrill
 */
public class OAuthDetailView extends BaseDetailsView implements IConnectionEditorDetailView {

	private static final String MSG_TOKEN = "Enter a token or <a>retrieve</a> a new one.";
	
	private IObservableValue tokenObservable;
	private Binding tokenBinding;
	private Text txtToken;
	private IValueChangeListener changeListener;
	private IObservableValue rememberTokenObservable;
	private IAuthorizationDetails authDetails;
	private Link tokenRequestLink;
	private ConnectionWizardPageModel pageModel;

	public OAuthDetailView(ConnectionWizardPageModel pageModel, IValueChangeListener changeListener, Object context, IObservableValue rememberTokenObservable) {
		this.pageModel = pageModel;
		this.rememberTokenObservable = rememberTokenObservable;
		this.changeListener = changeListener;
		if(context instanceof IAuthorizationDetails) {
			this.authDetails = (IAuthorizationDetails)context;
		}
	}
	
	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).spacing(10, 10).applyTo(composite);
		
		tokenRequestLink = new Link(composite, SWT.WRAP);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(tokenRequestLink);
		tokenRequestLink.setText(MSG_TOKEN);
		tokenRequestLink.addSelectionListener(new AuthLinkHandler(parent.getShell(), authDetails));
		
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
	public void setSelectedConnection(IObservableValue selectedConnectionObservable) {
		if (selectedConnectionObservable.getValue() instanceof Connection) {
			Connection connection = (Connection) selectedConnectionObservable.getValue();
			tokenObservable.setValue(connection.getToken());
			rememberTokenObservable.setValue(connection.isRememberPassword());
		} else if (selectedConnectionObservable.getValue() instanceof NewConnectionMarker) {
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
	
	private class AuthLinkHandler extends SelectionAdapter{
		private String link = "";
		private Shell shell;

		AuthLinkHandler(Shell shell, IAuthorizationDetails details){
			this.shell = shell;
			if(details != null) {
				details.getRequestTokenLink();
			}
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			BusyIndicator.showWhile(Display.getDefault(), new Runnable(){      
				public void run(){ 
					try {
						AuthDetailsJob job = null;
						if(StringUtils.isBlank(link)) {
							job = new AuthDetailsJob();
							job.setPriority(Job.SHORT);
							job.schedule();
						}
						OAuthDialog dialog = new OAuthDialog(shell, link);
						job.addJobChangeListener(dialog);
						dialog.open();
					}catch(Exception e) {
						showErrorDialog(shell,e);
					}
				}
			});
		}

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
		
		public AuthDetailsJob() {
			super("RetrieveAuthorizationDetailsJob");
		}

		public IAuthorizationDetails getDetails() {
			return details;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				IClient client = new ClientFactory().create(pageModel.getHost(), OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback());
				details = client.getAuthorizationDetails(pageModel.getHost());
			}catch(Exception e) {
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
					connection.setAuthType(IAuthorizationContext.AUTHSCHEME_OAUTH);
					connection.setToken((String)tokenObservable.getValue());
					connection.setRememberPassword((boolean)rememberTokenObservable.getValue());
				}
			});
			return connection;
		}
		
	}
	
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


//		@Override
//		protected Button createButton(Composite parent, int id, String label,
//				boolean defaultButton) {
//			return null;
//		}

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
			container.setLayout(new FillLayout());
			
			browser = new Browser(container, SWT.BORDER);
			browser.setText(loadingHtml);
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
				Display.getDefault().asyncExec(new Runnable() {
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
