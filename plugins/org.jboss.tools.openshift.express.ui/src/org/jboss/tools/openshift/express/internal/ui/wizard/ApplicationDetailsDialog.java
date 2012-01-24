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

import java.util.concurrent.Callable;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;

/**
 * @author André Dietisheim
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
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(titleSeparator);

		Composite treeTableContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).span(2, 1).grab(true, true).minSize(SWT.DEFAULT, 250)
				.applyTo(treeTableContainer);
		GridLayoutFactory.fillDefaults().margins(6, 6).spacing(14, 4).applyTo(treeTableContainer);
		TreeViewer viewer = createApplicationDetailsTable(treeTableContainer);
		fillApplicationDetailsTable(viewer);

		Label buttonsSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(buttonsSeparator);

		// createDetails("Name:", application.getName(), treeTableContainer);
		// createDetails("Type:", application.getCartridge().getName(), treeTableContainer);
		// createDetails("Embedded Cartridges:", new ErrorMessageCallable<String>("Embedded Cartridges") {
		//
		// @Override
		// public String call() throws Exception {
		// return StringUtils.toString(application.getEmbeddedCartridges(),
		// new StringUtils.ToStringConverter<IEmbeddableCartridge>() {
		//
		// @Override
		// public String toString(IEmbeddableCartridge cartridge) {
		// return cartridge.getName();
		// }
		// });
		// }
		//
		// }.get(), treeTableContainer);
		// createDetails("Creation Time:", new ErrorMessageCallable<String>("Creation Time") {
		//
		// @Override
		// public String call() throws Exception {
		// return RFC822DateUtils.getString(application.getCreationTime());
		// }
		// }.get(), treeTableContainer);
		// createDetails("UUIDxx:", new ErrorMessageCallable<String>("UUID") {
		//
		// @Override
		// public String call() throws Exception {
		// return application.getUUID();
		// }
		// }.get(), treeTableContainer);
		// createDetails("Git URL:", new ErrorMessageCallable<String>("Git URL") {
		//
		// @Override
		// public String call() throws Exception {
		// return application.getGitUri();
		// }
		// }.get(), treeTableContainer);
		//
		// Label publicUrlLabel = new Label(treeTableContainer, SWT.NONE);
		// publicUrlLabel.setText("Public URL:");
		// GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(publicUrlLabel);
		// Link publicUrlLink = new Link(treeTableContainer, SWT.WRAP);
		// String applicationUrl = new ErrorMessageCallable<String>("Public URL") {
		//
		// @Override
		// public String call() throws Exception {
		// return application.getApplicationUrl();
		// }
		// }.get();
		// publicUrlLink.setText("<a>" + applicationUrl + "</a>");
		// GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(publicUrlLink);
		// publicUrlLink.addSelectionListener(onPublicUrl(applicationUrl));

		return treeTableContainer;
	}

	private void fillApplicationDetailsTable(final TreeViewer viewer) {
		viewer.setContentProvider(new ApplicationDetailsContentProvider());
		viewer.setInput(application);
		viewer.expandToLevel(2);
	}

	private TreeViewer createApplicationDetailsTable(Composite tableContainer) {
		Tree tree = new Tree(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		int desiredHeight = tree.getItemHeight() * 10 + tree.getHeaderHeight();
		GridLayoutFactory.fillDefaults().margins(6, 6).spacing(14, 4).applyTo(tree);
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, desiredHeight).applyTo(tree);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		TreeColumnLayout treeLayout = new TreeColumnLayout();
		tableContainer.setLayout(treeLayout);
		TreeViewer viewer = new TreeViewer(tree);
		// sure ?
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		createTreeColumn("Property", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof ApplicationDetailsContentProvider.SimpleElement) {
					ApplicationDetailsContentProvider.SimpleElement element = (ApplicationDetailsContentProvider.SimpleElement) cell
							.getElement();
					cell.setText(element.getProperty());
				} else if (cell.getElement() instanceof ApplicationDetailsContentProvider.ContainerElement) {
					ApplicationDetailsContentProvider.ContainerElement element = (ApplicationDetailsContentProvider.ContainerElement) cell
							.getElement();
					cell.setText(element.getProperty());
				}
			}
		}, viewer, treeLayout);

		createTreeColumn("Value", 1, new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof ApplicationDetailsContentProvider.SimpleElement) {
					ApplicationDetailsContentProvider.SimpleElement element = (ApplicationDetailsContentProvider.SimpleElement) cell
							.getElement();
					cell.setText(element.getValue());
					if (element.isLink()) {
						Styler style = new Styler() {
							@Override
							public void applyStyles(TextStyle textStyle) {
								textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
								textStyle.underline = true;
							}

						};
						StyledString styledString = new StyledString(cell.getText(), style);
						cell.setStyleRanges(styledString.getStyleRanges());
						cell.setText(styledString.getString());
						//cell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_HAND));
					}
				}
			}
		}, viewer, treeLayout);

		return viewer;

	}

	private void createTreeColumn(String name, int weight, CellLabelProvider cellLabelProvider, TreeViewer treeViewer,
			TreeColumnLayout layout) {
		final TreeViewerColumn viewerColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		final TreeColumn column = viewerColumn.getColumn();
		column.setText(name);
		column.setResizable(true);
		viewerColumn.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column, new ColumnWeightData(weight, true));
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

	private SelectionAdapter onPublicUrl(final String applicationUrl) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ILog log = OpenShiftUIActivator.getDefault().getLog();
				BrowserUtil.checkedCreateExternalBrowser(applicationUrl, OpenShiftUIActivator.PLUGIN_ID, log);
			}
		};
	}

	private void createDetails(String name, String value, Composite container) {
		Label label = new Label(container, SWT.None);
		label.setText(name);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(label);
		Text text = new Text(container, SWT.NONE);
		text.setEditable(false);
		text.setBackground(container.getBackground());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(text);
		text.setText(value);
	}

	private abstract class ErrorMessageCallable<T> implements Callable<T> {

		private String fieldName;

		public ErrorMessageCallable(String fieldName) {
			this.fieldName = fieldName;
		}

		public T get() {
			try {
				return call();
			} catch (Exception e) {
				setErrorMessage(NLS.bind("Could not get {0}: {1}", fieldName, e.getMessage()));
				return null;
			}
		}

		@Override
		public abstract T call() throws Exception;
	}
}
