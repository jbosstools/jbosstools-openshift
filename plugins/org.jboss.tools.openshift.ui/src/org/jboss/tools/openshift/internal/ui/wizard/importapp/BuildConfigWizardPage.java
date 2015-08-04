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
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.BuildConfigTreeItems.Model2ObservableTreeItemConverter;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Andre Dietisheim
 */
public class BuildConfigWizardPage extends AbstractOpenShiftWizardPage {

	public IBuildConfigPageModel model;

	public BuildConfigWizardPage(IWizard wizard, IBuildConfigPageModel model) {
		super("Select Build Config", "Choose the build config that will be used to import a project to Eclipse", "", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridDataFactory.fillDefaults()
			.grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(parent);
		GridLayoutFactory.fillDefaults()
			.applyTo(parent);

		Group existingApplicationsGroup = new Group(parent, SWT.NONE);
		existingApplicationsGroup.setText("Existing Build Configs:");
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10,  10).applyTo(existingApplicationsGroup);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(existingApplicationsGroup);

		// build configs tree
		TreeViewer buildConfigsViewer = createBuildConfigsViewer(existingApplicationsGroup, dbc);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 200).span(1, 2).applyTo(buildConfigsViewer.getControl());
		final IObservableValue selectedItem = BeanProperties.value(IBuildConfigPageModel.PROPERTY_SELECTED_ITEM).observe(model);
		Binding selectedBuildConfigBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(buildConfigsViewer))
				.converting(new ObservableTreeItem2ModelConverter())
				.to(selectedItem)
				.converting(new Model2ObservableTreeItemConverter())
				.in(dbc);
		dbc.addValidationStatusProvider(new MultiValidator() {

			@Override
			protected IStatus validate() {
				if(!(selectedItem.getValue() instanceof IBuildConfig)) {
					return ValidationStatus.cancel("Please select the existing build config that you want to import");
				} else {
					return ValidationStatus.ok();
				}
			}
		});
		BeanProperties.value(IBuildConfigPageModel.PROPERTY_BUILDCONFIGS_TREEROOT).observe(model)
				.addValueChangeListener(onTreeRootChanged(buildConfigsViewer, model));
		
		ControlDecorationSupport.create(
				selectedBuildConfigBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		// refresh button
		Button refreshButton = new Button(existingApplicationsGroup, SWT.PUSH);
		refreshButton.setText("&Refresh");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).hint(100, SWT.DEFAULT).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefresh(buildConfigsViewer, model));
		
		// filler
		Label fillerLabel = new Label(existingApplicationsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, true).applyTo(fillerLabel);
	}

	private TreeViewer createBuildConfigsViewer(Composite parent, DataBindingContext dbc) {
		TreeViewer buildConfigsViewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] { 
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN)
						});
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		buildConfigsViewer.setContentProvider(contentProvider);
		buildConfigsViewer.setLabelProvider(new ObservableTreeItemStyledCellLabelProvider());
		buildConfigsViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		buildConfigsViewer.setComparator(new ViewerComparator());
		return buildConfigsViewer;
	}
	
	private SelectionListener onRefresh(final TreeViewer viewer, final IBuildConfigPageModel model) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setInput(model.getBuildConfigsTreeRoot());
				loadBuildConfigs(model);
				viewer.expandAll();
			}
		};
	}

	private IValueChangeListener onTreeRootChanged(final TreeViewer viewer, final IBuildConfigPageModel model) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				viewer.setInput(model.getBuildConfigsTreeRoot());
				loadBuildConfigs(model);
				viewer.expandAll();
			}
		};
	}

	private void loadBuildConfigs(final IBuildConfigPageModel model) {
		try {
			AbstractDelegatingMonitorJob job = new AbstractDelegatingMonitorJob("Loading build configs...") {

				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					model.loadBuildConfigs();
					return Status.OK_STATUS;
				}
			};
			WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer());
		} catch (InvocationTargetException | InterruptedException e) {
			// intentionnally swallowed
		}
	}
}
