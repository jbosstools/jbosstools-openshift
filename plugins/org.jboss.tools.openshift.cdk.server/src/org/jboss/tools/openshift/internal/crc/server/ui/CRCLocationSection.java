/******************************************************************************* 
 * Copyright (c) 2019-2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.crc.server.ui;

import java.io.File;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.internal.cdk.server.ui.AbstractLocationSection;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;

public class CRCLocationSection extends AbstractLocationSection {

	private static final String SECTION_TITLE = "CRC Details";
	private static final String LABEL_STRING = "CRC Binary: ";
	private static final String COMMAND_NAME = "Modify crc binary Location";
	private static final String LOC_ATTR = CRC100Server.PROPERTY_BINARY_FILE;
	private Text pullSecretText;
	private Button pullSecretBrowse;
	private ControlDecoration pullSecretDecor;
	private ModifyListener pullSecretModListener;
	private final PullSecretValidation pullSecretValidation = new PullSecretValidation();

	public CRCLocationSection() {
		super(SECTION_TITLE, LABEL_STRING, COMMAND_NAME, LOC_ATTR);
	}
	
	@Override
	protected void fillUI(FormToolkit toolkit, Composite composite) {
		createLocationWidgets(toolkit, composite);
		createPullSecretWidget(toolkit, composite);
	}

	private void createPullSecretWidget(FormToolkit toolkit, Composite composite) {
		toolkit.createLabel(composite, "Pull Secret File:");
		pullSecretText = toolkit.createText(composite, "", SWT.SINGLE | SWT.BORDER);
		pullSecretBrowse = toolkit.createButton(composite, "Browse...", SWT.PUSH);

		pullSecretText.setLayoutData(GridDataFactory.defaultsFor(pullSecretText).span(3, 1).minSize(150, SWT.DEFAULT).create());

		pullSecretDecor = new ControlDecoration(pullSecretText, SWT.TOP | SWT.LEFT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		Image img = fieldDecoration.getImage();
		pullSecretDecor.setImage(img);
		pullSecretDecor.hide(); // hiding it initially
	}

	@Override
	protected void locationBrowseClicked() {
		browseClicked(getLocationText(), FILE);
	}
	
	@Override
	protected void setDefaultValues() {
		// set initial values
		super.setDefaultValues();
		String psHome = server.getAttribute(CRC100Server.PROPERTY_PULL_SECRET_FILE, (String)null);
		if( psHome != null )
			pullSecretText.setText(psHome);
	}

	@Override
	protected void addListeners() {
		super.addListeners();
		pullSecretBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseClicked(pullSecretText, FILE);
				validate();
			}
		});

		pullSecretModListener = (ModifyEvent e) -> 
			execute(new SetPullSecretPropertyCommand(server));
		pullSecretText.addModifyListener(pullSecretModListener);
		pullSecretText.addModifyListener((ModifyEvent e) -> 
			validate());

	}

	@Override
	protected void validate() {
		validateBinary();
		validatePullSecret();
	}
	
	protected void validateBinary() {
		String s = getBinaryErrorString();
		if (s == null) {
			txtDecorator.hide();
		} else {
			txtDecorator.setDescriptionText(s);
			txtDecorator.show();
		}
	}

	protected String getBinaryErrorString() {
		String txt = getLocationText().getText();
		if( txt == null || txt.isEmpty()) {
			return "Please select a valid crc binary.";
		}
		File f = new File(txt);
		if( !f.isFile()) {
			return "Please select a valid crc binary.";
		}
		if( !f.canExecute()) {
			return "crc binary is not executable.";
		}
		return null;
	}

	protected void validatePullSecret() {
		pullSecretDecor.hide();
		String errorMessage = pullSecretValidation.validate(pullSecretText.getText(), 
				(String error) -> validate());
		if (errorMessage != null) {
			pullSecretDecor.setDescriptionText(errorMessage);
			pullSecretDecor.show();
		}
	}

	public class SetPullSecretPropertyCommand
			extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyTextCommand {
		public SetPullSecretPropertyCommand(IServerWorkingCopy server) {
			super(server, "Change Pull Secret File", pullSecretText, pullSecretText.getText(), 
					CRC100Server.PROPERTY_PULL_SECRET_FILE,
					pullSecretModListener);
		}
	}

}
