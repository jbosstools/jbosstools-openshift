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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.RefreshConnectionsModelJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 */
public class EditCartridgesHandler extends AbstractDomainHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IApplication application = getApplication(HandlerUtil.getCurrentSelection(event));
		if (application == null) {
			return null;
		}
		try {
			int result = WizardUtils.openWizardDialog(
					new EmbedCartridgeWizard(application), Display.getCurrent().getActiveShell());
			if (result == Dialog.OK) {
				new RefreshConnectionsModelJob(application.getDomain().getUser()).schedule();;
			}
			return null;
		} catch (OpenShiftException e) {
			Logger.error("Failed to edit cartridges", e);
			return OpenShiftUIActivator.createErrorStatus("Failed to edit cartridges", e);
		}
	}

protected IApplication getApplication(ISelection selection) {
	IApplication application = UIUtils.getFirstElement(selection, IApplication.class);
	if (application == null) {
		IEmbeddedCartridge cartridge = UIUtils.getFirstElement(selection, IEmbeddedCartridge.class);
		if (cartridge != null) {
			application = cartridge.getApplication();
		}
	}
	return application;
}
	
}
