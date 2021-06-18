/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.oauth.core.CommonOAuthCoreConstants;
import org.jboss.tools.common.oauth.core.TokenProvider;
import org.jboss.tools.common.oauth.core.exception.OAuthException;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.validator.CountryCodeValidator;
import org.jboss.tools.openshift.internal.ui.validator.PhoneNumberValidator;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox.SandboxProcessor.State;

/**
 * @author Red Hat Developers
 *
 */
public class SandboxWorkflowPage extends AbstractOpenShiftWizardPage { // implements PaintListener {

  private final SandboxModel model;
  
  private SandboxProcessor processor;
  
  private Label messageLabel;
  
  private Group verificationGroup;
  
  private Group confirmVerificationGroup;
  
  private final WritableValue<IStatus> status = new WritableValue<>();
  
  /**
   * @param title
   * @param description
   * @param pageName
   * @param wizard
   */
  public SandboxWorkflowPage(IWizard wizard, SandboxModel model) {
    super("Login to Red Hat Developer Sandbox", "Please login to Red Hat SSO if required, then provide required information to bootstrap your Red Hat Developer Sandbox.", "SandboxSSO", wizard);
    this.model = model;
  }
  
  private void reportMessage(String message, int type) {
    getControl().getDisplay().asyncExec(() -> messageLabel.setText(message));
  }
  
  private void ssoLogin(IProgressMonitor monitor) {
    try {
      reportMessage("Login to Red Hat SSO", NONE);
      model.setIDToken(TokenProvider.get().getToken(CommonOAuthCoreConstants.REDHAT_SSO_SERVER_ID, TokenProvider.ID_TOKEN, getControl()));
    } catch (OAuthException e) {
      reportMessage("Failed to login to Red Hat SSO", ERROR);
    }
  }
  
  private IStatus retrieveState(IProgressMonitor monitor) {
    if (model.getIDToken() == null) {
      ssoLogin(monitor);
    }
    if (model.getIDToken() != null) {
      checkSandbox(monitor);
    }
    return Status.OK_STATUS;
  }
  
