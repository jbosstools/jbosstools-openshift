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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.IConnectionUI;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionUI extends BaseDetailsView implements IConnectionUI<ExpressConnection> {

	private DataBindingContext privateDbc;
	private WritableValue passwordObservable;
	private IObservableValue rememberPasswordObservable;

	public ExpressConnectionUI() {
		this.privateDbc = new DataBindingContext();
		this.rememberPasswordObservable = new WritableValue(null, Boolean.class);
	}

	@Override
	public Composite createControls(Composite parent, IObservableValue detailViewModel, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(composite);
		
		// username
		Label rhLoginLabel = new Label(composite, SWT.NONE);
		rhLoginLabel.setText("&Username:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(rhLoginLabel);
		Text usernameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(usernameText);
		Binding usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("username"))
				.to(PojoProperties.value(ExpressConnection.class, "username").observeDetail(detailViewModel))
				.in(privateDbc);
		ControlDecorationSupport
				.create(usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		// password
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		Text passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordText);
		Binding passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("password"))
				.to(PojoProperties.value(ExpressConnection.class, "password").observeDetail(detailViewModel))
				.in(privateDbc);
		ControlDecorationSupport
				.create(passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Button rememberPasswordCheckBox = new Button(composite, SWT.CHECK);
		rememberPasswordCheckBox.setText("&Save Password (could trigger secure storage login)");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(rememberPasswordCheckBox);

		DataBindingUtils.observeAndPrintValidationState("", dbc);
		return composite;
	}

	@Override
	public void onVisible(IObservableValue connectionObservable, DataBindingContext dbc) {
		addValidationStatusProviders(privateDbc.getValidationStatusProviders(), dbc);
	}

	@Override
	public void onInVisible(IObservableValue connectionObservable, DataBindingContext dbc) {
		removeValidationStatusProviders(privateDbc.getValidationStatusProviders(), dbc);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ExpressConnection;
	}

	@Override
	public void updateConnection(ExpressConnection connection) {
	}

	protected void addValidationStatusProviders(Collection<ValidationStatusProvider> providers, DataBindingContext dbc) {
		for (ValidationStatusProvider provider: new ArrayList<ValidationStatusProvider>(providers)) {
			dbc.addValidationStatusProvider(provider);
		}
	}

	protected void removeValidationStatusProviders(Collection<ValidationStatusProvider> providers, DataBindingContext dbc) {
		for (ValidationStatusProvider provider: new ArrayList<ValidationStatusProvider>(providers)) {
			dbc.removeValidationStatusProvider(provider);
		}
	}

	@Override
	public void dispose() {
		privateDbc.dispose();
	}
}
