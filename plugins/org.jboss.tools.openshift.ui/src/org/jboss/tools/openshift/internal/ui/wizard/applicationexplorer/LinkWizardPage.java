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
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Red Hat Developers
 *
 */
public abstract class LinkWizardPage<T extends LinkModel<?>> extends AbstractOpenShiftWizardPage {

	protected T model;

	protected LinkWizardPage(IWizard wizard, T model, String title, String description) {
		super(title, description, "Link", wizard);
		this.model = model;
	}

	protected void createTargetControls(Composite parent, DataBindingContext dbc, String label, ILabelProvider labelProvider) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label targetsLabel = new Label(parent, SWT.NONE);
		targetsLabel.setText(label);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).applyTo(targetsLabel);
		Table targetsTable = new Table(parent, SWT.BORDER | SWT.SINGLE);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(targetsTable);
		TableViewer targetsTableViewer = new TableViewer(targetsTable);
		targetsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		if (labelProvider != null) {
			targetsTableViewer.setLabelProvider(labelProvider);
		}
		targetsTableViewer.setInput(model.getTargets());
		Binding portBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(targetsTableViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a target.")))
				.to(BeanProperties.value(LinkModel.PROPERTY_TARGET)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(portBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		}
}