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

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class CDK32LocationSection extends MinishiftLocationSection {
	private Text msProfileText;
	private ModifyListener msProfileListener;
	
	@Override
	protected void fillUI(FormToolkit toolkit, Composite composite) {
		super.fillUI(toolkit, composite);
		createMinishiftProfileWidgets(toolkit, composite);
	}
	
	@Override
	protected void setDefaultValues() {
		super.setDefaultValues();
		String profile = server.getAttribute(CDK32Server.PROFILE_ID, "");
		msProfileText.setText(profile);
	}
	

	protected void createMinishiftProfileWidgets(FormToolkit toolkit, Composite composite) {
		toolkit.createLabel(composite, "Minishift Profile:");
		msProfileText = toolkit.createText(composite, "", SWT.SINGLE | SWT.BORDER);
		msProfileText.setLayoutData(GridDataFactory.defaultsFor(msProfileText).span(4,1).minSize(150, SWT.DEFAULT).create());
	}
	
	@Override
	protected void addListeners() {
		super.addListeners();
		msProfileListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new SetMinishiftProfilePropertyCommand(server));
			}
		};
		msProfileText.addModifyListener(msProfileListener);
		msProfileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
	}
	

	public class SetMinishiftProfilePropertyCommand extends org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyTextCommand {
		public SetMinishiftProfilePropertyCommand(IServerWorkingCopy server) {
			super(server, "Change Minishift Profile", msProfileText, 
					msProfileText.getText(), CDK32Server.PROFILE_ID, msProfileListener);
		}
	}
	
	protected String isVersionCompatible(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if( cdkVers == null ) {
			return "Cannot determine CDK version.";
		}
		if( CDK32Server.matchesCDK32(cdkVers) ) {
			return null;
		}
		return "CDK version " + cdkVers + " is not compatible with this server adapter.";
	}

}
