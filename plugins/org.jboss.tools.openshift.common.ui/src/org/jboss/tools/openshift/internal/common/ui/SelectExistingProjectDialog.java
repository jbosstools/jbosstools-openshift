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
package org.jboss.tools.openshift.internal.common.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
//import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.common.core.preferences.StringPreferenceValue;

/**
 * @author André Dietisheim
 */
public class SelectExistingProjectDialog extends SelectProjectDialog {

	StringPreferenceValue showAllPreferences = new StringPreferenceValue("FILTER_ACCEPTABLE_PROJECTS", OpenShiftCommonUIActivator.PLUGIN_ID);
	private boolean showAll;
	
	public SelectExistingProjectDialog(String message, Shell shell) {
		super(shell);
		setMessage(NLS.bind("{0}.\nOnly non-shared projects or Git projects allowed.", message));
		this.showAll = getShowAllPreferences();
	}

	private boolean getShowAllPreferences() {
		boolean showAll = false;
		if(!StringUtils.isEmpty(showAllPreferences.get())) {
			showAll = Boolean.valueOf(showAllPreferences.get());
		}
		return showAll;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		Button filterCheckbox = new Button(dialogArea, SWT.CHECK);
		filterCheckbox.setText("&Show all projects");
		filterCheckbox.setSelection(showAll);
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
					showAll = ((Button) e.widget).getSelection();
					showAllPreferences.set(String.valueOf(showAll));
					setListElements(getProjects());
				}
			}
		};
	}

	@Override
	protected boolean isValid(IProject project) {
		if (showAll) {
			return true;
		}
		
		if (!project.isAccessible()) {
			return false;
		}
		
		if(ProjectUtils.isInternalRSE(project.getName())) {
			return false;
		}
			
		if(isNonGitShared(project)) {
			return false;
		}

		return true;
	}

	protected boolean isNonGitShared(IProject project) {
		if (EGitUtils.isShared(project)) {
			if (!EGitUtils.isSharedWithGit(project)) {
				return true;
			}
		}
		return false;
	}
}
