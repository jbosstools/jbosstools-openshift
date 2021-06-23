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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.common.util.SwtUtil;
import org.jboss.tools.openshift.core.odo.utils.KubernetesClusterHelper;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.OCCommandUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox.SandboxModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox.SandboxWizard;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthDialog;

import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * @author Red Hat Developers
 *
 */
public class LoginWizardPage extends AbstractOpenShiftWizardPage {

	private LoginModel model;
	
	private Text txtURL;
	private Text txtUsername;
	private Text txtPassword;
	private Text txtToken;

	/**
	 * @param wizard the parent wizard
	 * @param model the model
	 */
	protected LoginWizardPage(IWizard wizard, LoginModel model) {
		super("Sign in to OpenShift", "Please sign in to your OpenShift server.", "Server Connection", wizard);
		this.model = model;
	}
	
	private void onSandboxClicked(Composite parent) {
	  SandboxModel sandboxModel = new SandboxModel();
	  SandboxWizard wizard = new SandboxWizard(sandboxModel);
	  Point size = SwtUtil.getOptimumSizeFromTopLevelShell(getShell());
	  WizardDialog dialog = new WizardDialog(getShell(), wizard);
	  dialog.setMinimumPageSize(size);
	  if (dialog.open() == Window.OK) {
	    model.setUrl(sandboxModel.getClusterURL());
	    model.setToken(sandboxModel.getClusterToken());
	  }
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);

    StyledText expLabel = StyledTextUtils.emulateLinkWidget("Enter the cluster URL and the required credentials. You can also bootstrap a <a>Red Hat Developer Sandbox</a> cluster using your Red Hat account", new StyledText(parent, SWT.WRAP));
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(expLabel);
    StyledTextUtils.emulateLinkAction(expLabel, r -> onSandboxClicked(parent));

    Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setText("URL:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(urlLabel);
		txtURL = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1).applyTo(txtURL);
		
