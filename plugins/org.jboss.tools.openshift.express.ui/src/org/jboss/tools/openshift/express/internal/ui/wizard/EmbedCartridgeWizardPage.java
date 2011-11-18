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
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IEmbeddableCartridge;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.common.StringUtils;

/**
 * @author André Dietisheim
 */
public class EmbedCartridgeWizardPage extends AbstractOpenShiftWizardPage {

	private EmbedCartridgeWizardPageModel model;
	private CheckboxTableViewer viewer;

	public EmbedCartridgeWizardPage(ApplicationWizardModel wizardModel, IWizard wizard) {
		super("Embed Cartridges", "Please select the cartridges to embed into your application",
				"EmbedCartridgePage", wizard);
		this.model = new EmbedCartridgeWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group embedGroup = new Group(parent, SWT.NONE);
		embedGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults()
				.hint(200, 150).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(embedGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(3).margins(6, 6).applyTo(embedGroup);

		Composite tableContainer = new Composite(embedGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(3, 1).hint(200, 150).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		viewer.addCheckStateListener(onCartridgeChecked());

		Button checkAllButton = new Button(embedGroup, SWT.PUSH);
		checkAllButton.setText("Embed A&ll");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.CENTER).applyTo(checkAllButton);
		checkAllButton.addSelectionListener(onCheckAll());

		Button uncheckAllButton = new Button(embedGroup, SWT.PUSH);
		uncheckAllButton.setText("Embed N&one");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.CENTER).applyTo(uncheckAllButton);
		uncheckAllButton.addSelectionListener(onUncheckAll());
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());

		createTableColumn("Embeddable Cartridge", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) cell.getElement();
				cell.setText(cartridge.getName());
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

	private ICheckStateListener onCartridgeChecked() {
		return new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
				if (event.getChecked()) {
					if (IEmbeddableCartridge.PHPMYADMIN_34.equals(cartridge)) {
						addPhpMyACartridge(cartridge);
					} else if (IEmbeddableCartridge.JENKINS_14.equals(cartridge)) {
						addJenkinsCartridge(cartridge);
					}
				} else {
					model.getSelectedEmbeddableCartridges().remove(cartridge);
				}
			}
		};
	}

	private void addJenkinsCartridge(final IEmbeddableCartridge cartridge) {
		if (model.hasApplication(ICartridge.JENKINS_14)) {
			model.getSelectedEmbeddableCartridges().add(cartridge);
		} else {
			final JenkinsApplicationDialog dialog = new JenkinsApplicationDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				try {
					final String name = dialog.getValue();
					WizardUtils.runInWizard(new Job(
							NLS.bind("Creating jenkins application \"{0}\"...", name)) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								model.createJenkinsApplication(name);
								return Status.OK_STATUS;
							} catch (Exception e) {
								getShell().getDisplay().syncExec(new Runnable() {
									@Override
									public void run() {
										viewer.setChecked(cartridge, false);
									}
								});
								return OpenShiftUIActivator
										.createErrorStatus("Could not load embeddable cartridges", e);
							}
						}

					}, getContainer(), getDataBindingContext());
					model.getSelectedEmbeddableCartridges().add(cartridge);
				} catch (Exception e) {
					// ignore
				}
			} else {
				viewer.setChecked(cartridge, false);
			}
		}
	}

	private void addPhpMyACartridge(IEmbeddableCartridge cartridge) {
		if (MessageDialog.openQuestion(getShell(), "Enable MySQL cartridge",
				"To embed PhpMyAdmin, you'd also have to embed MySql. ")) {
			List<IEmbeddableCartridge> selectedCartriges = model.getSelectedEmbeddableCartridges();
			viewer.setChecked(IEmbeddableCartridge.MYSQL_51, true);
			selectedCartriges.add(IEmbeddableCartridge.MYSQL_51);
			selectedCartriges.add(cartridge);
		} else {
			viewer.setChecked(cartridge, false);
		}
		refreshViewer();
	}

	private SelectionListener onCheckAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(true);
				addJenkinsCartridge(IEmbeddableCartridge.JENKINS_14);
			}

		};
	}

	private SelectionListener onUncheckAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(false);
			}

		};
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setViewerInput(model.loadEmbeddableCartridges());
						return Status.OK_STATUS;
					} catch (Exception e) {
						clearViewer();
						return OpenShiftUIActivator.createErrorStatus("Could not load embeddable cartridges", e);
					}
				}

			}, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}
	}

	private void clearViewer() {
		setViewerInput(new ArrayList<IEmbeddableCartridge>());
	}

	private void setViewerInput(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(cartridges);
			}
		});
	}

	private void refreshViewer() {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.refresh(true, true);
			}
		});
	}

	private static class JenkinsApplicationDialog extends InputDialog {

		public JenkinsApplicationDialog(Shell shell) {
			super(shell
					, "New Jenkins application"
					, "To embed jenkins into your application, you'd first have to create a jenkins application."
					, null
					, new JenkinsNameValidator());
		}

		private static class JenkinsNameValidator implements IInputValidator {

			@Override
			public String isValid(String input) {
				if (StringUtils.isEmpty(input)) {
					return "You have to provide a name for the jenkins application";
				}
				return null;
			}
		}
	}

}