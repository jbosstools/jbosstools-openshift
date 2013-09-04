/******************************************************************************* 
* Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentPage;
import org.jboss.ide.eclipse.as.ui.editor.ModuleDeploymentOptionsComposite;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;

public class ExpressModuleDeploymentPage extends DeploymentPage implements IEditorPart {

	protected void addDeploymentLocationControls(Composite parent, Control top) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Label l1 = toolkit.createLabel(parent, OpenshiftUIMessages.EditorDeploymentPageDescription); 
		FormData fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.top = top == null ? new FormAttachment(0, 5) : new FormAttachment(top, 5); 
		fd.right = new FormAttachment(100, -5);
		l1.setLayoutData(fd);
		
		// Simply create a composite to show the per-module customizations
		perModuleOptions = new ModuleDeploymentOptionsComposite(parent, this, getFormToolkit(parent), preferences);
		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.top = new FormAttachment(l1, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -5);
		perModuleOptions.setLayoutData(fd);
	}
	
	protected void setDeploymentTabEnablement() {
		// Do nothing
	}
	public void doSave(IProgressMonitor monitor) {
		// Do nothing
	}
	
	
}
