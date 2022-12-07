/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.Starter;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.SelectProjectComponentBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentWizardPage extends AbstractOpenShiftWizardPage {

	private static class ComponentTypeValidator extends IsNotNullValidator {

		/**
		 * @param invalidStatus
		 */
		public ComponentTypeValidator(IStatus invalidStatus) {
			super(invalidStatus);
		}

		@Override
		public IStatus validate(Object value) {
			IStatus status = super.validate(value);
			if (status.isOK() && value instanceof String) {
				status = super.validate(null);
			}
			return status;
		}
	}

	private CreateComponentModel model;

	private static final Image INFORMATION_IMAGE = WorkbenchPlugin.getDefault().getSharedImages()
			.getImage(ISharedImages.IMG_OBJS_INFO_TSK);

	protected CreateComponentWizardPage(IWizard wizard, CreateComponentModel model) {
		super("Create component", "Specify component parameters.", "Create component", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);

		Label componentNameLabel = new Label(parent, SWT.NONE);
		componentNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(componentNameLabel);
		Text componentNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(componentNameText);

		ISWTObservableValue<String> componentNameObservable = WidgetProperties.text(SWT.Modify)
				.observe(componentNameText);
		Binding componentNameBinding = ValueBindingBuilder.bind(componentNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(ComponentModel.PROPERTY_COMPONENT_NAME).observe(model)).in(dbc);
		ControlDecorationSupport.create(componentNameBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));

		IObservableValue<Object> projectObservable = BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT)
				.observe(model);
		SelectProjectComponentBuilder builder = new SelectProjectComponentBuilder();
		builder.setTextLabel("Use existing workspace project:").setRequired(true)
				.setEclipseProjectObservable(projectObservable)
				.setSelectionListener(SelectionListener.widgetSelectedAdapter(this::onBrowseProjects))
				.setButtonIndent(0).build(parent, dbc, 1);

		CLabel information = new CLabel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(information);
		ValueBindingBuilder.bind(WidgetProperties.text().observe(information)).notUpdatingParticipant()
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
				.converting(IConverter.create(
						flag -> (boolean) flag ? "Project has a devfile, component type selection is not required"
								: ""))
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.image().observe(information)).notUpdatingParticipant()
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
				.converting(IConverter.create(flag -> (boolean) flag ? INFORMATION_IMAGE : null)).in(dbc);

		Label componentTypesLabel = new Label(parent, SWT.NONE);
		componentTypesLabel.setText("Component type:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(componentTypesLabel);
		org.eclipse.swt.widgets.List componentTypesList = new org.eclipse.swt.widgets.List(parent,
				SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, 150)
				.applyTo(componentTypesList);
		ListViewer componentTypesListViewer = new ListViewer(componentTypesList);
		componentTypesListViewer.setContentProvider(ArrayContentProvider.getInstance());
		componentTypesListViewer.setLabelProvider(new ComponentTypeColumLabelProvider());
		componentTypesListViewer.setInput(model.getComponentTypes());
		Binding componentTypesBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(componentTypesListViewer))
				.validatingAfterGet(
						new ComponentTypeValidator(ValidationStatus.cancel("You have to select a component type.")))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_TYPE, ComponentType.class)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(componentTypesBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(componentTypesList))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
				.converting(new InvertingBooleanConverter()).in(dbc);

		Label componentStartersLabel = new Label(parent, SWT.NONE);
		componentStartersLabel.setText("Project starter:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(componentStartersLabel);
		Combo componentStartersVersionsCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(componentStartersVersionsCombo);
		ComboViewer componentStartersComboViewer = new ComboViewer(componentStartersVersionsCombo);
		componentStartersComboViewer.setContentProvider(new ObservableListContentProvider<>());
		componentStartersComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Starter) {
					return ((Starter) element).getName();
				}
				return "";
			}
		});
		componentStartersComboViewer.setInput(
				BeanProperties.list(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTERS).observe(model));
		ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(componentStartersComboViewer))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTER).observe(model))
				.in(dbc);
		IObservableValue<List> selectedStartersObservable = BeanProperties
				.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTERS, List.class).observe(model);
		IObservableValue<Boolean> emptyProjectObservable = BeanProperties
				.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_EMPTY, Boolean.class).observe(model);
		IObservableValue<Boolean> computedObservable = ComputedValue.create(() -> {
			return !selectedStartersObservable.getValue().isEmpty() && emptyProjectObservable.getValue();
		});
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(componentStartersVersionsCombo))
				.to(computedObservable).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.text().observe(information)).notUpdatingParticipant()
				.to(computedObservable)
				.converting(IConverter.create(flag -> (boolean) flag
						? "Your project is empty, you can initialize it from starters (templates)"
						: ""))
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.image().observe(information)).notUpdatingParticipant()
				.to(computedObservable).converting(IConverter.create(flag -> (boolean) flag ? INFORMATION_IMAGE : null))
				.in(dbc);
/*
		Label startDevAfterCreateLabel = new Label(parent, SWT.NONE);
		startDevAfterCreateLabel.setText("Start dev mode:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(startDevAfterCreateLabel);
		Button startDevAfterCreateButton = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(startDevAfterCreateButton);

		ISWTObservableValue<Boolean> startDevAfterCreateObservable = WidgetProperties.buttonSelection()
				.observe(startDevAfterCreateButton);
		Binding startDevAfterCreateBinding = ValueBindingBuilder.bind(startDevAfterCreateObservable)
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_DEVMODE_AFTER_CREATE).observe(model)).in(dbc);
		ControlDecorationSupport.create(startDevAfterCreateBinding, SWT.LEFT | SWT.TOP); */
	}

	private void onBrowseProjects(SelectionEvent e) {
		SelectExistingProjectDialog dialog = new SelectExistingProjectDialog("Select an Eclipse project", getShell());
		dialog.setInitialSelections(model.getEclipseProject());
		if (dialog.open() == Window.OK) {
			model.setEclipseProject(dialog.getSelectedProject());
		}
	}
}
