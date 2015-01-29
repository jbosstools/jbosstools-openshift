/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;
import org.jboss.tools.openshift.internal.ui.deployment.DeploymentWizard;
import org.jboss.tools.openshift.internal.ui.deployment.DeploymentWizardContext;

//import com.openshift.kube.Project;
//import com.openshift.kube.capability.ImageRegistryHosting;

/**
 * @author Jeff Cantrill
 */
public class NewDeploymentHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
//		if (selection == null 
//				|| selection.isEmpty()
//				|| !(selection instanceof StructuredSelection))
//			return null;
//		Object element = ((StructuredSelection) selection).getFirstElement();
//		if (!(element instanceof Project))
//			return null;
//		
//		Project p = (Project) element;
//		if(!p.getClient().isCapableOf(ImageRegistryHosting.class))
//			return null;
//		
//		ImageRegistryHosting hosting = p.getClient().getCapability(ImageRegistryHosting.class);
//		DeploymentWizardContext context = new DeploymentWizardContext(p.getClient(), p, hosting.getRegistryUri());
//		if (WizardUtils.openWizard(new DeploymentWizard(context), HandlerUtil.getActiveShell(event))) {
//		}

		return null;
	}

}
