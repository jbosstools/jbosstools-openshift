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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.express.internal.ui.job.LoadKeysJob;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
class ManageSSHKeysWizardPage extends AbstractOpenShiftWizardPage {

	private SSHKeysWizardPageModel pageModel;
	private TableViewer viewer;

	ManageSSHKeysWizardPage(Connection connection, IWizard wizard) {
		this(OpenShiftExpressUIMessages.MANAGE_SSH_KEYS_WIZARD_PAGE,
				NLS.bind(OpenShiftExpressUIMessages.MANAGE_SSH_KEYS_WIZARD_PAGE_DESCRIPTION ,connection.getUsername()),
				"ManageSSHKeysPage", connection, wizard);
	}
	
	ManageSSHKeysWizardPage(String title, String description, String pageName, Connection connection, IWizard wizard) {
		super(title, description, pageName, wizard);
		this.pageModel = new SSHKeysWizardPageModel(connection);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group sshKeysGroup = new Group(parent, SWT.NONE);
		sshKeysGroup.setText(OpenShiftExpressUIMessages.SSH_PUBLIC_KEYS_GROUP);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(sshKeysGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(sshKeysGroup);

		Composite tableContainer = new Composite(sshKeysGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(SSHKeysWizardPageModel.PROPERTY_SELECTED_KEY).observe(pageModel))
				.in(dbc);

		Button addExistingButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addExistingButton);
		addExistingButton.setText(OpenShiftExpressUIMessages.ADD_EXISTING_BUTTON);
		addExistingButton.addSelectionListener(onAddExisting());

		Button addNewButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addNewButton);
		addNewButton.setText(OpenShiftExpressUIMessages.NEW_BUTTON);
		addNewButton.addSelectionListener(onAddNew());
		
