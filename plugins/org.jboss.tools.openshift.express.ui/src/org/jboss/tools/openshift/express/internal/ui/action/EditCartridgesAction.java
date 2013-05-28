/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class EditCartridgesAction extends AbstractOpenShiftAction {

	public EditCartridgesAction() {
		super(OpenShiftExpressUIMessages.EDIT_CARTRIDGES_ACTION, true);
		setImageDescriptor(OpenShiftImages.TASK_REPO_NEW);
	}
	
	@Override
	public void run() {
		IApplication application = getApplication(getSelection());
		if (application == null) {
			return;
		}
		try {
			int result = WizardUtils.openWizardDialog(
					new EmbedCartridgeWizard(application), Display.getCurrent().getActiveShell());
			if (result == Dialog.OK) {
				RefreshViewerJob.refresh(viewer);
			}
		} catch (OpenShiftException e) {
			Logger.error("Failed to edit cartridges", e);
		}
	}

	protected IApplication getApplication(ISelection selection) {
		IApplication application = UIUtils.getFirstElement(selection, IApplication.class);
		if (application == null) {
			IEmbeddedCartridge cartridge = UIUtils.getFirstElement(getSelection(), IEmbeddedCartridge.class);
			if (cartridge != null) {
				application = cartridge.getApplication();
			}
		}
		return application;
	}
}
