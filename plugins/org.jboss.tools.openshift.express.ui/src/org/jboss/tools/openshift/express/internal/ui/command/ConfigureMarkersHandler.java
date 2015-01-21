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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.markers.ConfigureMarkersWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class ConfigureMarkersHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IProject.class);
		if (project != null) {
			new OkCancelButtonWizardDialog(HandlerUtil.getActiveShell(event), new ConfigureMarkersWizard(project))
					.open();
		}
		return Status.OK_STATUS;
	}

}
