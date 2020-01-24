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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Red Hat Developers
 *
 */
public class CreateStorageWizardPage extends AbstractOpenShiftWizardPage {

	private CreateStorageModel model;

	protected CreateStorageWizardPage(IWizard wizard, CreateStorageModel model) {
		super("Create storage", "Specify a name and mount point for the storage and choose a size.", "Create storage", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label storageNameLabel = new Label(parent, SWT.NONE);
		storageNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(storageNameLabel);
		Text storageNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(storageNameText);

		ISWTObservableValue<String> storageNameObservable = WidgetProperties.text(SWT.Modify).observe(storageNameText);
		Binding storageNameBinding = ValueBindingBuilder.bind(storageNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(CreateStorageModel.PROPERTY_STORAGE_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(storageNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Label mountPathLabel = new Label(parent, SWT.NONE);
		mountPathLabel.setText("Mount path:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(mountPathLabel);
		Text mountPathText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(mountPathText);

		ISWTObservableValue<String> mountPathObservable = WidgetProperties.text(SWT.Modify).observe(mountPathText);
		Binding mountPathBinding = ValueBindingBuilder.bind(mountPathObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a mount path"))
				.to(BeanProperties.value(CreateStorageModel.PROPERTY_MOUNT_PATH).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(mountPathBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Label sizeLabel = new Label(parent, SWT.NONE);
		sizeLabel.setText("Size:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(sizeLabel);
		Combo sizeCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(sizeCombo);
		ComboViewer sizeComboViewer = new ComboViewer(sizeCombo);
		sizeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		sizeComboViewer.setInput(model.getSizes());
		Binding portBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(sizeComboViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a size.")))
				.to(BeanProperties.value(CreateStorageModel.PROPERTY_SIZE)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(portBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		}
}
