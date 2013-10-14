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
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.DestroyDomainJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.IDomain;

/**
 * @author André Dietisheim
 */
public class ManageDomainsWizardPage extends AbstractOpenShiftWizardPage {

	private ManageDomainsWizardPageModel pageModel;
	private TableViewer viewer;

	public ManageDomainsWizardPage(String title, String description, ManageDomainsWizardPageModel pageModel, IWizard wizard) {
		super(title, description, title, wizard);
		this.pageModel = pageModel;
	}

	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group domainsGroup = new Group(parent, SWT.NONE);
		domainsGroup.setText("Domains");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(domainsGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(domainsGroup);

		// domains table
		Composite tableContainer = new Composite(domainsGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setInput(BeanProperties.list(
				ManageDomainsWizardPageModel.PROPERTY_DOMAINS).observe(pageModel));
		loadDomains(dbc);
		IObservableValue viewerSingleSelection = ViewerProperties.singleSelection().observe(viewer);
		ValueBindingBuilder.bind(viewerSingleSelection)
				.to(BeanProperties.value(ManageDomainsWizardPageModel.PROPERTY_SELECTED_DOMAIN).observe(pageModel))
				.in(dbc);

		// new domain
		Button newButton = new Button(domainsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(newButton);
		newButton.setText("New...");
		newButton.addSelectionListener(onNew(dbc));

		// edit domain
		Button editButton = new Button(domainsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editButton);
		editButton.setText("Edit...");
		editButton.addSelectionListener(onEdit(dbc));
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editButton))
				.notUpdatingParticipant()
				.to(viewerSingleSelection)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);

		// remove
		Button removeButton = new Button(domainsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.addSelectionListener(onRemove(dbc));
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.notUpdatingParticipant()
				.to(viewerSingleSelection)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);

		Composite filler = new Composite(domainsGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		// refresh
		Button refreshButton = new Button(domainsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.END).applyTo(refreshButton);
		refreshButton.setText("Refresh...");
		refreshButton.addSelectionListener(onRefresh(dbc));
	}

	private void loadDomains(DataBindingContext dbc) {
		try {
			org.jboss.tools.common.ui.WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob("Loading domains...") {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.loadDomains();
							return Status.OK_STATUS;
						}
					}, new DelegatingProgressMonitor(), getContainer(), dbc);
		} catch (InvocationTargetException e) {
			Logger.error(NLS.bind("Could not load domains for connection {0}", pageModel.getConnection().getId()), e);
		} catch (InterruptedException e) {
			Logger.error(NLS.bind("Could not load domains for connection {0}", pageModel.getConnection().getId()), e);
		}
	}

	private SelectionListener onNew(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardUtils.openWizard(new NewDomainWizard(pageModel.getConnection()), getShell());
			}
		};
	}

	private SelectionListener onEdit(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardUtils.openWizard(new EditDomainWizard(pageModel.getSelectedDomain()), getShell());
			}
		};
	}

	private SelectionListener onRemove(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IDomain domain = pageModel.getSelectedDomain();
				if (domain == null) {
					return;
				}
				DestroyDomainDialog dialog = new DestroyDomainDialog(domain, getShell());
				dialog.open();
				if (dialog.isCancel()) {
					return;
				}

				AbstractDelegatingMonitorJob deleteDomainJob = new DestroyDomainJob(domain, dialog.isForceDelete());
				try {
					org.jboss.tools.common.ui.WizardUtils.runInWizard(
							deleteDomainJob, deleteDomainJob.getDelegatingProgressMonitor(), getContainer(), dbc);
				} catch (InvocationTargetException ex) {
					Logger.error(NLS.bind("Could not destroy domain {0}", domain.getId()), ex);
				} catch (InterruptedException ex) {
					Logger.error(NLS.bind("Could not destroy domain {0}", domain.getId()), ex);
				}
			}
		};
	}

	private SelectionListener onRefresh(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshModel(dbc);
			}
		};
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IDomain>() {

					@Override
					public String getValue(IDomain domain) {
						return domain.getId();
					}
				})
				.name("ID").align(SWT.LEFT).weight(1).minWidth(50).buildColumn()
				.column(new IColumnLabelProvider<IDomain>() {

					@Override
					public String getValue(IDomain domain) {
						return domain.getSuffix();
					}
				})
				.name("Suffix").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();

		return viewer;
	}

	private void refreshModel(final DataBindingContext dbc) {
		try {
			org.jboss.tools.common.ui.WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob("Refreshing domains...") {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.refresh();
							return Status.OK_STATUS;
						}
					}
					, new DelegatingProgressMonitor(), getContainer(), dbc);
		} catch (InvocationTargetException ex) {
			Logger.error(NLS.bind("Could not refresh connection {0}", pageModel.getConnection().getId()), ex);
		} catch (InterruptedException ex) {
			Logger.error(NLS.bind("Could not refresh connection {0}", pageModel.getConnection().getId()), ex);
		}
	}

	public IDomain getSelectedDomain() {
		return pageModel.getSelectedDomain();
	}

	@Override
	public void dispose() {
		pageModel.dispose();
		super.dispose();
	}
	
	
}
