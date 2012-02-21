/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class ExpressDetailsSection extends ServerEditorSection {
	private ExpressDetailsComposite details;
	private IEditorInput input;
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		this.input = input;
	}

	public void createSection(Composite parent) {
		super.createSection(parent);
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED|ExpandableComposite.TITLE_BAR);
		section.setText("Openshift Express Server");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL| GridData.GRAB_VERTICAL));

		details = ExpressDetailsComposite.createComposite(section, createCallback(), ExpressServerUtils.EXPRESS_SOURCE_MODE, false);
		toolkit.paintBordersFor(details.getComposite());
		toolkit.adapt(details.getComposite());
		section.setClient(details.getComposite());
		details.appNameCombo.setEnabled(false);
	}
	
	private IServerModeUICallback createCallback() {
		return DeploymentTypeUIUtil.getCallback(server, input, this);
	}

	/**
	 * Allow a section an opportunity to respond to a doSave request on the editor.
	 * @param monitor the progress monitor for the save operation.
	 */
	public void doSave(IProgressMonitor monitor) {
//		try {
//			ExpressServerUtils.setExpressPassword(server.getOriginal(), details.getPassword());
//			monitor.worked(100);
//		} catch( CoreException ce ) {
//			// TODO 
//		}
	}

}
