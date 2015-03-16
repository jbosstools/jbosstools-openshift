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
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.IConnectionEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionEditor extends BaseDetailsView implements IConnectionEditor {

	private Text usernameText;
	private Binding usernameBinding;
	private IObservableValue usernameObservable;
	private Text passwordText;
	private Binding passwordBinding;
	private IObservableValue rememberPasswordObservable;
	private Button rememberPasswordCheckBox;
	private Binding rememberPasswordBinding;
	private IObservableValue passwordObservable;
	
	public ExpressConnectionEditor() {
	}

	@Override
	public Composite createControls(Composite parent, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(composite);

		// username
		Label rhLoginLabel = new Label(composite, SWT.NONE);
		rhLoginLabel.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(rhLoginLabel);
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

		this.rememberPasswordCheckBox = new Button(composite, SWT.CHECK);
		rememberPasswordCheckBox.setText("&Save Password (could trigger secure storage login)");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(rememberPasswordCheckBox);

		return composite;
	}

	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		if (detailViewModel.getValue() == null) {
			return;
		}
		createBindings(detailViewModel, dbc);
	}

	@Override
	public void onInVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		disposeBindings();
	}

	private void createBindings(IObservableValue detailViewModel, DataBindingContext dbc) {
		// username
		this.usernameObservable = 
				BeanProperties.value(ExpressConnection.class, ExpressConnection.PROPERTY_USERNAME).observeDetail(detailViewModel);
		this.usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("username"))
				.to(usernameObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		this.passwordObservable = 
				BeanProperties.value(ExpressConnection.class, ExpressConnection.PROPERTY_PASSWORD).observeDetail(detailViewModel);
		this.passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("password"))
				.to(passwordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// remember password
		this.rememberPasswordObservable = 
				BeanProperties.value(ExpressConnection.class, ExpressConnection.PROPERTY_REMEMBER_PASSWORD).observeDetail(detailViewModel);
		this.rememberPasswordBinding = ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(rememberPasswordCheckBox))
				.to(rememberPasswordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				rememberPasswordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}

	private void disposeBindings() {
		DataBindingUtils.dispose(usernameBinding);
		this.usernameBinding = null;
		DataBindingUtils.dispose(usernameObservable);
		this.usernameObservable = null;
		DataBindingUtils.dispose(passwordBinding);
		this.passwordBinding = null;
		DataBindingUtils.dispose(passwordObservable);
		this.passwordObservable = null;
		DataBindingUtils.dispose(rememberPasswordBinding);
		this.rememberPasswordBinding = null;
		DataBindingUtils.dispose(rememberPasswordObservable);
		this.rememberPasswordObservable = null;
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ExpressConnection;
	}

	@Override
	public void dispose() {
		disposeBindings();
	}
}
