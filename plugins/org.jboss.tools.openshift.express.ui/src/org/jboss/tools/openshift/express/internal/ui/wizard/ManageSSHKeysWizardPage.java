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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IOpenShiftSSHKey;

/**
 * @author André Dietisheim
 */
public class ManageSSHKeysWizardPage extends AbstractOpenShiftWizardPage {

	private ManageSSHKeysWizardPageModel pageModel;
	private TableViewer viewer;

	public ManageSSHKeysWizardPage(UserDelegate user, IWizard wizard) {
		super("Manage SSH Keys", "Manage the SSH keys that are available to your OpenShift account",
				"ManageSSHKeysPage", wizard);
		this.pageModel = new ManageSSHKeysWizardPageModel(user);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group sshKeysGroup = new Group(parent, SWT.NONE);
		sshKeysGroup.setText("SSH Public Keys");
		GridDataFactory.fillDefaults()
				.hint(200, 300).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(sshKeysGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(sshKeysGroup);

		Composite tableContainer = new Composite(sshKeysGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 4).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ViewerSupport.bind(
				viewer,
				BeanProperties.list(ManageSSHKeysWizardPageModel.PROPERTY_SSH_KEYS).observe(pageModel), 
				BeanProperties.values(new String[]{"name", "keyType", "publicKey"}));

		Button addButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add Existing...");

		Button newButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(newButton);
		newButton.setText("New...");

		Button removeButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		TableViewer viewer = new TableViewer(table);
		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());

		viewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object thisKey, Object thatKey) {
				if (thisKey instanceof IOpenShiftSSHKey 
						&& thatKey instanceof IOpenShiftSSHKey) {
					return ((IOpenShiftSSHKey) thisKey).getName().compareTo(((IEmbeddableCartridge) thatKey).getName());
				}
				return super.compare(viewer, thisKey, thatKey);
			}

		});

		createTableColumn("Name", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IOpenShiftSSHKey key = (IOpenShiftSSHKey) cell.getElement();
				cell.setText(key.getName());
			}
		}, viewer, tableLayout);
		createTableColumn("Type", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IOpenShiftSSHKey key = (IOpenShiftSSHKey) cell.getElement();
				cell.setText(key.getName());
			}
		}, viewer, tableLayout);
		createTableColumn("Public Key", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IOpenShiftSSHKey key = (IOpenShiftSSHKey) cell.getElement();
				cell.setText(key.getPublicKey());
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

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading ssh keys...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
//						setViewerInput(pageModel.loadSSHKeys());
						pageModel.loadSSHKeys();
						return Status.OK_STATUS;
					} catch (Exception e) {
						clearViewer();
						return OpenShiftUIActivator.createErrorStatus("Could not load ssh keys.", e);
					}
				}

			}, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}
	}

	private void clearViewer() {
		setViewerInput(new ArrayList<IOpenShiftSSHKey>());
	}

	private void setViewerInput(final Collection<IOpenShiftSSHKey> keys) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(keys);
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