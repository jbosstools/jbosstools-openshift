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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerUtils;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftExpressApplicationWizard;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class ImportApplicationAction extends AbstractOpenShiftAction {

	public ImportApplicationAction() {
		super(OpenShiftExpressUIMessages.IMPORT_APPLICATION_ACTION, true);
		setImageDescriptor(OpenShiftImages.GO_INTO);
	}
	
	@Override
	public void run() {
		final IApplication application = UIUtils.getFirstElement(getSelection(), IApplication.class);
		if (application == null) {
			return;
		}
		final Connection connection = OpenShiftExplorerUtils.getConnectionFor(getSelection());
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(application.getName());
		OpenShiftExpressApplicationWizard wizard = new ImportOpenShiftExpressApplicationWizard(connection, project,
				application);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dialog.create();
		dialog.open();

	}
	
}
