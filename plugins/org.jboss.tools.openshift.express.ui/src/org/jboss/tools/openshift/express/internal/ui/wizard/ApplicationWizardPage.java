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
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.IDomain;
import org.jboss.tools.openshift.express.client.NotFoundOpenShiftException;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardPage extends AbstractOpenShiftWizardPage {

	private TableViewer viewer;
	private ApplicationWizardPageModel model;
	private ImportProjectWizardModel wizardModel;

	protected ApplicationWizardPage(IWizard wizard, ImportProjectWizardModel wizardModel) {
		super("Application selection", "Please select an application to start with, or create a new one.",
				"Application selection", wizard);
		this.wizardModel = wizardModel;
		this.model = new ApplicationWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		createDomainGroup(container, dbc);
		createApplicationsGroup(container, dbc);
	}

	private void createDomainGroup(Composite container, DataBindingContext dbc) {
		Group domainGroup = new Group(container, SWT.BORDER);
		domainGroup.setText("Domain");
		GridDataFactory.fillDefaults()
				.grab(true, false).align(SWT.FILL, SWT.TOP).span(3, 1).applyTo(domainGroup);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(4).applyTo(domainGroup);
		Label namespaceLabel = new Label(domainGroup, SWT.NONE);
		namespaceLabel.setText("&Domain name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(namespaceLabel);
		Text namespaceText = new Text(domainGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(namespaceText);
		Binding namespaceBinding = DataBindingUtils.bindMandatoryTextField(
				namespaceText, "Domain", ApplicationWizardPageModel.PROPERTY_NAMESPACE, model, dbc);
		Button createRenameButton = new Button(domainGroup, SWT.PUSH);
		DataBindingUtils.bindEnablementToValidationStatus(createRenameButton, IStatus.OK, dbc, namespaceBinding);
		dbc.bindValue(WidgetProperties.text().observe(createRenameButton)
				, BeanProperties.value(ApplicationWizardPageModel.PROPERTY_DOMAIN).observe(model)
				, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER)
				, new UpdateValueStrategy().setConverter(new Converter(IDomain.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof IDomain) {
							return "&Rename";
						} else {
							return "&Create";
						}
					}
				}));
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).hint(80, SWT.DEFAULT).applyTo(createRenameButton);
		createRenameButton.addSelectionListener(onCreateRenameDomain(dbc));
	}

	private void createApplicationsGroup(Composite container, DataBindingContext dbc) {
		Group applicationGroup = new Group(container, SWT.BORDER);
		applicationGroup.setText("Available Applications");
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(400, 260).span(3, 1)
				.applyTo(applicationGroup);
		GridLayoutFactory.fillDefaults().numColumns(4).margins(6, 6).applyTo(applicationGroup);

		Composite tableContainer = new Composite(applicationGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults().span(4, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		viewer.addDoubleClickListener(onApplicationDoubleClick());
		Binding selectedApplicationBinding = dbc.bindValue(
				ViewerProperties.singleSelection().observe(viewer),
				BeanProperties.value(ApplicationWizardPageModel.PROPERTY_SELECTED_APPLICATION).observe(model),
				new UpdateValueStrategy().setAfterGetValidator(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (value != null) {
							return ValidationStatus.ok();
						}
						else {
							return ValidationStatus
									.info("Please select an application to start with, or create a new one");
						}
					}
				}),
				null);

		Button newButton = new Button(applicationGroup, SWT.PUSH);
		newButton.setText("Ne&w");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(80, SWT.DEFAULT).applyTo(newButton);
		newButton.addSelectionListener(onNew(dbc));

		Button deleteButton = new Button(applicationGroup, SWT.PUSH);
		deleteButton.setText("&Delete");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(80, SWT.DEFAULT).applyTo(deleteButton);
		DataBindingUtils.bindEnablementToValidationStatus(deleteButton, IStatus.OK, dbc, selectedApplicationBinding);
		deleteButton.addSelectionListener(onDelete(dbc));

		Button embedButton = new Button(applicationGroup, SWT.PUSH);
		embedButton.setText("E&mbed");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(80, SWT.DEFAULT).applyTo(embedButton);
		DataBindingUtils.bindEnablementToValidationStatus(embedButton, IStatus.OK, dbc, selectedApplicationBinding);
		embedButton.addSelectionListener(onEmbed(dbc));

		Button detailsButton = new Button(applicationGroup, SWT.PUSH);
		detailsButton.setText("De&tails");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).hint(80, SWT.DEFAULT)
				.applyTo(detailsButton);
		DataBindingUtils.bindEnablementToValidationStatus(detailsButton, IStatus.OK, dbc, selectedApplicationBinding);
		detailsButton.addSelectionListener(onDetails(dbc));
	}

	private SelectionListener onCreateRenameDomain(DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model.hasDomain()) {
					renameDomain();
				} else {
					try {
						createDomain();
					} catch (OpenShiftException ex) {
						IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, ex.getMessage(), ex);
						OpenShiftUIActivator.getDefault().getLog().log(status);
						ErrorDialog.openError(getShell(), "Error creating domain",
								"An error occurred while creating the domain.", status);
					}
				}
			}
		};
	}

	private void createDomain() throws OpenShiftException {
		if (WizardUtils.openWizardDialog(
				new NewDomainDialog(model.getNamespace(), wizardModel), getContainer().getShell()) == Dialog.OK) {
			model.loadDomain();
		}
	}

	private void renameDomain() {
		try {
			WizardUtils.runInWizard(
					new Job("Renaming domain...") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								model.renameDomain();
								return Status.OK_STATUS;
							} catch (Exception e) {
								return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
										"Could not rename domain", e);
							}
						}
					}, getContainer(), getDataBindingContext());
		} catch (Exception ex) {
			// ignore
		}
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
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
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
		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);

		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	private SelectionAdapter onDelete(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					if (MessageDialog
							.openQuestion(
									getShell(),
									"Delete Application",
									NLS.bind(
											"You're up to delete all data within an application. The data may not be recovered. "
													+ "Are you sure that you want to delete application {0}?",
											model.getSelectedApplication().getName()))) {
						WizardUtils.runInWizard(
								new Job(NLS.bind("Deleting application \"{0}\"...",
										model.getSelectedApplication().getName())) {

									@Override
									protected IStatus run(IProgressMonitor monitor) {
										try {
											model.destroyCurrentApplication();
											refreshViewer();
											return Status.OK_STATUS;
										} catch (OpenShiftException e) {
											return OpenShiftUIActivator.createErrorStatus(
													NLS.bind("Could not delete application \"{0}\"",
															model.getSelectedApplication().getName())
													, e);
										}
									}
								}, getContainer(), dbc);
					}
				} catch (Exception ex) {
					// ignore
				}
			}
		};
	}

	private SelectionAdapter onNew(DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = getContainer().getShell();
				NewApplicationWizard applicationDialog = new NewApplicationWizard(model.getUser());
				if (WizardUtils.openWizardDialog(applicationDialog, shell) == Dialog.OK) {
					viewer.refresh();
					model.setSelectedApplication(applicationDialog.getApplication());
				}
			}
		};
	}

	private SelectionAdapter onEmbed(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardUtils.openWizardDialog(new EmbedCartridgeWizard(model.getUser()), getShell());
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
		new ApplicationDetailsDialog(model.getSelectedApplication(), getShell()).open();
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading applications...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.loadDomain();
						final Collection<IApplication> applications = model.loadApplications();
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

		} catch (Exception ex) {
			// ignore
		}
	}

	private void refreshViewer() {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}

	private void clearViewer() {
		setViewerInput(new ArrayList<IApplication>());
	}

	private void setViewerInput(final Collection<IApplication> applications) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(applications);
			}
		});
	}
}
