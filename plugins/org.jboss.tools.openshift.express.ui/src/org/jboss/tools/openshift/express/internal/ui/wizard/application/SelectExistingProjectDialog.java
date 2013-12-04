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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.jboss.tools.openshift.egit.core.EGitUtils;

/**
 * @author André Dietisheim
 */
public class SelectExistingProjectDialog extends ElementListSelectionDialog {

	public SelectExistingProjectDialog(String openShiftAppName, Shell shell) {
		super(shell, new ProjectLabelProvider());
		setTitle("Select Existing Project");
		setMessage(NLS.bind(
				"Select an existing project for {0}.\nOnly non-shared projects or Git projects allowed.",
				openShiftAppName));
		setMultipleSelection(false);
		setAllowDuplicates(false);
		setElements(getProjects());
	}

	private Object[] getProjects() {
		List<IProject> projects = new ArrayList<IProject>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (isValid(project)) {
				projects.add(project);
			}
		}
		return projects.toArray();
	}

	private boolean isValid(IProject project) {
		if (!project.isAccessible()) {
			return false;
		}
		if (EGitUtils.isShared(project)) {
			if (!EGitUtils.isSharedWithGit(project)) {
				return false;
			}
		}

		return true;
	}

	private static class ProjectLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IProject)) {
				return null;
			}

			return ((IProject) element).getName();
		}

	}

}
