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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.JobScheduler;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

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
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(sshKeysGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(sshKeysGroup);

		Composite tableContainer = new Composite(sshKeysGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
			.to(BeanProperties.value(ManageSSHKeysWizardPageModel.PROPERTY_SELECTED_KEY).observe(pageModel))
			.in(dbc);

		Button addButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add Existing...");
		addButton.addSelectionListener(onAdd());

		Button newButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(newButton);
		newButton.setText("New...");

		Button removeButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.addSelectionListener(onRemove());

		Composite filler = new Composite(sshKeysGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		Button refreshButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.END).applyTo(refreshButton);
		refreshButton.setText("Refresh...");
		refreshButton.addSelectionListener(onRefresh());
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String keyName = pageModel.getSelectedSSHKey().getName();
				if (MessageDialog.openConfirm(getShell(),
						"Remove SSH Key",
						MessageFormat.format(
								"Are you sure that you want to remove public SSH key {0} from OpenShift?",
								keyName)))
					try {
						RemoveKeyJob removeSSHKeyJob = new RemoveKeyJob();
						new JobScheduler(removeSSHKeyJob).runWhenDone(new RefreshViewerJob());
						setViewerInput(pageModel.getSSHKeys());
						WizardUtils.runInWizard(removeSSHKeyJob, getContainer()) ;
					} catch (Exception ex) {
						StatusManager.getManager().handle(
								OpenShiftUIActivator.createErrorStatus("Could not remove key " + keyName + ".", ex), StatusManager.LOG);
					}
			}
		};
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardUtils.openWizardDialog(new AddSSHKeyWizard(pageModel.getUser()), getShell());
			}
		};
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IOpenShiftSSHKey>() {

					@Override
					public String getValue(IOpenShiftSSHKey key) {
						return key.getName();
					}
				})
				.name("Name").align(SWT.LEFT).weight(2).minWidth(200).buildColumn()
				.column(new IColumnLabelProvider<IOpenShiftSSHKey>() {

					@Override
					public String getValue(IOpenShiftSSHKey key) {
						return key.getKeyType().getTypeId();
					}
				})
				.name("Type").align(SWT.LEFT).weight(1).minWidth(50).buildColumn()
				.column(new IColumnLabelProvider<IOpenShiftSSHKey>() {

					@Override
					public String getValue(IOpenShiftSSHKey key) {
						return StringUtils.shorten(key.getPublicKey(), 24);
					}
				})
				.name("Type").align(SWT.LEFT).weight(4).minWidth(100).buildColumn()
				.buildViewer();

		return viewer;
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			Job loadKeysJob = new LoadKeysJob();
			new JobScheduler(loadKeysJob).runWhenDone(new RefreshViewerJob());
			WizardUtils.runInWizard(loadKeysJob, getContainer());
		} catch (Exception e) {
			StatusManager.getManager().handle(
					OpenShiftUIActivator.createErrorStatus("Could not load ssh keys.", e), StatusManager.LOG);
		}
	}

	private SelectionListener onRefresh() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					Job refreshKeysJob = new RefreshKeysJob();
					new JobScheduler(refreshKeysJob).runWhenDone(new RefreshViewerJob());
					WizardUtils.runInWizard(refreshKeysJob, getContainer());
				} catch (Exception ex) {
					StatusManager.getManager().handle(
							OpenShiftUIActivator.createErrorStatus("Could not refresh keys.", ex), StatusManager.LOG);
				}
			}

		};
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

	private class RemoveKeyJob extends Job {
	
		private RemoveKeyJob() {
			super("Removing SSH key " + pageModel.getSelectedSSHKey().getName() + "...");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			pageModel.removeKey();
			return Status.OK_STATUS;
		}
	}
	
	private class RefreshKeysJob extends Job {
		
		private RefreshKeysJob() {
			super("Refreshing SSH keys... ");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			pageModel.refresh();
			return Status.OK_STATUS;
		}
	}

	private class LoadKeysJob extends Job {
		
		private LoadKeysJob() {
			super("Loading SSH keys... ");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			pageModel.loadSSHKeys();
			return Status.OK_STATUS;
		}
	}

	private class RefreshViewerJob extends UIJob {

		public RefreshViewerJob() {
			super("Refreshing ssh keys...");
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			viewer.setInput(pageModel.getSSHKeys());
			return Status.OK_STATUS;
		}
	}

}

