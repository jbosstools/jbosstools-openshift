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
import org.eclipse.core.databinding.observable.value.WritableValue;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.server.BuildConfigDetailViews;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.BuildConfigTreeItems.Model2ObservableTreeItemConverter;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Andre Dietisheim
 * @author Jeff Maury
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

		Group buildConfigsGroup = new Group(parent, SWT.NONE);
		buildConfigsGroup.setText("Existing Build Configs:");
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10,  10).applyTo(buildConfigsGroup);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(buildConfigsGroup);

		// build configs tree
		TreeViewer buildConfigsViewer = createBuildConfigsViewer(new Tree(buildConfigsGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL), model, dbc);
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
		BeanProperties.value(IBuildConfigPageModel.PROPERTY_CONNECTION).observe(model)
				.addValueChangeListener(onConnectionChanged(buildConfigsViewer, model));
		
		ControlDecorationSupport.create(
				selectedBuildConfigBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		// refresh button
		Button refreshButton = new Button(buildConfigsGroup, SWT.PUSH);
		refreshButton.setText("&Refresh");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).hint(100, SWT.DEFAULT).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefresh(buildConfigsViewer, model));
		
		// filler
		Label fillerLabel = new Label(buildConfigsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, true).applyTo(fillerLabel);
		
	      // details
        ExpandableComposite expandable = new ExpandableComposite(buildConfigsGroup, SWT.None);
        GridDataFactory.fillDefaults()
            .span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
            .applyTo(expandable);
        expandable.setText("Build config Details");
        expandable.setExpanded(true);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(0, 0).applyTo(expandable);
        GridDataFactory.fillDefaults()
        .span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
        .applyTo(expandable);
	      Composite detailsContainer = new Composite(expandable, SWT.NONE);
	        GridDataFactory.fillDefaults()
	                .span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
	                .applyTo(detailsContainer);
	        IObservableValue selectedService = new WritableValue();
	        ValueBindingBuilder
	            .bind(selectedItem)
	            .to(selectedService)
	            .notUpdatingParticipant()
	            .in(dbc);
	        new BuildConfigDetailViews(selectedService, detailsContainer, dbc).createControls();
	        expandable.setClient(detailsContainer);
	        expandable.addExpansionListener(new IExpansionListener() {
	            @Override
	            public void expansionStateChanging(ExpansionEvent e) {
	            }
	            
	            @Override
	            public void expansionStateChanged(ExpansionEvent e) {
	                buildConfigsGroup.update();
	                buildConfigsGroup.layout(true);
	            }
	        });
		
		loadBuildConfigs(model);
	}

	private TreeViewer createBuildConfigsViewer(Tree tree, IBuildConfigPageModel model, DataBindingContext dbc) {
		TreeViewer buildConfigsViewer = new TreeViewer(tree);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] { 
						BeanProperties.list(IBuildConfigPageModel.PROPERTY_BUILDCONFIGS),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN)});
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		buildConfigsViewer.setContentProvider(contentProvider);
		buildConfigsViewer.setLabelProvider(new ObservableTreeItemStyledCellLabelProvider());
		buildConfigsViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		buildConfigsViewer.setComparator(new ViewerComparator());
		buildConfigsViewer.setInput(model);
		return buildConfigsViewer;
	}
	
	private SelectionListener onRefresh(final TreeViewer viewer, final IBuildConfigPageModel model) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				loadBuildConfigs(model);
				viewer.expandAll();
			}
		};
	}

	private IValueChangeListener onConnectionChanged(final TreeViewer viewer, final IBuildConfigPageModel model) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
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
