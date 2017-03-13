/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;
import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;

public class MinishiftLocationSection extends AbstractLocationSection {

	private static String SECTION_TITLE = "CDK Details";
	private static String LABEL_STRING = "Minishift Location: ";
	private static String COMMAND_NAME = "Modify Minishift Location";
	private static String LOC_ATTR = CDK3Server.MINISHIFT_FILE;
	
	private Combo hypervisorCombo;
	
	public MinishiftLocationSection() {
		super(SECTION_TITLE, LABEL_STRING, COMMAND_NAME, LOC_ATTR);
	}

	@Override
	protected void fillUI(FormToolkit toolkit, Composite composite) {
		createLocationWidgets(toolkit, composite);
		createHypervisorWidgets(toolkit, composite);
	}

	protected void createHypervisorWidgets(FormToolkit toolkit, Composite composite) {
		Label l = toolkit.createLabel(composite, "Hypervisor:");
		hypervisorCombo = new Combo(composite,  SWT.READ_ONLY);
		hypervisorCombo.setLayoutData(GridDataFactory.defaultsFor(hypervisorCombo).span(4, 1).create());
		hypervisorCombo.setItems(CDK3Server.getHypervisors());
	}
	
	@Override
	protected void setDefaultValues() {
		// set initial values
		super.setDefaultValues();
		String hyp = server.getAttribute(CDK3Server.PROP_HYPERVISOR, CDK3Server.getHypervisors()[0]);
		int ind = Arrays.asList(CDK3Server.getHypervisors()).indexOf(hyp);
		if( ind != -1 ) {
			hypervisorCombo.select(ind);
		}
	}
	
	@Override
	protected void addListeners() {
		super.addListeners();
		SelectionListener sl = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new SetHypervisorPropertyCommand(server, this));
			}
		};
		hypervisorCombo.addSelectionListener(sl);
	}
	
	public class SetHypervisorPropertyCommand extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyComboCommand {
		public SetHypervisorPropertyCommand(IServerWorkingCopy server, SelectionListener sl) {
			super(server, "Change hypervisor", hypervisorCombo, hypervisorCombo.getItem(hypervisorCombo.getSelectionIndex()), CDK3Server.PROP_HYPERVISOR, sl);
		}
	}
	
	@Override
	protected File getFile(File selected, Shell shell) {
		return chooseFile(selected, shell);
	}
	
}
