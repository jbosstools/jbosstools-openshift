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
package org.jboss.tools.openshift.internal.common.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * @author André Dietisheim
 */
public class SelectProjectDialog extends ElementListSelectionDialog {

	public SelectProjectDialog(Shell shell) {
		super(shell, new ProjectLabelProvider());
		setTitle("Select Existing Project");
		setMessage("Select an existing project.");
		setMultipleSelection(false);
		setAllowDuplicates(false);
		initRestrictions();
		setElements(getProjects());
	}

	/**
	 * Initializes the state that affects the result of getProject().
	 * This method is called once at starting the dialog before the first call of getProject(). 
	 */
	protected void initRestrictions() {
	}

	protected Object[] getProjects() {
		List<IProject> projects = new ArrayList<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (isValid(project)) {
				projects.add(project);
			}
		}
		return projects.toArray();
	}

	protected boolean isValid(IProject project) {
		return true;
	}

	public IProject getSelectedProject() {
		Object[] results = getResult();
		if (results == null 
				|| results.length < 1) {
			return null;
		} else {
			return (IProject) results[0];
		}
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