		Button removeButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText(OpenShiftExpressUIMessages.REMOVE_BUTTON);
		removeButton.addSelectionListener(onRemove());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.to(ViewerProperties.singleSelection().observe(viewer))
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		
		Composite filler = new Composite(sshKeysGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		Button refreshButton = new Button(sshKeysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.END).applyTo(refreshButton);
		refreshButton.setText(OpenShiftExpressUIMessages.REFRESH_BUTTON);
		refreshButton.addSelectionListener(onRefresh());
		
		Link sshPrefsLink = new Link(parent, SWT.NONE);
		sshPrefsLink
				.setText(OpenShiftExpressUIMessages.SSH_PREFS_LINK);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String keyName = pageModel.getSelectedSSHKey().getName();
				if (MessageDialog.openConfirm(getShell(),
						OpenShiftExpressUIMessages.REMOVE_SSH_KEY_DIALOG_TITLE,
						NLS.bind(
								OpenShiftExpressUIMessages.REMOVE_SSH_KEY_QUESTION,
								keyName)))
					try {
						IStatus status = WizardUtils.runInWizard(
								new JobChainBuilder(
										new RemoveKeyJob()).andRunWhenDone(new RefreshViewerJob()).build()
								, getContainer(), getDatabindingContext() );
						if(status.equals(Status.ERROR)){
							setErrorMessage(status.getMessage());
						}else{
							setErrorMessage(null);
						}
					} catch (Exception ex) {
						setErrorMessage(ex.getMessage());
						StatusManager.getManager().handle(
								OpenShiftUIActivator.createErrorStatus(NLS.bind(OpenShiftExpressUIMessages.COULD_NOT_REMOVE_SSH_KEY, keyName), ex),
								StatusManager.LOG);
					}
			}
		};
	}

	private SelectionListener onAddExisting() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				AddSSHKeyWizard wizard = new AddSSHKeyWizard(pageModel.getConnection());
				if (WizardUtils.openWizardDialog(wizard, getShell()) == Dialog.CANCEL) {
					return;
				}

				try {
					WizardUtils.runInWizard(
							new RefreshViewerJob(), getContainer(), getDatabindingContext());
					pageModel.setSelectedSSHKey(wizard.getSSHKey());
				} catch (Exception ex) {
					setErrorMessage(ex.getMessage());
					StatusManager.getManager().handle(
							OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_REFRESH_VIEWER, ex), StatusManager.LOG);
				}
			}
		};
	}

	private SelectionListener onAddNew() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				NewSSHKeyWizard wizard = new NewSSHKeyWizard(pageModel.getConnection());
				if (WizardUtils.openWizardDialog(wizard, getShell()) == Dialog.CANCEL) {
					return;
				}

				try {
					WizardUtils.runInWizard(
							new RefreshViewerJob(),	getContainer(), getDatabindingContext());
					pageModel.setSelectedSSHKey(wizard.getSSHKey());
				} catch (Exception ex) {
					setErrorMessage(ex.getMessage());
					StatusManager.getManager().handle(
							OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_REFRESH_VIEWER, ex), StatusManager.LOG);
				}
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
				.name("Content").align(SWT.LEFT).weight(4).minWidth(100).buildColumn()
				.buildViewer();

		return viewer;
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			Job loadKeysJob = new LoadKeysJob(pageModel.getConnection());
			new JobChainBuilder(loadKeysJob).andRunWhenDone(new RefreshViewerJob());
			WizardUtils.runInWizard(loadKeysJob, getContainer());
		} catch (Exception e) {
			setErrorMessage(e.getMessage());
			StatusManager.getManager().handle(
					OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_LOAD_SSH_KEYS, e), StatusManager.LOG);
		}
	}

	private SelectionListener onRefresh() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					Job refreshKeysJob = new RefreshKeysJob();
					new JobChainBuilder(refreshKeysJob).andRunWhenDone(new RefreshViewerJob());
					IStatus status = WizardUtils.runInWizard(refreshKeysJob, getContainer(), getDatabindingContext());
					if(status.equals(Status.ERROR)){
						setErrorMessage(status.getMessage());
					}else{
						setErrorMessage(null);
					}
				} catch (Exception ex) {
					setErrorMessage(ex.getMessage());
					StatusManager.getManager().handle(
							OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_REFRESH_SSH_KEYS, ex), StatusManager.LOG);
				}
			}
		};
	}

	private SelectionAdapter onSshPrefs() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SSHUtils.openPreferencesPage(getShell());
			}
		};
	}

	protected SSHKeysWizardPageModel getPageModel() {
		return pageModel;
	}
	
	private class RemoveKeyJob extends Job {

		private RemoveKeyJob() {
			super(NLS.bind(OpenShiftExpressUIMessages.REMOVE_SSH_KEY_JOB, pageModel.getSelectedSSHKey().getName()));
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				pageModel.removeKey();
				return Status.OK_STATUS;
			}catch(OpenShiftException ex){
				return OpenShiftUIActivator.createErrorStatus(NLS.bind(OpenShiftExpressUIMessages.COULD_NOT_REMOVE_SSH_KEY, pageModel.getSelectedSSHKey().getName()), ex);
			}
		}
	}

	private class RefreshKeysJob extends Job {

		private RefreshKeysJob() {
			super(OpenShiftExpressUIMessages.REFRESH_SSH_KEYS_JOB);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				pageModel.refresh();
				return Status.OK_STATUS;
			}catch(OpenShiftException ex){
				return OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_REFRESH_SSH_KEYS, ex);
			}
		}
	}

	private class RefreshViewerJob extends UIJob {

		public RefreshViewerJob() {
			super(OpenShiftExpressUIMessages.REFRESH_VIEWER_JOB);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			try{
				IOpenShiftSSHKey key = pageModel.getSelectedSSHKey();
				viewer.setInput(pageModel.getSSHKeys());
				if (key != null) {
					viewer.setSelection(new StructuredSelection(key), true);
				}
				setErrorMessage(null);
				return Status.OK_STATUS;
			}catch(OpenShiftException ex){
				setErrorMessage(ex.getMessage());
				return OpenShiftUIActivator.createErrorStatus(OpenShiftExpressUIMessages.COULD_NOT_REFRESH_VIEWER, ex);
			}
		}
	}

}
