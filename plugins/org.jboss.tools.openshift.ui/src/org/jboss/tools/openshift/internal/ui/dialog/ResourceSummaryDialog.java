/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

import com.openshift.restclient.model.IResource;

/**
 * A dialog to display info about a list of resources
 * @author jeff.cantrill
 */
public class ResourceSummaryDialog  extends TitleAreaDialog {

	private Collection<IResource> resources;
	private String message;

	public ResourceSummaryDialog(Shell parentShell, Collection<IResource> resources, String message) {
		super(parentShell);
		this.resources = resources;
		this.message = message;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setupDialog(parent);
		return control;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Label titleSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(titleSeparator);

		Composite dialogArea = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(dialogArea);
		TreeViewer viewer = createTable(dialogArea);
		fillApplicationDetailsTable(viewer);

		Label buttonsSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(buttonsSeparator);

		return dialogArea;
	}
	
	private void fillApplicationDetailsTable(final TreeViewer viewer) {
		viewer.setInput(resources);
	}
	
	private TreeViewer createTable(Composite parent) {
		Composite tableContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.FILL).grab(true, true).hint(500, 300).applyTo(tableContainer);

		TreeColumnLayout treeLayout = new TreeColumnLayout();
		tableContainer.setLayout(treeLayout);
		TreeViewer viewer = new TreeViewer(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(new ResourceSummaryContentProvider());
		viewer.setLabelProvider(new ResourceSummaryLabelProvider());

		return viewer;
	}
	
	private void setupDialog(Composite parent) {
		parent.getShell().setText("Create Application Summary");
		setTitle(message);
		setTitleImage(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		setDialogHelpAvailable(false);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
}
