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
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Red Hat Developers
 *
 */
public class OAuthBrowser extends Composite implements ProgressListener {
  public class TokenEvent extends TypedEvent {

    private String token;
    
    /**
     * @param object
     */
    public TokenEvent(OAuthBrowser browser, String token) {
      super(browser);
      this.token = token;
    }

    /**
     * @return the token
     */
    public String getToken() {
      return token;
    }
  }
  
  public interface TokenListener extends EventListener {
    void tokenReceived(TokenEvent event);
  }
  
  private Browser browser;
  
  private List<TokenListener> listeners = new ArrayList<>();

  /**
   * @param parent
   * @param style
   */
  public OAuthBrowser(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, false));
    setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    browser = new Browser(this, SWT.NONE);
    browser.setLayoutData(new GridData( GridData.FILL_BOTH));
    browser.addProgressListener(this);
  }
  
  public void setText(String text) {
    browser.setText(text);
  }
  
  public void setUrl(String url) {
    browser.setUrl(url);
  }
  
  public void addTokenListener(TokenListener listener) {
    listeners.add(listener);
  }
  
  public void removeTokenListener(TokenListener listener) {
    listeners.remove(listener);
  }
  
  public void addProgressListener(ProgressListener listener) {
    browser.addProgressListener(listener);
  }
  
  public void removeProgressListener(ProgressListener listener) {
    browser.removeProgressListener(listener);
  }
  
  protected void fireTokenReceived(String token) {
    TokenEvent event = new TokenEvent(this, token);
    listeners.forEach(l -> l.tokenReceived(event));
  }

  @Override
  public void changed(ProgressEvent event) {
  }

  @Override
  public void completed(ProgressEvent event) {
    TokenExtractor extractor = new TokenExtractor(browser.getText());
    if (extractor.isTokenPage()) {
      fireTokenReceived(extractor.getToken());
    }
  }
}
