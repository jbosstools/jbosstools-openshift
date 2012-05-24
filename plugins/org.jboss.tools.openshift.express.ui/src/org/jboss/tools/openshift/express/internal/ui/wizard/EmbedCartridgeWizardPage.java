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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
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
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.EmbedCartridgesJob;

import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;

/**
 * @author André Dietisheim
 */
public class EmbedCartridgeWizardPage extends AbstractOpenShiftWizardPage {

	private static final long EMBED_CARTRIDGES_TIMEOUT = 2 * 60 * 1000;
	private EmbedCartridgeWizardPageModel pageModel;
	private CheckboxTableViewer viewer;

	public EmbedCartridgeWizardPage(ApplicationWizardModel wizardModel, IWizard wizard) {
		super("Embed Cartridges", "Please select the cartridges to embed into your application",
				"EmbedCartridgePage", wizard);
		this.pageModel = new EmbedCartridgeWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group embedGroup = new Group(parent, SWT.NONE);
		embedGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults()
				.hint(200, 300).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(embedGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(3).margins(6, 6).applyTo(embedGroup);

		Composite tableContainer = new Composite(embedGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(IEmbeddableCartridge.class).observe(viewer),
				BeanProperties.set(
						EmbedCartridgeWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES)
						.observe(pageModel));
		;
		// strategy has to be attached after the binding, so that the binding
		// can still add the checked cartridge and the strategy can correct
		viewer.addCheckStateListener(new EmbedCartridgeStrategyAdapter(pageModel, this));

		// hiding buttons for now: https://issues.jboss.org/browse/JBIDE-10399
		// Button checkAllButton = new Button(embedGroup, SWT.PUSH);
		// checkAllButton.setText("Embed A&ll");
		// GridDataFactory.fillDefaults()
		// .hint(110, SWT.DEFAULT).align(SWT.FILL,
		// SWT.CENTER).applyTo(checkAllButton);
		// checkAllButton.addSelectionListener(onCheckAll());

		// Button uncheckAllButton = new Button(embedGroup, SWT.PUSH);
		// uncheckAllButton.setText("Embed N&one");
		// GridDataFactory.fillDefaults()
		// .hint(110, SWT.DEFAULT).align(SWT.FILL,
		// SWT.CENTER).applyTo(uncheckAllButton);
		// uncheckAllButton.addSelectionListener(onUncheckAll());
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());

		viewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IEmbeddableCartridge && e2 instanceof IEmbeddableCartridge) {
					return ((IEmbeddableCartridge) e1).getName().compareTo(((IEmbeddableCartridge) e2).getName());
				}
				return super.compare(viewer, e1, e2);
			}

		});

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

	// private SelectionListener onCheckAll() {
	// return new SelectionAdapter() {
	//
	// @Override
	// public void widgetSelected(SelectionEvent e) {
	// viewer.setAllChecked(true);
	// try {
	// addJenkinsCartridge(IEmbeddedCartridge.JENKINS_14);
	// } catch (OpenShiftException ex) {
	// OpenShiftUIActivator.log("Could not select jenkins cartridge", ex);
	// } catch (SocketTimeoutException ex) {
	// OpenShiftUIActivator.log("Could not select jenkins cartridge", ex);
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
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setViewerInput(pageModel.loadEmbeddableCartridges());
						setViewerCheckedElements(pageModel.getSelectedEmbeddableCartridges());
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

	private void setViewerCheckedElements(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setCheckedElements(cartridges.toArray());
			}
		});
	}

	private void safeRefreshSelectedEmbeddedCartridges() {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					Set<IEmbeddableCartridge> selectedEmbeddableCartridges = pageModel
							.refreshSelectedEmbeddedCartridges();
					viewer.setCheckedElements(
							selectedEmbeddableCartridges.toArray());
				} catch (Exception e) {
					OpenShiftUIActivator.log(e);
				}
			}
		});
	}

	private void setViewerInput(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(cartridges);
			}
		});
	}

	public boolean processCartridges() {
		try {
			EmbedCartridgesJob job = new EmbedCartridgesJob(
					new ArrayList<IEmbeddableCartridge>(pageModel.getSelectedEmbeddableCartridges()), 
					pageModel.getApplication());
			IStatus result = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer(), EMBED_CARTRIDGES_TIMEOUT);
			if (!result.isOK()) {
				safeRefreshSelectedEmbeddedCartridges();
			} else {
				openLogDialog(job.getAddedCartridges());
			}
			return result.isOK();
		} catch (Exception e) {
			return false;
		}
	}

	private void openLogDialog(final List<IEmbeddedCartridge> cartridges) {
		if (cartridges.size() == 0) {
			return;
		}

		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), cartridges).open();

			}
		});
	}

	/**
	 * Viewer element comparer based on #equals(). The default implementation in
	 * CheckboxTableViewer compares elements based on instance identity.
	 * <p>
	 * We need this since the available cartridges (item listed in the viewer)
	 * are not the same instance as the ones in the embedded application (items
	 * to check in the viewer).
	 */
	private static class EqualityComparer implements IElementComparer {

		@Override
		public boolean equals(Object thisObject, Object thatObject) {
			if (thisObject == null) {
				return thatObject != null;
			}

			if (thatObject == null) {
				return false;
			}

			return thisObject.equals(thatObject);
		}

		@Override
		public int hashCode(Object element) {
			return element.hashCode();
		}

	}
}