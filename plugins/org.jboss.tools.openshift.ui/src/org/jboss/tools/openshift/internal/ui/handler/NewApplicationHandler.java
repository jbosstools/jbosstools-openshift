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
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;
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
		IProject project = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IProject.class);
		NewApplicationWizardModel model = new NewApplicationWizardModel(project);
		NewApplicationWizard wizard = new NewApplicationWizard(model);
		WizardUtils.openWizard(wizard, HandlerUtil.getActiveShell(event));

		return null;
	}

}
