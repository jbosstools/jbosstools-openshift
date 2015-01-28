/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.console;

import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
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
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.IGearGroup;
import com.openshift.client.NotFoundOpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class TailFilesWizardPage extends AbstractOpenShiftWizardPage {

	private final TailFilesWizardPageModel pageModel;
	private CheckboxTableViewer viewer;

	public TailFilesWizardPage(final TailFilesWizardPageModel pageModel, final IWizard wizard) {
		super("Tail Log Files", "This will run tail on your OpenShift application '" + pageModel.getApplication().getName() +
				"'.\nYou can use the defaults or change the tail options.",
				"TailFilePage", wizard);
		this.pageModel = pageModel;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(parent);
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		// label
		final Label filePatternLabel = new Label(container, SWT.NONE);
		filePatternLabel.setText("Tail options:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false)
				.applyTo(filePatternLabel);
		// input text field
		final Text filePatternText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(filePatternText);
		final IObservableValue filePatternTextObservable = WidgetProperties.text(SWT.Modify).observe(filePatternText);
		final IObservableValue filePatternModelObservable = BeanProperties.value(
				TailFilesWizardPageModel.PROPERTY_FILE_PATTERN).observe(pageModel);
		ValueBindingBuilder.bind(filePatternTextObservable).to(filePatternModelObservable).in(dbc);
		// reset button (in case user inputs something and wants/needs to revert)
		final Button resetButton = new Button(container, SWT.PUSH);
		resetButton.setText("Reset");
		GridDataFactory.fillDefaults()
				.hint(100, SWT.DEFAULT).span(1, 1).align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(resetButton);
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.resetFilePattern();
			}
		});
		
		// gears selection container
		final Composite gearsSelectionContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).span(3, 1)
			.applyTo(gearsSelectionContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(gearsSelectionContainer);
		// enable tail logs on all gears at the same time
		final Label selectGearsLabel = new Label(gearsSelectionContainer, SWT.NONE);
		selectGearsLabel.setText("Please, select the gears on which you want to tail files:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(selectGearsLabel);
		
		final Composite tableContainer = new Composite(gearsSelectionContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 2)
			.applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(IGearGroup.class).observe(viewer),
				BeanProperties.set(
						TailFilesWizardPageModel.PROPERTY_SELECTED_GEAR_GROUPS)
						.observe(pageModel));
		final Button selectAllButton = new Button(gearsSelectionContainer, SWT.PUSH);
		selectAllButton.setText("Select all");
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.selectAllGears(); 
			}
		});
		GridDataFactory.fillDefaults()
				.hint(100, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(selectAllButton);
		final Button deselectAllButton = new Button(gearsSelectionContainer, SWT.PUSH);
		deselectAllButton.setText("Deselect all");
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.deselectAllGears();
			}
		});

		GridDataFactory.fillDefaults()
				.hint(100, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).grab(false, false).applyTo(deselectAllButton);		

	}
	
	private CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		createTableColumn("Cartridges", 5, SWT.LEFT, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final IGearGroup gearGroup = (IGearGroup) cell.getElement();
				final String cartridgeNames = GearGroupsUtils.getCartridgeDisplayNames(gearGroup);
				cell.setText(cartridgeNames);
			}
		}, viewer, tableLayout);
		createTableColumn("Number of Gears", 3, SWT.RIGHT, new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final IGearGroup gearGroup = (IGearGroup) cell.getElement();
				cell.setText(Integer.toString(gearGroup.getGears().size()));
			}
		}, viewer, tableLayout);
		createTableColumn("", 1, SWT.RIGHT, new CellLabelProvider(){
			@Override
			public void update(ViewerCell cell) {
			}
		}, viewer, tableLayout);
		return viewer;
	}
	
	private void createTableColumn(String name, int weight, int alignment, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.getColumn().setAlignment(alignment);
		if(cellLabelProvider != null) {
			column.setLabelProvider(cellLabelProvider);
		}
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	// private SelectionListener onCheckAll() {
	// return new SelectionAdapter() {
	//
	// @Override
	// public void widgetSelected(SelectionEvent e) {
	// viewer.setAllChecked(true);
	// try {
	// addJenkinsCartridge(IEmbeddedCartridge.JENKINS_14);
	// } catch (OpenShiftException ex) {
	// ExpressUIActivator.log("Could not select jenkins cartridge", ex);
	// } catch (SocketTimeoutException ex) {
	// ExpressUIActivator.log("Could not select jenkins cartridge", ex);
	// }
	// }
	//
	// };
	// }

	// private SelectionListener onUncheckAll() {
	// return new SelectionAdapter() {
	//
	// @Override
	// public void widgetSelected(SelectionEvent e) {
	// viewer.setAllChecked(false);
	// }
	//
	// };
	// }

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			loadApplicationGearGroups(dbc);
		} catch (Exception e) {
			Logger.error("Could not reset File Pattern text field", e);
		}
	}

	private void loadApplicationGearGroups(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading gear groups for application '" + pageModel.getApplication().getName() + "'...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						pageModel.loadGearGroups();
						setViewerInput(pageModel.getGearGroups());
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return ExpressUIActivator.createErrorStatus(
								"Could not load application's gear list", e);
					}
				}
			}, getContainer(), dbc);
		} catch (Exception ex) {
			// ignore
		}
	}
	
	private void setViewerInput(final Collection<IGearGroup> gearGroups) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (viewer != null) {
					viewer.setInput(gearGroups);
				}
			}
		});
	}

}