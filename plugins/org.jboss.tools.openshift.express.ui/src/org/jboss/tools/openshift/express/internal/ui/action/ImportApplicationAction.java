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
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class ImportApplicationAction extends AbstractAction {

	public ImportApplicationAction() {
		super(OpenShiftExpressUIMessages.IMPORT_APPLICATION_ACTION, true);
		setImageDescriptor(OpenShiftImages.GO_INTO);
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection instanceof ITreeSelection 
				&& treeSelection.getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) treeSelection.getFirstElement();
			final UserDelegate user = getUser(treeSelection.getPaths());
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(application.getName());
			OpenShiftExpressApplicationWizard wizard = new ImportOpenShiftExpressApplicationWizard(user, project, application);
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
			dialog.create();
			dialog.open();
			
		}
	}

	private UserDelegate getUser(TreePath[] paths) {
		UserDelegate user = null;
		if( paths != null 
				&& paths.length == 1 ) {
			Object selection = paths[0].getParentPath().getLastSegment();
			if( selection instanceof UserDelegate )
				user = (UserDelegate) selection;
		}
		return user;
	}

	
}
