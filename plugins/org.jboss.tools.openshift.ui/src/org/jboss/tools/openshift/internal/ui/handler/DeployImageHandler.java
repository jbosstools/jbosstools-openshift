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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizard;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizardModel;

import com.openshift.restclient.model.IProject;


/**
 * @author jeff.cantrill
 */
public class DeployImageHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DeployImageWizardModel model = new DeployImageWizardModel();
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		final IDockerImage image = UIUtils.getFirstElement(selection, IDockerImage.class);
		if(image != null) {
			IDockerConnection dockerConnection = image.getConnection();
			model.setOriginatedFromDockerExplorer(true);
			model.setDockerConnection(dockerConnection);
			model.setImage(image.repo());
		}else{
			final IProject project = UIUtils.getFirstElement(selection, IProject.class);
			if(project != null) {
				model.initModel(ConnectionsRegistryUtil.getConnectionFor(project), project);
			}else {
				final Connection osConnection = UIUtils.getFirstElement(selection, Connection.class);
				if(osConnection != null){
					model.setConnection(osConnection);
				}
			}
		}

		
		WizardUtils.openWizardDialog(600, 1500, new DeployImageWizard(model), HandlerUtil.getActiveShell(event));
		return null;
	}
}
