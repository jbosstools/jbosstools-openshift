/******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 *****************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
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
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;

/**
 * @author Martes G Wigglesworth
 * @author Martin Rieman <mrieman@redhat.com>
 * @author Andre Dietisheim
 * 
 */
public class EnvironmentVariablesWizardPage extends AbstractOpenShiftWizardPage {

	private AbstractEnvironmentVariablesWizardModel model;
	private TableViewer viewer;

	public EnvironmentVariablesWizardPage(AbstractEnvironmentVariablesWizardModel model, IWizard wizard) {
		super(ExpressUIMessages.EnvironmentVariables, ExpressUIMessages.PleaseProvadeNewVariable, "", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		Group envVariableGroup = new Group(container, SWT.NONE);
		envVariableGroup.setText(ExpressUIMessages.EnvironmentVariables);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(envVariableGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(envVariableGroup);

		Composite tableContainer = new Composite(envVariableGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults().span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(AbstractEnvironmentVariablesWizardModel.PROPERTY_SELECTED).observe(model))
				.in(dbc);
		viewer.setComparator(new ViewerComparator());
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setInput(BeanProperties.list(AbstractEnvironmentVariablesWizardModel.PROPERTY_VARIABLES).observe(model));

		Button addButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText(ExpressUIMessages.Add);
		addButton.addSelectionListener(onAdd());

		Button editExistingButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText(ExpressUIMessages.Edit);
		editExistingButton.addSelectionListener(onEdit());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(editExistingButton)).notUpdatingParticipant()
				.to(BeanProperties.value(AbstractEnvironmentVariablesWizardModel.PROPERTY_SELECTED).observe(model))
				.converting(new IsNotNull2BooleanConverter()).in(dbc);

		Button removeButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText(ExpressUIMessages.Remove);
		removeButton.addSelectionListener(onRemove());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(removeButton)).notUpdatingParticipant()
				.to(BeanProperties.value(AbstractEnvironmentVariablesWizardModel.PROPERTY_SELECTED).observe(model))
				.converting(new IsNotNull2BooleanConverter()).in(dbc);

		Label filler = new Label(envVariableGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, true).applyTo(filler);

		Button refreshButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(refreshButton);
		refreshButton.setText(ExpressUIMessages.Refresh);
		refreshButton.addSelectionListener(onRefresh());

		// not supported
		enableEnvVariableGroup(model.isSupported(), envVariableGroup);
		Label validationLabel = new Label(envVariableGroup, SWT.NONE);
		validationLabel.setVisible(false);
		GridDataFactory.fillDefaults().exclude(true).applyTo(validationLabel);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(validationLabel))
				.notUpdating(
						BeanProperties.value(AbstractEnvironmentVariablesWizardModel.PROPERTY_SUPPORTED).observe(model))
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (Boolean.FALSE.equals((Boolean) value)) {
							return ValidationStatus
									.warning(NLS.bind(ExpressUIMessages.ServerDoesNotSupportChanging, model.getHost()));
						}
						return ValidationStatus.ok();
					}
				}).in(dbc);
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer).contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<EnvironmentVariableItem>() {

					@Override
					public String getValue(EnvironmentVariableItem variable) {
						return variable.getName();
					}
				}).name("Name").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.column(new IColumnLabelProvider<EnvironmentVariableItem>() {

					@Override
					public String getValue(EnvironmentVariableItem variable) {
						return variable.getValue();
					}
				}).name("Value").align(SWT.LEFT).weight(2).minWidth(100).buildColumn().buildViewer();

		return viewer;
	}

	private void enableEnvVariableGroup(final boolean enable, Group envVariableGroup) {
		envVariableGroup.setEnabled(enable);
		UIUtils.enableAllChildren(enable, envVariableGroup);
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new LoadEnvironmentVariablesJob(), getContainer(), dbc);
		} catch (InvocationTargetException e) {
			Logger.error(ExpressUIMessages.CouldNotLoadVariables, e);
		} catch (InterruptedException e) {
			Logger.error(ExpressUIMessages.CouldNotLoadVariables, e);
		}
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				EnvironmentVariableWizard wizard = new EnvironmentVariableWizard(model);
				new OkCancelButtonWizardDialog(getShell(), wizard).open();
			}
		};
	}

	private SelectionListener onEdit() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				OkCancelButtonWizardDialog editVariableWizardDialog = new OkCancelButtonWizardDialog(getShell(),
						new EnvironmentVariableWizard(model.getSelected(), model));
				editVariableWizardDialog.open();
				viewer.refresh();
			}
		};
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				EnvironmentVariableItem selectedVariable = model.getSelected();
				if (selectedVariable == null) {
					return;
				}
				if (MessageDialog.openConfirm(getShell(), ExpressUIMessages.RemoveVariable,
						NLS.bind(ExpressUIMessages.DoYouWantToRemoveVariable,
								selectedVariable.getName() + "=" + selectedVariable.getValue())))
					model.remove(selectedVariable);
			}
		};
	}

	private SelectionListener onRefresh() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					if (MessageDialog.openConfirm(getShell(), ExpressUIMessages.RefreshVariables,
							ExpressUIMessages.DoYouWantToRefreshVariables))
						WizardUtils.runInWizard(new RefreshEnvironmentVariablesJob(), getContainer(),
								getDatabindingContext());
				} catch (InvocationTargetException e) {
					Logger.error(ExpressUIMessages.CouldNotRefreshVariables, e);
				} catch (InterruptedException e) {
					Logger.error(ExpressUIMessages.CouldNotRefreshVariables, e);
				}
			}
		};
	}

	private class LoadEnvironmentVariablesJob extends AbstractDelegatingMonitorJob {

		public LoadEnvironmentVariablesJob() {
			super(ExpressUIMessages.LoadingVariables);
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			model.loadEnvironmentVariables();
			return Status.OK_STATUS;
		}
	}

	private class RefreshEnvironmentVariablesJob extends AbstractDelegatingMonitorJob {

		public RefreshEnvironmentVariablesJob() {
			super(ExpressUIMessages.RefreshingVariables);
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			model.refreshEnvironmentVariables();
			return Status.OK_STATUS;
		}
	}

}
