/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.ICellToolTipProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.webhooks.WebHooksDialog;
import org.jboss.tools.openshift.internal.ui.webhooks.WebhookUtil;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.TemplateParameterViewerUtils.ParameterNameViewerComparator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;

/**
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class NewApplicationSummaryDialog extends ResourceSummaryDialog {

	private CreateApplicationFromTemplateJob job;

	public NewApplicationSummaryDialog(Shell parentShell, CreateApplicationFromTemplateJob job, String message) {
		super(parentShell, job.getResources(),  "Create Application Summary", message,  new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider());
		this.job = job;
	}

	@Override
	protected void createAreaAfterResourceSummary(Composite parent) {
		
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(area);
		
		final Collection<IBuildConfig> buildConfigs = findBuildConfigsWithWebHooks();
		if (!buildConfigs.isEmpty()) {
			Link webHooksLink = new Link(area, SWT.NONE);
			webHooksLink.setText("Click <a>here</a> to display the webhooks available to automatically trigger builds.");
			GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(webHooksLink);
			webHooksLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WebHooksDialog dialog = new WebHooksDialog(getParentShell(), buildConfigs);
					dialog.open();
				}
			});
		}

		if(job.getParameters().isEmpty()) {
			return;
		}

		Label lblParams = new Label(area, SWT.NONE);
		lblParams.setText("Please make note of the following parameters which may \ninclude values required to administer your resources:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(lblParams);
		
		Composite parameters = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults()
			.hint(100, 250)
			.grab(true, true)
			.applyTo(parameters);
		
		TableViewer viewer = createTable(parameters);
		viewer.setInput(job.getParameters());
	}
	
	private Collection<IBuildConfig> findBuildConfigsWithWebHooks() {
		Set<IBuildConfig> buildConfigs = new LinkedHashSet<>();
		for (IResource r : job.getResources()) {
			if (r instanceof IBuildConfig && !WebhookUtil.getWebHooks((IBuildConfig)r).isEmpty()) {
				buildConfigs.add((IBuildConfig)r);
			}
		}
		return buildConfigs;
	}

	public static TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		ICellToolTipProvider<IParameter> cellToolTipProvider = new ICellToolTipProvider<IParameter>() {

			@Override
			public String getToolTipText(IParameter object) {
				return object.getDescription();
			}

			@Override
			public int getToolTipDisplayDelayTime(IParameter object) {
				return 0;
			}
		};
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IParameter>() {

					@Override
					public String getValue(IParameter variable) {
						return variable.getName();
					}})
					.cellToolTipProvider(cellToolTipProvider)
					.name("Name")
					.align(SWT.LEFT).weight(2).minWidth(100)
					.buildColumn()
				.column(new IColumnLabelProvider<IParameter>() {

					@Override
					public String getValue(IParameter parameter) {
						return TemplateParameterViewerUtils.getValueLabel(parameter);
					}})
					.cellToolTipProvider(cellToolTipProvider)
					.name("Value")
					.align(SWT.LEFT).weight(2).minWidth(100)
					.buildColumn()
				.buildViewer();
		viewer.setComparator(new ParameterNameViewerComparator());
		return viewer;
	}
}
