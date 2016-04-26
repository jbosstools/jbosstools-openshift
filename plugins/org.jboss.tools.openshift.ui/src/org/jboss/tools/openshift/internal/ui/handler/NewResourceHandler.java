/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.ConnectionNotFoundException;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.resource.NewResourceWizard;

/**
 * Handler to trigger the New Openshift resource workflow
 * 
 * @author Jeff Maury
 */
public class NewResourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
            WizardUtils.openWizardDialog(new NewResourceWizard(), HandlerUtil.getActiveShell(event));
		} catch (ConnectionNotFoundException e) {
			return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to find the connection", e);
		}

		return Status.OK_STATUS;
	}

}
