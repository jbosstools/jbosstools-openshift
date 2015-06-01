/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionNotFoundException;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.application.NewApplicationWizard;
import org.jboss.tools.openshift.internal.ui.wizard.application.NewApplicationWizardModel;

import com.openshift.restclient.model.IProject;

/**
 * Handler to trigger the New Application workflow
 * 
 * @author jeff.cantrill
 */
public class NewApplicationHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IProject project = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IProject.class);
			if(project == null) {
				return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to find the OpenShift project to use when creating the new application.");
			}
			Connection connection = ConnectionsRegistryUtil.getConnectionFor(project);
			NewApplicationWizardModel model = new NewApplicationWizardModel(project, connection.getResourceFactory());
			NewApplicationWizard wizard = new NewApplicationWizard(model);
			WizardUtils.openWizardDialog(wizard, HandlerUtil.getActiveShell(event));
		}catch(ConnectionNotFoundException e) {
			return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to find the connection", e);
		}

		return Status.OK_STATUS;
	}

}
