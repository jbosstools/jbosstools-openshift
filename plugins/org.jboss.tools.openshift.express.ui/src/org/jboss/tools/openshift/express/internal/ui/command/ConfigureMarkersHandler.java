/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.markers.ConfigureMarkersWizard;

/**
 * @author Andre Dietisheim
 */
public class ConfigureMarkersHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IProject.class);
		if (project != null) {
			new WizardDialog(HandlerUtil.getActiveShell(event), new ConfigureMarkersWizard(project)) {
				
				@Override
				protected Control createButtonBar(Composite parent) {
					Control control = super.createButtonBar(parent);
					getButton(IDialogConstants.FINISH_ID).setText(IDialogConstants.OK_LABEL);
					return control;
				}

			}.open();
		}
		return Status.OK_STATUS;
	}

}
