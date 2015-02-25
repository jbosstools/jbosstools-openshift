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
import org.jboss.tools.openshift.internal.common.ui.connection.IConnectionUI;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;

/**
 * Detail view used in the common Connection Wizard to
 * support establishing connections to v3 instance of OpenShift
 *
 */
public class ConnectionDetailView extends BaseDetailsView implements IConnectionUI{

	private static final String USERNAME = "username";
	private static final String PASSWORD  = "password";

	private Text txtUserName;
	private Text txtPassword;
	private Binding bindUserName;
	private Binding bindPassword;

	@Override
	public Composite createControls(Composite parent, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(composite);

		// username
		Label lblUserName = new Label(composite, SWT.NONE);
		lblUserName.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(lblUserName);
		this.txtUserName = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtUserName);

		// password
		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(lblPassword);
		this.txtPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtPassword);
		
		return composite;
	}
	
	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		// username
		this.bindUserName = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(txtUserName))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator(USERNAME))
				.to(BeanProperties.value(Connection.class, USERNAME).observeDetail(detailViewModel))
				.in(dbc);
		ControlDecorationSupport.create(
				bindUserName, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		this.bindPassword = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(txtPassword))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator(PASSWORD))
				.to(BeanProperties.value(Connection.class, PASSWORD).observeDetail(detailViewModel))
				.in(dbc);
		ControlDecorationSupport.create(
				bindPassword, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}
	
	@Override
	public boolean isViewFor(Object object) {
		return object instanceof Connection;
	}
}
