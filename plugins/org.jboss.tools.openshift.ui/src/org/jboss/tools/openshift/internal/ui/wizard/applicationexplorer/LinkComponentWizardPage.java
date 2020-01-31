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
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;

/**
 * @author Red Hat Developers
 *
 */
public class LinkComponentWizardPage extends LinkWizardPage<LinkComponentModel> {

	protected LinkComponentWizardPage(IWizard wizard, LinkComponentModel model) {
		super(wizard, model, "Link component", "Select a target component to bind the component to.");
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		createTargetControls(parent, dbc, "Target component:", new ComponentColumLabelProvider());
		
		IObservableList<Integer> portsObservable = BeanProperties.list(LinkComponentModel.PROPERTY_PORTS, Integer.class).observe(model);
		Label portsLabel = new Label(parent, SWT.NONE);
		portsLabel.setText("Port:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(portsLabel);
		Combo portsCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(portsCombo);
		ComboViewer portsComboViewer = new ComboViewer(portsCombo);
		portsComboViewer.setContentProvider(new ObservableListContentProvider<>());
		portsComboViewer.setInput(portsObservable);
		Binding portsBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(portsComboViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a port.")))
				.to(BeanProperties.value(LinkComponentModel.PROPERTY_PORT).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(portsBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());

	}
}
