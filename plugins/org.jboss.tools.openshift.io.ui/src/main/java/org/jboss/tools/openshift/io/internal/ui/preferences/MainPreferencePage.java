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
package org.jboss.tools.openshift.io.internal.ui.preferences;

import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.openshift.io.core.AccountService;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;

import java.time.Instant;
import java.util.Date;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class MainPreferencePage
	extends org.eclipse.jface.preference.PreferencePage
	implements IWorkbenchPreferencePage {

	private boolean removed = false;
	
	private Label label;
	
	private Button button;
	
	public MainPreferencePage() {
		super();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		createWidgets(parent);
		return parent;
	}

	private void createWidgets(Composite control) {
		Composite parent = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.FILL, SWT.FILL).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);
		
		label = new Label(parent, SWT.WRAP);
		updateLabel();
		
		button = new Button(parent, SWT.PUSH);
		button.setText("Remove");
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::onButton));
		
	}

	void onButton(SelectionEvent event) {
		button.setEnabled(false);
		removed = true;
		updateLabel();
		
	}
	void updateLabel() {
		String msg;
		ICluster cluster = AccountService.getDefault().getModel().getClusters().get(0);
		if (removed || cluster.getAccounts().isEmpty()) {
			msg = "No configured accounts";
		} else {
			IAccount account = cluster.getAccounts().get(0);
			msg = account.getId() + " account configured valid until " + Date.from(Instant.ofEpochMilli(account.getAccessTokenExpiryTime()));
		}
		label.setText(msg);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (removed) {
			ICluster cluster = AccountService.getDefault().getModel().getClusters().get(0);
			if (!cluster.getAccounts().isEmpty()) {
				IAccount account = cluster.getAccounts().get(0);
				cluster.removeAccount(account);
				cluster.save();
			}
		}
		return true;
	}

	
	

}