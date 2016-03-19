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

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.project.NewProjectWizard;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * @author Fred Bricon
 */
public class NewProjectHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if(connection == null) {
			IProject project = UIUtils.getFirstElement(selection, IProject.class);
			if (project != null) {
				connection = ConnectionsRegistryUtil.getConnectionFor(project);
			}
		}
		if(connection == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No connection selected"); //$NON-NLS-1$
		}
		openNewProjectDialog(connection, HandlerUtil.getActiveShell(event));
		return null;
	}

	public static void openNewProjectDialog(final Connection connection, final Shell shell) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				List<IProject> oldProjects = connection.getResources(ResourceKind.PROJECT);
				WizardUtils.openWizardDialog(new NewProjectWizard(connection, oldProjects), shell);
			}
		});
	}

}
