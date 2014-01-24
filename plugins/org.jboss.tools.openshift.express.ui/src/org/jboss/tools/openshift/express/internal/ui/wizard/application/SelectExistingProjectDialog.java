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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class SelectExistingProjectDialog extends ElementListSelectionDialog {

	private static final String RSE_INTERNAL_PROJECTS = "RemoteSystems";
	StringPreferenceValue filterPreferences = new StringPreferenceValue("FILTER_ACCEPTABLE_PROJECTS", OpenShiftUIActivator.PLUGIN_ID);
	private boolean filterEnabled;
	
	public SelectExistingProjectDialog(String openShiftAppName, Shell shell) {
		super(shell, new ProjectLabelProvider());
		setTitle("Select Existing Project");
		setMessage(NLS.bind(
				"Select an existing project for {0}.\nOnly non-shared projects or Git projects allowed.",
				openShiftAppName));
		setMultipleSelection(false);
		setAllowDuplicates(false);
		this.filterEnabled = getFilterPreferences();
		setElements(getProjects());
	}

	private boolean getFilterPreferences() {
		if(StringUtils.isEmpty(filterPreferences.get())) {
			return true;
		}
		return Boolean.valueOf(filterPreferences.get());
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

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		Button filterCheckbox = new Button(dialogArea, SWT.CHECK);
		filterCheckbox.setText("&filter acceptable projects");
		filterCheckbox.setSelection(filterEnabled);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(filterCheckbox);
		filterCheckbox.addSelectionListener(onFilterChecked());
		return dialogArea;
	}

	private SelectionListener onFilterChecked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				if (e.widget instanceof Button) {
					filterEnabled = ((Button) e.widget).getSelection();
					filterPreferences.store(String.valueOf(filterEnabled));
					setListElements(getProjects());
				}
			}
			
		};
	}

	private boolean isValid(IProject project) {
		if (!filterEnabled) {
			return true;
		}
		
		if (!project.isAccessible()) {
			return false;
		}
		
		if(isInternalRSEProject(project.getName())) {
			return false;
		}
			
		if(isNonGitShared(project)) {
			return false;
		}

		return true;
	}

	private boolean isInternalRSEProject(String name) {
		return name != null
				&& name.startsWith(RSE_INTERNAL_PROJECTS);
	}

	protected boolean isNonGitShared(IProject project) {
		if (EGitUtils.isShared(project)) {
			if (!EGitUtils.isSharedWithGit(project)) {
				return true;
			}
		}
		return false;
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
