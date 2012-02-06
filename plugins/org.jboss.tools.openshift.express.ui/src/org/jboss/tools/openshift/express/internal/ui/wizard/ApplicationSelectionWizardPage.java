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

import java.util.ArrayList;
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationSelectionWizardPage extends AbstractOpenShiftWizardPage {

	private TableViewer tableViewer;
	private final ApplicationSelectionWizardPageModel pageModel;

	public ApplicationSelectionWizardPage(IWizard wizard, IOpenShiftWizardModel wizardModel) {
		super("Application selection", "Click 'next' or select another application to import.",
				"Application selection", wizard);
		this.pageModel = new ApplicationSelectionWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		createApplicationsGroup(container, dbc);
	}

	/**
	 * Creates the SWT Group in which a table which will display the existing applications with their corresponding
	 * type. Before each application, a radio button will let the user choose which application to import in his
	 * workspace.
	 * 
	 * @param container
	 * @param dbc
	 */
	private void createApplicationsGroup(Composite container, DataBindingContext dbc) {
		Label applicationLabel = new Label(container, SWT.NONE);
		applicationLabel.setText("Existing Applications on OpenShift");
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).span(5, 1).applyTo(applicationLabel);
		// GridLayoutFactory.fillDefaults().numColumns(4).margins(6, 6).applyTo(applicationGroup);

		Composite tableContainer = new Composite(container, SWT.NONE);
		this.tableViewer = createTable(tableContainer);
		
		GridDataFactory.fillDefaults().span(1, 2).align(SWT.FILL, SWT.FILL).grab(true, false)
				//.hint(SWT.DEFAULT, desiredHeight)
				.applyTo(tableContainer);
		tableViewer.addDoubleClickListener(onApplicationDoubleClick());
		Binding selectedApplicationBinding = dbc.bindValue(ViewerProperties.singleSelection().observe(tableViewer),
				BeanProperties.value(ApplicationWizardPageModel.PROPERTY_SELECTED_APPLICATION).observe(pageModel),
				new UpdateValueStrategy().setAfterGetValidator(new IValidator() {
					@Override
					public IStatus validate(Object value) {
						if (value != null) {
							return ValidationStatus.ok();
						} else {
							return ValidationStatus.cancel("Select an application in the list below.");
						}
					}
				}), null);

		Button refreshButton = new Button(container, SWT.PUSH);
		refreshButton.setText("R&efresh");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, false).hint(80, SWT.DEFAULT)
				.applyTo(refreshButton);
		// DataBindingUtils.bindEnablementToValidationStatus(refreshButton, IStatus.OK, dbc,
		// selectedApplicationBinding);
		refreshButton.addSelectionListener(onRefresh(dbc));

		Button detailsButton = new Button(container, SWT.PUSH);
		detailsButton.setText("De&tails");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).hint(80, SWT.DEFAULT)
				.applyTo(detailsButton);
		DataBindingUtils.bindEnablementToValidationStatus(detailsButton, IStatus.OK, dbc, selectedApplicationBinding);
		detailsButton.addSelectionListener(onDetails(dbc));
		// bottom filler
		Composite spacer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true)
		.applyTo(spacer);

	}

	private IDoubleClickListener onApplicationDoubleClick() {
		return new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openDetailsDialog();
			}
		};
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
				IApplication application = (IApplication) cell.getElement();
				cell.setText(application.getName());
			}
		}, viewer, tableLayout);

		createTableColumn("Type", 1, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplication application = (IApplication) cell.getElement();
				cell.setText(application.getCartridge().getName());
			}
		}, viewer, tableLayout);

		createTableColumn("URL", 3, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				IApplication application = (IApplication) cell.getElement();
				try {
					cell.setText(application.getApplicationUrl());
				} catch (OpenShiftException e) {
				}
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
							try {
								final Collection<IApplication> applications = pageModel.getApplications();
								setViewerInput(applications);
								return Status.OK_STATUS;
							} catch (NotFoundOpenShiftException e) {
								// no domain and therefore no applications present
								clearViewer();
								return Status.OK_STATUS;
							} catch (Exception e) {
								clearViewer();
								return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
										"Could not load applications", e);
							}
						}

					}, getContainer(), getDataBindingContext());

				} catch (Exception e) {
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
		new ApplicationDetailsDialog(pageModel.getSelectedApplication(), getShell()).open();
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading applications...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						final Collection<IApplication> applications = pageModel.getApplications();
						setViewerInput(applications);
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						// no domain and therefore no applications present
						clearViewer();
						return Status.OK_STATUS;
					} catch (Exception e) {
						clearViewer();
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not load applications",
								e);
					}
				}

			}, getContainer(), getDataBindingContext());

		} catch (Exception ex) {
			// ignore
		}
	}

	private void clearViewer() {
		Logger.debug("Clearing applications list in viewer");
		setViewerInput(new ArrayList<IApplication>());
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
}