  private void reportState(State state) {
    switch (state) {
    case NONE:
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.cancel("Checking Red Hat Developer Sandbox signup state")));
      break;
    case NEEDS_SIGNUP:
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.cancel("Checking Red Hat Developer Sandbox needs signup")));
      break;
    case NEEDS_APPROVAL:
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.cancel("Your Red Hat Developer Sandbox needs to be approved, you should wait or retry later")));
      break;
    case NEEDS_VERIFICATION:
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.cancel("Your Red Hat Developer Sandbox needs to be verified, enter your country code and phone number and click 'Verify'")));
      break;
    case CONFIRM_VERIFICATION:
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.cancel("You need to send the verification code received on your phone, enter the verification code and phone number and click 'Verify'")));
      break;
    case READY:
      reportMessage("Your Red Hat Developer Sandbox is ready, let's login now !!!", NONE);
      status.getRealm().asyncExec(() -> status.setValue(ValidationStatus.ok()));
      model.setClusterURL(processor.getClusterURL());
      break;
    }
  }

  private IStatus checkSandbox(IProgressMonitor monitor) {
    reportMessage("Checking Developer Sandbox account", NONE);
    if (processor == null) {
      processor = new SandboxProcessor(model.getIDToken());
    }
    boolean stop = false;
    try {
      while (!monitor.isCanceled() && !stop) {
        processor.advance(model.getCountryCode(), model.getPhoneNumber(), model.getVerificationCode());
        reportState(processor.getState());
        stop = processor.getState().isNeedsInteraction();
        if (!stop) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } catch (IOException e) {
      reportMessage("Error accessing the Red Hat Developer Sandbox API: " + e.getLocalizedMessage(), ERROR);
    }
    return Status.OK_STATUS;
  }
  
  private void launchJob() {
    try {
      Job job = Job.create("Retrieving Red Hat Developer Sandbox state", this::retrieveState);
      WizardUtils.runInWizard(job, getContainer());
      updateGroups();
    } catch (InvocationTargetException e) {
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  private void updateGroups() {
    ((GridData)verificationGroup.getLayoutData()).exclude = true;
    verificationGroup.setVisible(false);
    verificationGroup.setEnabled(false);
    ((GridData)confirmVerificationGroup.getLayoutData()).exclude = true;
    confirmVerificationGroup.setVisible(false);
    confirmVerificationGroup.setEnabled(false);
    if (processor != null && processor.getState() == State.NEEDS_VERIFICATION) {
      ((GridData)verificationGroup.getLayoutData()).exclude = false;
      verificationGroup.setVisible(true);
      verificationGroup.setEnabled(true);
    } else if (processor != null && processor.getState() == State.CONFIRM_VERIFICATION) {
      ((GridData)confirmVerificationGroup.getLayoutData()).exclude = false;
      confirmVerificationGroup.setVisible(true);
      confirmVerificationGroup.setEnabled(true);
    }
    confirmVerificationGroup.getParent().layout(true, true);
  }

  @Override
  protected void doCreateControls(Composite parent, DataBindingContext dbc) {
    
    GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(parent);
    
    messageLabel = new Label(parent, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(messageLabel);
    
    verificationGroup = new Group(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(verificationGroup);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(verificationGroup);
    
    Label countryCodeLabel = new Label(verificationGroup, SWT.NONE);
    countryCodeLabel.setText("Country code:");
    Text countryCodeText = new Text(verificationGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).span(2, 1).grab(true, false).applyTo(countryCodeText);
    ISWTObservableValue<String> countryCodeObservable = WidgetProperties.text(SWT.Modify).observe(countryCodeText);
    Binding countryCodeBinding = ValueBindingBuilder.bind(countryCodeObservable)
        .validatingAfterGet(new CountryCodeValidator() {
          @Override
          public IStatus validate(String countryCode) {
            if (verificationGroup.isVisible()) {
              return super.validate(countryCode);
            }
            return ValidationStatus.ok();
          }
        })
        .to(BeanProperties.value(SandboxModel.PROPERTY_COUNTRY_CODE).observe(model)).in(dbc);
    ControlDecorationSupport.create(countryCodeBinding, SWT.LEFT | SWT.TOP);

    Label phoneNumberLabel = new Label(verificationGroup, SWT.NONE);
    phoneNumberLabel.setText("Phone number:");
    Text phoneNumberText = new Text(verificationGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).span(2, 1).grab(true, false).applyTo(phoneNumberText);
    ISWTObservableValue<String> phoneNumberObservable = WidgetProperties.text(SWT.Modify).observe(phoneNumberText);
    Binding phoneNumberBinding = ValueBindingBuilder.bind(phoneNumberObservable)
        .validatingAfterGet(new PhoneNumberValidator() {
          @Override
          public IStatus validate(String phoneNumber) {
            if (verificationGroup.isVisible()) {
              return super.validate(phoneNumber);
            }
            return ValidationStatus.ok();
          }
        })
        .to(BeanProperties.value(SandboxModel.PROPERTY_PHONE_NUMBER).observe(model)).in(dbc);
    ControlDecorationSupport.create(phoneNumberBinding, SWT.LEFT | SWT.TOP);


    Button sendVerificationButton = new Button(verificationGroup, SWT.PUSH);
    sendVerificationButton.setText("Verify");
    sendVerificationButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> launchJob()));

    confirmVerificationGroup = new Group(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(confirmVerificationGroup);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(confirmVerificationGroup);
    
    Label verificationCodeLabel = new Label(confirmVerificationGroup, SWT.NONE);
    verificationCodeLabel.setText("Verification code:");
    Text verifictionCodeText = new Text(confirmVerificationGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).span(2, 1).grab(true, false).applyTo(verifictionCodeText);
    ISWTObservableValue<String> verificationCodeObservable = WidgetProperties.text(SWT.Modify).observe(verifictionCodeText);
    Binding verificationCodeBinding = ValueBindingBuilder.bind(verificationCodeObservable)
            .to(BeanProperties.value(SandboxModel.PROPERTY_VERIFICATION_CODE).observe(model)).in(dbc);
    ControlDecorationSupport.create(verificationCodeBinding, SWT.LEFT | SWT.TOP);
    
    Button confirmVerificationButton = new Button(confirmVerificationGroup, SWT.PUSH);
    confirmVerificationButton.setText("Verify");
    confirmVerificationButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> launchJob()));

    MultiValidator validator = new MultiValidator() {
      
      @Override
      protected IStatus validate() {
        return status.getValue();
      }
    };
    dbc.addValidationStatusProvider(validator);
    updateGroups();
}

  @Override
  protected void onPageActivated(DataBindingContext dbc) {
    Display.getCurrent().asyncExec(() -> launchJob());
  }
}
