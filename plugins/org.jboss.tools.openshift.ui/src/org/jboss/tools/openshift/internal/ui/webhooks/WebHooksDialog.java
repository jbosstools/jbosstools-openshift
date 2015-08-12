/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.webhooks;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Fred Bricon
 */
public class WebHooksDialog extends Dialog {

	private IBuildConfig buildConfig;

	public WebHooksDialog(Shell parent, IBuildConfig buildConfig) {
		super(parent);
		this.buildConfig = buildConfig;
		setShellStyle(getShellStyle() | SWT.RESIZE); 
	}

	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("Copy Web Hooks urls");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		WebHooksComponent webHookComponent = new WebHooksComponent(buildConfig, container, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).hint(300, SWT.DEFAULT)
			.applyTo(webHookComponent);
		
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

}
