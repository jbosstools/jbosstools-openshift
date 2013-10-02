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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.variables.EnvironmentVariablesWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 */
public class EditEnvironmentVariablesHandler extends AbstractDomainHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application == null) {
			return null;
		}
		try {
			WizardUtils.openWizardDialog(
					new EnvironmentVariablesWizard(application), Display.getCurrent().getActiveShell());
			return null;
		} catch (OpenShiftException e) {
			Logger.error("Failed to edit cartridges", e);
			return OpenShiftUIActivator.createErrorStatus("Failed to edit cartridges", e);
		}
	}
	
}
