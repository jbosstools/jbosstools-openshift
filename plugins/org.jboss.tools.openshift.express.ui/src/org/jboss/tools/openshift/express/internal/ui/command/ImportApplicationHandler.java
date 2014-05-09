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
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ImportOpenShiftApplicationWizard;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class ImportApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application == null) {
			return OpenShiftUIActivator.createCancelStatus("Could not find the application to import");
		}
		WizardUtils.openWizard(
				new ImportOpenShiftApplicationWizard(application, false),
				HandlerUtil.getActiveShell(event));
		return Status.OK_STATUS;
	}
}
