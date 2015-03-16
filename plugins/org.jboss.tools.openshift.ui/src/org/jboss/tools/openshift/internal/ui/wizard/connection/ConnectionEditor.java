/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.IConnectionEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;

/**
 * Detail view used in the common Connection Wizard to
 * support establishing connections to v3 instance of OpenShift
 *
 */
public class ConnectionEditor extends BaseDetailsView implements IConnectionEditor {

	private static final String USERNAME = "username";
	private static final String PASSWORD  = "password";

	private Text usernameText;
	private Binding usernameBinding;
	private IObservableValue usernameObservable;
	private Text passwordText;
	private Binding passwordBinding;
	private IObservableValue passwordObservable;

	@Override
	public Composite createControls(Composite parent, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(composite);

		// username
		Label usernameLabel = new Label(composite, SWT.NONE);
		usernameLabel.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(usernameLabel);
		this.usernameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(usernameText);

		// password
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		this.passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordText);
		
		return composite;
	}
	
	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		if (detailViewModel.getValue() == null) {
			return;
		}
		createBindings(detailViewModel, dbc);
	}

	private void createBindings(IObservableValue detailViewModel, DataBindingContext dbc) {
		// username
		this.usernameObservable = BeanProperties.value(Connection.class, USERNAME).observeDetail(detailViewModel);
		this.usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator(USERNAME))
				.to(usernameObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		this.passwordObservable = BeanProperties.value(Connection.class, PASSWORD).observeDetail(detailViewModel);
		this.passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator(PASSWORD))
				.to(passwordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}
	
	@Override
	public void onInVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
		DataBindingUtils.dispose(usernameBinding);
		this.usernameBinding = null;
		DataBindingUtils.dispose(usernameObservable);
		this.usernameObservable = null;
		DataBindingUtils.dispose(passwordBinding);
		this.passwordBinding = null;
		DataBindingUtils.dispose(passwordObservable);
		this.passwordObservable = null;
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof Connection;
	}
}
