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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.util.SwtUtil;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthBrowser.TokenEvent;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthBrowser.TokenListener;

/*
 * Leaving for now as we may need this if we are ever able
 * to progammatically get the token
 */
public class OAuthDialog extends Dialog {

	private String loadingHtml;
	private String url;
	private OAuthBrowser browser;
	private String token;
	private final boolean autoClose;

	public OAuthDialog(Shell parentShell, String url, boolean autoClose) {
		super(parentShell);
		this.url = url;
		this.autoClose = autoClose;
		try {
			loadingHtml = IOUtils.toString(OpenShiftUIActivator.getDefault().getPluginFile("html/spinner.html"));
		} catch (Exception e) {
			loadingHtml = "Loading...";
		}
	}
	
	public OAuthDialog(Shell parentShell, String url) {
	  this(parentShell, url, false);
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
	  return SwtUtil.getOptimumSizeFromTopLevelShell(getShell());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);

		browser = new OAuthBrowser(container, SWT.BORDER);
		browser.setText(loadingHtml);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(browser);

		final ProgressBar progressBar = new ProgressBar(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(progressBar);

		ProgressListener progressListener = new ProgressListener() {
			@Override
			public void changed(ProgressEvent event) {
				if (event.total <= 0)
					return;
				int ratio = event.current * 100 / event.total;
				progressBar.setSelection(ratio);
			}

			@Override
			public void completed(ProgressEvent event) {
				progressBar.setSelection(0);
			}
		};
		
		TokenListener tokenListener = new TokenListener() {
      @Override
      public void tokenReceived(TokenEvent event) {
        token = event.getToken();
        if (autoClose) {
          OAuthDialog.this.close();
        }
      }
		};
		browser.addProgressListener(progressListener);
		browser.addTokenListener(tokenListener);
		setURL(url);
		return container;
	}

	public void setURL(String url) {
		if (StringUtils.isNotBlank(url)) {
			this.url = url;
			browser.setUrl(url);
		}
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
}