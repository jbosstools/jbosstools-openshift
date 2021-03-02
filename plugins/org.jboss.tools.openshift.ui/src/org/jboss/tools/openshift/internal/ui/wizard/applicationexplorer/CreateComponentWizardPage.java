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

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.S2iComponentType;
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
  
  private class ComponentVersionValidator extends IsNotNullValidator {

    /**
     * @param invalidStatus
     */
    public ComponentVersionValidator(IStatus invalidStatus) {
      super(invalidStatus);
    }

    @Override
    public IStatus validate(Object value) {
      if (model.getSelectedComponentType().getKind() == ComponentKind.S2I) {
        return super.validate(value);
      }
      return ValidationStatus.ok();
    }
  }

	private CreateComponentModel model;
	
	private static final Image INFORMATION_IMAGE = WorkbenchPlugin.getDefault().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

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

		ISWTObservableValue<String> componentNameObservable = WidgetProperties.text(SWT.Modify).observe(componentNameText);
		Binding componentNameBinding = ValueBindingBuilder.bind(componentNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(ComponentModel.PROPERTY_COMPONENT_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(componentNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		IObservableValue<Object> projectObservable = BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT).observe(model);
		SelectProjectComponentBuilder builder = new SelectProjectComponentBuilder();
		builder.setTextLabel("Use existing workspace project:").setRequired(true)
				.setEclipseProjectObservable(projectObservable).setSelectionListener(SelectionListener.widgetSelectedAdapter(this::onBrowseProjects))
				.setButtonIndent(0).build(parent, dbc, 1);

		CLabel information = new CLabel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(information);
		ValueBindingBuilder.bind(WidgetProperties.text().observe(information))
				.notUpdatingParticipant()
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
				.converting(IConverter.create(flag -> (boolean) flag?"Project has a devfile, component type selection is not required":""))
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.image().observe(information))
				.notUpdatingParticipant()
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
				.converting(IConverter.create(flag -> (boolean) flag?INFORMATION_IMAGE:null))
				.in(dbc);
		
		Label componentTypesLabel = new Label(parent, SWT.NONE);
		componentTypesLabel.setText("Component type:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(componentTypesLabel);
		Tree componentTypesTree = new Tree(parent, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
    .applyTo(componentTypesTree);
		TreeViewer componentTypesTreeViewer = new TreeViewer(componentTypesTree);
    componentTypesTreeViewer.setContentProvider(new ComponentTypeContentProvider(model.getComponentTypes()));
		componentTypesTreeViewer.setLabelProvider(new ComponentTypeColumLabelProvider());
		componentTypesTreeViewer.setInput(model.getComponentTypes());
    Binding componentTypesBinding = ValueBindingBuilder
        .bind(ViewerProperties.singleSelection().observe(componentTypesTreeViewer))
        .validatingAfterGet(new ComponentTypeValidator(
            ValidationStatus.cancel("You have to select a component type.")))
        .to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_TYPE, ComponentType.class)
            .observe(model))
        .in(dbc);
    ControlDecorationSupport.create(componentTypesBinding, SWT.LEFT | SWT.TOP, null,
        new RequiredControlDecorationUpdater());
    ValueBindingBuilder.bind(WidgetProperties.enabled().observe(componentTypesTree))
        .to(BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE).observe(model))
        .converting(new InvertingBooleanConverter())
        .in(dbc);

		IObservableValue<ComponentType> componentTypeObservable = BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_TYPE, ComponentType.class).observe(model);
		Label componentVersionsLabel = new Label(parent, SWT.NONE);
		componentVersionsLabel.setText("Component version:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(componentVersionsLabel);
		Combo componentVersionsCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(componentVersionsCombo);
		ComboViewer componentVersionsComboViewer = new ComboViewer(componentVersionsCombo);
		componentVersionsComboViewer.setContentProvider(new ObservableListContentProvider<>());
		componentVersionsComboViewer.setInput(PojoProperties.list("versions").observeDetail(componentTypeObservable));
		Binding componentVersionsBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(componentVersionsComboViewer))
				.validatingAfterGet(new ComponentVersionValidator(
						ValidationStatus.cancel("You have to select a component version.")))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_VERSION).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(componentVersionsBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(componentVersionsCombo))
		    .to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_TYPE).observe(model))
		    .converting(IConverter.create(type -> type instanceof S2iComponentType))
		    .in(dbc);
		
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
		componentStartersComboViewer.setInput(BeanProperties.list(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTERS).observe(model));
		Binding componentStartersBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(componentStartersComboViewer))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTER).observe(model))
				.in(dbc);
		IObservableValue<List> selectedStartersObservable = BeanProperties.value(CreateComponentModel.PROPERTY_SELECTED_COMPONENT_STARTERS, List.class).observe(model);
		IObservableValue<Boolean> emptyProjectObservable = BeanProperties.value(CreateComponentModel.PROPERTY_ECLIPSE_PROJECT_EMPTY, Boolean.class).observe(model);
		IObservableValue<Boolean> computedObservable = ComputedValue.create(() -> {
			return !selectedStartersObservable.getValue().isEmpty() && emptyProjectObservable.getValue();
		});
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(componentStartersVersionsCombo))
				.to(computedObservable)
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.text().observe(information))
				.notUpdatingParticipant()
				.to(computedObservable)
				.converting(IConverter.create(flag -> (boolean) flag?"Your project is empty, you can initialize it from starters (templates)":""))
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.image().observe(information))
				.notUpdatingParticipant()
				.to(computedObservable)
				.converting(IConverter.create(flag -> (boolean) flag?INFORMATION_IMAGE:null))
				.in(dbc);

		Label applicationLabel = new Label(parent, SWT.NONE);
		applicationLabel.setText("Application:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(applicationLabel);
		Text applicationNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(applicationNameText);

		ISWTObservableValue<String> applicationNameObservable = WidgetProperties.text(SWT.Modify).observe(applicationNameText);
		Binding applicationNameBinding = ValueBindingBuilder.bind(applicationNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify an application"))
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_APPLICATION_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(applicationNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		if (StringUtils.isNotBlank(model.getApplicationName())) {
			applicationNameText.setEnabled(false);
		}

		Label pushAfterCreateLabel = new Label(parent, SWT.NONE);
		pushAfterCreateLabel.setText("Push after create:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(pushAfterCreateLabel);
		Button pushAfterCreateButton = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(pushAfterCreateButton);

		ISWTObservableValue<Boolean> pushAfterCreateObservable = WidgetProperties.buttonSelection().observe(pushAfterCreateButton);
		Binding pushAfterCreateBinding = ValueBindingBuilder.bind(pushAfterCreateObservable)
				.to(BeanProperties.value(CreateComponentModel.PROPERTY_PUSH_AFTER_CREATE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(pushAfterCreateBinding, SWT.LEFT | SWT.TOP);
}
	
	private void onBrowseProjects(SelectionEvent e) {
		SelectExistingProjectDialog dialog = new SelectExistingProjectDialog("Select an Eclipse project", getShell());
		dialog.setInitialSelections(model.getEclipseProject());
		if (dialog.open() == Dialog.OK) {
			model.setEclipseProject(dialog.getSelectedProject());
		}
	}
}
