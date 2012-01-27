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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.propertytable.PropertyNameCellLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.propertytable.PropertyValueCellLabelProvider;

import com.openshift.express.client.IApplication;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationDetailsDialog extends TitleAreaDialog {

	private IApplication application;

	public ApplicationDetailsDialog(IApplication application, Shell parentShell) {
		super(parentShell);
		this.application = application;
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
		TreeViewer viewer = createApplicationDetailsTable(dialogArea);
		fillApplicationDetailsTable(viewer);

		Label buttonsSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(buttonsSeparator);

		return dialogArea;
	}

	private void fillApplicationDetailsTable(final TreeViewer viewer) {
		viewer.setInput(application);
		viewer.expandToLevel(2);
	}

	private TreeViewer createApplicationDetailsTable(Composite parent) {
		Composite tableContainer = new Composite(parent, SWT.NONE);
		TreeColumnLayout treeLayout = new TreeColumnLayout();
		tableContainer.setLayout(treeLayout);

		Tree tree = new Tree(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).hint(500, 300).applyTo(tableContainer);

		final TreeViewer viewer = new TreeViewer(tree);
		viewer.setContentProvider(new ApplicationDetailsContentProvider());
		createTreeColumn("Property", 1, new PropertyNameCellLabelProvider(), viewer, treeLayout);
		createTreeColumn("Value", 3, new PropertyValueCellLabelProvider(), viewer, treeLayout);

		return viewer;
	}

	private void createTreeColumn(String name, int weight, CellLabelProvider cellLabelProvider, TreeViewer treeViewer,
			TreeColumnLayout layout) {
		final TreeViewerColumn viewerColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		final TreeColumn column = viewerColumn.getColumn();
		column.setText(name);
		column.setResizable(true);
		viewerColumn.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column, new ColumnWeightData(weight, weight * 200, true));
	}

	private void setupDialog(Composite parent) {
		parent.getShell().setText("Application Details");
		setTitle(NLS.bind("Details of Application {0}", application.getName()));
		setTitleImage(OpenShiftImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		setDialogHelpAvailable(false);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
}
