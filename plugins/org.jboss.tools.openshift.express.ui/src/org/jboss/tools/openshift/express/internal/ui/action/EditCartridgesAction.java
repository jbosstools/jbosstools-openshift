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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class EditCartridgesAction extends AbstractAction {

	public EditCartridgesAction() {
		super(OpenShiftExpressUIMessages.EDIT_CARTRIDGES_ACTION, true);
		setImageDescriptor(OpenShiftImages.TASK_REPO_NEW);
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
			try {
				final IApplication application = (IApplication) treeSelection.getFirstElement();
				final Connection connection = getConnection(application);
				EmbedCartridgeWizard wizard = new EmbedCartridgeWizard(application, connection);
				int result = WizardUtils.openWizardDialog(wizard, Display.getCurrent().getActiveShell());
				if(result == Dialog.OK) {
					RefreshViewerJob.refresh(viewer);
				}
			} catch (OpenShiftException e) {
				Logger.error("Failed to edit cartridges", e);
			} catch (URISyntaxException e) {
				Logger.error("Failed to edit cartridges", e);
			} catch (UnsupportedEncodingException e) {
				Logger.error("Failed to edit cartridges", e);
			}
		}
	}

	private Connection getConnection(final IApplication application) throws UnsupportedEncodingException, OpenShiftException, URISyntaxException {
		Connection tmpConnection = new Connection(new URI(application.getDomain().getUser().getRhlogin()), null);
		final Connection existingConnection = ConnectionsModel.getDefault().getConnectionByUrl(tmpConnection.toURLString());
		if (existingConnection == null) {
			return tmpConnection;
		} else {
			return existingConnection;
		}
	}
}
