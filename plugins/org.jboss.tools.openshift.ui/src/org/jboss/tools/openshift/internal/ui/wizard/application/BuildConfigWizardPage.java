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
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.NavigatorContentServiceFactory;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;

/**
 * @author Andre Dietisheim
 */
public class BuildConfigWizardPage extends AbstractOpenShiftWizardPage {

	private static final String COMMON_VIEWER_ID = "org.jboss.tools.openshift.ui.wizard.application.BuildConfigViewer";

	public IBuildConfigPageModel model;
	private TreeViewer buildConfigsViewer;

	public BuildConfigWizardPage(IWizard wizard, IBuildConfigPageModel model) {
		super("Select Build Config", "Choose the build config that will be used to import into Eclipse", "", wizard);
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
		this.buildConfigsViewer = createBuildConfigsViewer(existingApplicationsGroup, dbc);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 200).span(1, 2).applyTo(buildConfigsViewer.getControl());
		BeanProperties.value(IBuildConfigPageModel.PROPERTY_CONNECTION).observe(model)
				.addValueChangeListener(onConnectionChanged(buildConfigsViewer));
		final IObservableValue selectedItem = BeanProperties.value(IBuildConfigPageModel.PROPERTY_SELECTED_ITEM).observe(model);
		Binding selectedBuildConfigBinding = ValueBindingBuilder
				.bind(ViewerProperties.singlePostSelection().observe(buildConfigsViewer))
				.to(selectedItem).in(dbc);
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
		
		ControlDecorationSupport.create(selectedBuildConfigBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));
		
		// refresh button
		Button refreshButton = new Button(existingApplicationsGroup, SWT.PUSH);
		refreshButton.setText("&Refresh");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).hint(100, SWT.DEFAULT).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefresh());
		
		// filler
		Label fillerLabel = new Label(existingApplicationsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, true).applyTo(fillerLabel);
	}

	private SelectionListener onRefresh() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				buildConfigsViewer.setInput(model.getConnection());
			}
		};
	}

	private IValueChangeListener onConnectionChanged(final TreeViewer viewer) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				viewer.setInput(event.diff.getNewValue());
			}
		};
	}

	protected TreeViewer createBuildConfigsViewer(Composite parent, DataBindingContext dbc) {
		CommonViewer commonViewer = new CommonViewer(COMMON_VIEWER_ID, parent,
				SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		INavigatorContentService contentService = NavigatorContentServiceFactory.INSTANCE
				.createContentService(COMMON_VIEWER_ID, commonViewer);
		contentService.createCommonContentProvider();
		contentService.createCommonLabelProvider();
		commonViewer.setFilters(new ViewerFilter[] { new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				return element instanceof IProject 
						|| (element instanceof ResourceGrouping && ((ResourceGrouping) element).getKind() == ResourceKind.BUILD_CONFIG)
						|| element instanceof IBuildConfig;
			}
		} });
		return commonViewer;
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		buildConfigsViewer.setInput(model.getConnection());
	}
}
