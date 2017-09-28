/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.io.core.LoginResponse;
import org.jboss.tools.openshift.io.internal.ui.processor.DefaultRequestProcessor;
import org.jboss.tools.openshift.io.internal.ui.processor.RequestProcessor;

public class BrowserBasedLoginDialog extends Dialog {

	private String startURL;
	private Browser browser;
	private LoginResponse info;
	
	private final RequestProcessor processor;

	public BrowserBasedLoginDialog(Shell parentShell, String startURL, String landingURL) {
		super(parentShell);
		this.startURL = startURL;
		this.processor = new DefaultRequestProcessor(landingURL);
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
		Shell parent = getParentShell();
		return new Point(parent.getSize().x * 3 / 4, parent.getSize().y * 3 / 4);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);

		browser = new Browser(container, SWT.BORDER);
		browser.setText("Loading");
		Browser.clearSessions();
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
				System.out.println("URL=" + browser.getUrl());
				info = processor.getRequestInfo(browser, browser.getUrl(), browser.getText());
				if (null != info) {
					close();
				}
			}
		};
		browser.addProgressListener(progressListener);
		setURL(startURL);
		return container;
	}

	public void setURL(String url) {
		this.startURL = url;
		browser.setUrl(url);
	}

	/**
	 * @return the info
	 */
	public LoginResponse getInfo() {
		return info;
	}


}
