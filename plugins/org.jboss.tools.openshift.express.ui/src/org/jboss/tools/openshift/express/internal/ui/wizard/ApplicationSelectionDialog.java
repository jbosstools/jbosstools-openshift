package org.jboss.tools.openshift.express.internal.ui.wizard;

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

import java.util.Collection;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationSelectionDialog extends TitleAreaDialog {

	private final ApplicationSelectionDialogModel dialogModel;
	private final DataBindingContext dbc;
	private final IWizard wizard;
	private TableViewer tableViewer;

	public ApplicationSelectionDialog(OpenShiftExpressApplicationWizard wizard, IApplication selectedApplication,
			Shell parentShell) {
		super(parentShell);
		this.dialogModel = new ApplicationSelectionDialogModel(wizard.getWizardModel());
		this.dialogModel.setSelectedApplication(selectedApplication);
		this.wizard = wizard;
		this.dbc = new DataBindingContext();
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setupDialog(parent);
		return control;
	}

	private void setupDialog(Composite parent) {
		parent.getShell().setText("Application selection");
		setTitle("Select an application in the list");
		setTitleImage(OpenShiftImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
		setDialogHelpAvailable(false);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Creates the SWT Group in which a table which will display the existing applications with their corresponding
	 * type. Before each application, a radio button will let the user choose which application to import in his
	 * workspace.
	 * 
	 * @param container
	 * @param dbc
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Label titleSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(titleSeparator);

		Composite dialogArea = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(dialogArea);

		Label applicationLabel = new Label(dialogArea, SWT.NONE);
		applicationLabel.setText("Existing Applications on OpenShift");
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).span(5, 1).applyTo(applicationLabel);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(dialogArea);

		Composite tableContainer = new Composite(dialogArea, SWT.NONE);
		tableViewer = createTable(tableContainer);
		tableViewer.setInput(dialogModel.getApplications());
		GridDataFactory.fillDefaults().span(1, 2).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 200)
				.applyTo(tableContainer);
		tableViewer.addDoubleClickListener(onApplicationDoubleClick());
		Binding selectedApplicationBinding = dbc.bindValue(ViewerProperties.singleSelection().observe(tableViewer),
				BeanProperties.value(ApplicationSelectionDialogModel.PROPERTY_SELECTED_APPLICATION)
						.observe(dialogModel), new UpdateValueStrategy().setAfterGetValidator(new IValidator() {
					@Override
					public IStatus validate(Object value) {
						if (value != null) {
							return ValidationStatus.ok();
						} else {
							return ValidationStatus.cancel("Select an application in the list below.");
						}
					}
				}), null);

		/*Button refreshButton = new Button(dialogArea, SWT.PUSH);
		refreshButton.setText("R&efresh");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, false).hint(80, SWT.DEFAULT)
				.applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefresh(dbc));
		*/
		Button detailsButton = new Button(dialogArea, SWT.PUSH);
		detailsButton.setText("De&tails...");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).hint(80, SWT.DEFAULT)
				.applyTo(detailsButton);
		DataBindingUtils.bindEnablementToValidationStatus(detailsButton, IStatus.OK, dbc, selectedApplicationBinding);
		detailsButton.addSelectionListener(onDetails(dbc));
		// bottom filler
		Composite spacer = new Composite(dialogArea, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(spacer);
		// horizontal line to separate content from buttons
		Label buttonsSeparator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(buttonsSeparator);

		return dialogArea;
	}

	private IDoubleClickListener onApplicationDoubleClick() {
		return new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
//				openDetailsDialog();
				setApplicationSelected(event.getSelection());
			}
		};
	}

	private void setApplicationSelected(ISelection selection) {
		if(selection instanceof StructuredSelection) {
			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if(firstElement instanceof IApplication) {
				IApplication application = (IApplication) firstElement;
				dialogModel.setSelectedApplication(application);
				close();
			}
		}
	}
	
	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		int desiredHeight = table.getItemHeight() * 10 + table.getHeaderHeight();
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, desiredHeight).applyTo(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		createTableColumn("Name", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final IApplication application = (IApplication) cell.getElement();
				cell.setText(application.getName());
			}
		}, viewer, tableLayout);

		createTableColumn("Type", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final IApplication application = (IApplication) cell.getElement();
				cell.setText(application.getCartridge().getName());
			}
		}, viewer, tableLayout);

		createTableColumn("URL", 3, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final IApplication application = (IApplication) cell.getElement();
				cell.setText(application.getApplicationUrl());
			}
		}, viewer, tableLayout);

		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.getColumn().setResizable(true);
		column.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	private SelectionAdapter onRefresh(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					WizardUtils.runInWizard(new Job("Loading applications...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							dialogModel.refresh(); 
							final Collection<IApplication> applications = dialogModel.getApplications();
							setViewerInput(applications);
							return Status.OK_STATUS;
						}

					}, wizard.getContainer(), dbc);
				} catch (Exception e) {
					Logger.error("Failed to refresh applications list", e);
					// ignore
				}
			}
		};
	}

	private SelectionAdapter onDetails(DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				openDetailsDialog();
			}
		};
	}

	private void openDetailsDialog() {
		new ApplicationDetailsDialog(dialogModel.getSelectedApplication(), getShell()).open();
	}

	private void setViewerInput(final Collection<IApplication> applications) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				Logger.debug("Setting {} application(s) in the viewer", applications.size());
				tableViewer.setInput(applications);
			}
		});
	}

	public IApplication getSelectedApplication() {
		return dialogModel.getSelectedApplication();
	}
}