		Button pasteButton = new Button(parent, SWT.NONE);
		pasteButton.setText("Paste login command");
		pasteButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::onPasteLoginCommand));

		ISWTObservableValue<String> urlObservable = WidgetProperties.text(SWT.Modify).observe(txtURL);
		Binding urlBinding = ValueBindingBuilder.bind(urlObservable)
		        .validatingAfterConvert(new URLValidator("url", true)).converting(new TrimTrailingSlashConverter())
		        .to(BeanProperties.value(LoginModel.PROPERTY_URL).observe(model)).in(dbc);
		ControlDecorationSupport.create(urlBinding, SWT.LEFT | SWT.TOP);

		Label userLabel = new Label(parent, SWT.NONE);
		userLabel.setText("Username:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(userLabel);
		txtUsername = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtUsername);

		ISWTObservableValue<String> userObservable = WidgetProperties.text(SWT.Modify).observe(txtUsername);
		ValueBindingBuilder.bind(userObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_USERNAME).observe(model)).in(dbc);
		ControlDecoration usernameDecoration = new ControlDecoration(txtUsername, SWT.LEFT | SWT.TOP);

		Label passwordLabel = new Label(parent, SWT.NONE);
		passwordLabel.setText("Password:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(passwordLabel);
		txtPassword = new Text(parent, SWT.BORDER);
		txtPassword.setEchoChar('*');
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(txtPassword);

		ISWTObservableValue<String> passwordObservable = WidgetProperties.text(SWT.Modify).observe(txtPassword);
		ValueBindingBuilder.bind(passwordObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_PASSWORD).observe(model)).in(dbc);
		ControlDecoration passwordDecoration = new ControlDecoration(txtPassword, SWT.LEFT | SWT.TOP);


		Label tokenLabel = new Label(parent, SWT.NONE);
		tokenLabel.setText("Token:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(tokenLabel);
		txtToken = new Text(parent, SWT.BORDER);
		txtToken.setEchoChar('*');
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtToken);

		ISWTObservableValue<String> tokenObservable = WidgetProperties.text(SWT.Modify).observe(txtToken);
		ValueBindingBuilder.bind(tokenObservable)
		        .to(BeanProperties.value(LoginModel.PROPERTY_TOKEN).observe(model)).in(dbc);
		ControlDecoration tokenDecoration = new ControlDecoration(txtToken, SWT.LEFT | SWT.TOP);

		Button retrieveTokenButton = new Button(parent, SWT.NONE);
		retrieveTokenButton.setText("Retrieve token");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(retrieveTokenButton);
		retrieveTokenButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::onRetrieveToken));

		dbc.addValidationStatusProvider(new LoginFormValidator(
				userObservable, usernameDecoration,
				passwordObservable, passwordDecoration,
				tokenObservable, tokenDecoration));
	}
	
  private void onPasteLoginCommand(SelectionEvent e) {
    Clipboard clipboard = new Clipboard(Display.getDefault());
    String clipboardText = (String) clipboard.getContents(TextTransfer.getInstance());
    if (clipboardText == null) {
      MessageDialog.openError(getWizard().getContainer().getShell(), "Error when parsing login command",
          "Cannot paste clipboard into the login dialog. Only text is accepted.");
    } else if (OCCommandUtils.isValidCommand(clipboardText)) {
      txtURL.setText(OCCommandUtils.getServer(clipboardText));
      if (IAuthorizationContext.AUTHSCHEME_BASIC.equals(OCCommandUtils.getAuthMethod(clipboardText))) {
        txtUsername.setText(OCCommandUtils.getUsername(clipboardText));
        txtPassword.setText(OCCommandUtils.getPassword(clipboardText));
      } else if (IAuthorizationContext.AUTHSCHEME_OAUTH.equals(OCCommandUtils.getAuthMethod(clipboardText))) {
        txtToken.setText(OCCommandUtils.getToken(clipboardText));
      }
    } else {
      MessageDialog.openError(getWizard().getContainer().getShell(), "Error when parsing login command",
          "Login command pasted from clipboard is not valid:\n" + clipboardText);
    }
  }
	
	private void onRetrieveToken(SelectionEvent event) {
	  String[] tokenEndpoint = new String[1];
	  String url = txtURL.getText();
	  Job job = Job.create("Retrieving token endpoint from cluster", monitor -> {
	    try {
	      tokenEndpoint[0] = KubernetesClusterHelper.getTokenRequest(url);
	      return Status.OK_STATUS;
	    } catch (IOException e) {
	      return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
	    }
	  });
	  try {
      WizardUtils.runInWizard(job,getContainer(), getDataBindingContext());
      if (JobUtils.isOk(job.getResult()) && tokenEndpoint[0] != null) {
        OAuthDialog dialog = new OAuthDialog(getShell(), tokenEndpoint[0], true);
        if (dialog.open() == Window.OK && dialog.getToken() != null) {
          txtToken.setText(dialog.getToken());
        }
      } else {
        MessageDialog.openError(getWizard().getContainer().getShell(), "Error when launching web based authentication",
            "Can't retrieve the token endpoint from cluster to start from");
      }
    } catch (InvocationTargetException | InterruptedException e) {
      MessageDialog.openError(getWizard().getContainer().getShell(), "Error when launching web browser",
          "Can't create the web browser to login");
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
	}
	
	private static class LoginFormValidator extends MultiValidator {
		
		private static final IStatus requiredStatus = ValidationStatus.cancel("Token or Username and Password are required");

		private IObservableValue<String> userObservable;
		private ControlDecoration userDecoration;

		private IObservableValue<String> passwordObservable;
		private ControlDecoration passwordDecoration;

		private IObservableValue<String> tokenObservable;
		private ControlDecoration tokenDecoration;

		private UpdateableDecorationUpdater decorationUpdater = new UpdateableDecorationUpdater();

		private LoginFormValidator(IObservableValue<String> userObservable, ControlDecoration userDecoration,
				IObservableValue<String> passwordObservable, ControlDecoration passwordDecoration,
				IObservableValue<String> tokenObservable, ControlDecoration tokenDecoration) {
			this.userObservable = userObservable;
			this.userDecoration = userDecoration;
			this.passwordObservable = passwordObservable;
			this.passwordDecoration = passwordDecoration;
			this.tokenObservable = tokenObservable;
			this.tokenDecoration = tokenDecoration;
		}

		@Override
		protected IStatus validate() {
			String user = userObservable.getValue();
			String password = passwordObservable.getValue();
			String token = tokenObservable.getValue();

			if (token.isEmpty() && user.isEmpty() && password.isEmpty()) {
				updateDecorations(requiredStatus, requiredStatus, requiredStatus);
				return requiredStatus;
			}
			else if (!token.isEmpty() && (!user.isEmpty() || !password.isEmpty())) {
				IStatus error = ValidationStatus.error("Can't use token authentication with user or password");
				updateDecorations(ValidationStatus.ok(), error, error);
				return error;
			}
			else if (!user.isEmpty() && (password.isEmpty() || !token.isEmpty())) {
				IStatus error = ValidationStatus.error("Can't use user authentication without a password or with a token");
				updateDecorations(error, ValidationStatus.ok(), error);
				return error;
			} else {
				updateDecorations(ValidationStatus.ok(), ValidationStatus.ok(), ValidationStatus.ok());
				return ValidationStatus.ok();
			}
		}

		private void updateDecorations(IStatus tokenStatus, IStatus userStatus, IStatus passwordStatus) {
			decorationUpdater.update(tokenDecoration, tokenStatus);
			decorationUpdater.update(userDecoration, userStatus);
			decorationUpdater.update(passwordDecoration, passwordStatus);
		}

		static class UpdateableDecorationUpdater extends RequiredControlDecorationUpdater {

			@Override
			protected void update(ControlDecoration decoration, IStatus status) {
				super.update(decoration, status);
			}

		}
	}
}