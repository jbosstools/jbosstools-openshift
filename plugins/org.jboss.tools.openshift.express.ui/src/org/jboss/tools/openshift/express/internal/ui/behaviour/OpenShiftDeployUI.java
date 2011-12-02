/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class OpenShiftDeployUI implements IDeploymentTypeUI {

	public OpenShiftDeployUI() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void fillComposite(Composite parent, IServerModeUICallback callback) {
		createWidgets(parent, callback);
		addListeners(callback);
	}
	
	private void addListeners(IServerModeUICallback callback) {
		
	}
	
	//private Text userText, passText;
	private Text userText, passText;
	private Combo modeCombo;
	
	private String safeString(String s) {
		return s == null ? "" : s;
	}
	private void createWidgets(Composite parent, IServerModeUICallback callback) {
		// TODO Auto-generated method stub
		parent.setLayout(new FillLayout());
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FormLayout());
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("OpenShift Username:");
		userText = new Text(composite, SWT.BORDER);
		Label passLabel = new Label(composite, SWT.NONE);
		passLabel.setText("OpenShift Password:");
		passText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		
		Label domainLabel = new Label(composite, SWT.NONE);
		domainLabel.setText("Domain: " + ExpressServerUtils.getExpressDomain(callback.getServer()));
		Label appLabel = new Label(composite, SWT.NONE);
		appLabel.setText("App: " + ExpressServerUtils.getExpressApplicationName(callback.getServer()));
		
		Label modeLabel = new Label(composite, SWT.NONE);
		modeLabel.setText("Mode: " + ExpressServerUtils.getExpressModeAsString(callback.getServer()));
		
		userText.setText(safeString(ExpressServerUtils.getExpressUsername(callback.getServer())));
		passText.setText(safeString(ExpressServerUtils.getExpressPassword(callback.getServer())));
		userText.setEnabled(false);
		passText.setEnabled(false);
		// Maybe just make this a label ??
//		modeCombo = new Combo(composite, SWT.READ_ONLY);
//		modeCombo.setItems(new String[]{
//				"Source", "Binary"
//		});
//		modeCombo.select(0);
		
		nameLabel.setLayoutData(UIUtil.createFormData2(0, 5,null,0,0,5,null,0));
		userText.setLayoutData(UIUtil.createFormData2(0,3,null,0,nameLabel,5,100,-5));
		
		passLabel.setLayoutData(UIUtil.createFormData2(userText, 5,null,0,0,5,null,0));
		passText.setLayoutData(UIUtil.createFormData2(userText,5,null,0,nameLabel,5,100,-5));
		
		domainLabel.setLayoutData(UIUtil.createFormData2(passText,5,null,0,0,5,null,0));
		appLabel.setLayoutData(UIUtil.createFormData2(domainLabel,5,null,0,0,5,null,0));
		modeLabel.setLayoutData(UIUtil.createFormData2(appLabel,5,null,0,0,5,null,0));
//		modeCombo.setLayoutData(UIUtil.createFormData2(appLabel,3,null,0,modeLabel,5,100,-5));
	}

}
