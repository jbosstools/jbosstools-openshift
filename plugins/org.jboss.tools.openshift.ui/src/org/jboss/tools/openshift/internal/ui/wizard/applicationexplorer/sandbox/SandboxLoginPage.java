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
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.core.odo.utils.KubernetesClusterHelper;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthBrowser;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthBrowser.TokenEvent;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthBrowser.TokenListener;

/**
 * @author Red Hat Developers
 *
 */
public class SandboxLoginPage extends AbstractOpenShiftWizardPage {

  private final SandboxModel model;
  
  private OAuthBrowser browser;
  
  private final WritableValue<IStatus> status = new WritableValue<>();  
  /**
   * @param title
   * @param description
   * @param pageName
   * @param wizard
   */
  public SandboxLoginPage(IWizard wizard, SandboxModel model) {
    super("Login to Red Hat Developer Sandbox", "Please login to Red Hat Developer Sandbox.", "SandboxLogin", wizard);
    this.model = model;
  }
  
  @Override
  protected void doCreateControls(Composite parent, DataBindingContext dbc) {
    
    GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(parent);
    
    browser = new OAuthBrowser(parent, SWT.NONE);
    browser.setLayoutData(new GridData( GridData.FILL_BOTH));
    browser.addTokenListener(new TokenListener() {
      @Override
      public void tokenReceived(TokenEvent event) {
        model.setClusterToken(event.getToken());
        status.setValue(ValidationStatus.ok());
      }
    });
    dbc.addValidationStatusProvider(new MultiValidator() {
      @Override
      protected IStatus validate() {
        return status.getValue();
      }
    });
}

  @Override
  protected void onPageActivated(DataBindingContext dbc) {
    try {
      if (model.getClusterToken() != null) {
        status.setValue(ValidationStatus.ok());
      } else {
        browser.setUrl(KubernetesClusterHelper.getTokenRequest(model.getClusterURL()));
        status.setValue(ValidationStatus.cancel("Please complete login"));
      }
    } catch (IOException e) {
      OpenShiftUIActivator.log(ERROR, e.getLocalizedMessage(), e);
    }
  }

}
