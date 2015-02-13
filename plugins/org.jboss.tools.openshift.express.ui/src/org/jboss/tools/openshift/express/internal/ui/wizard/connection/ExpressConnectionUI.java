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
	private Text usernameText;
	private Text passwordText;
	
	public ExpressConnectionUI() {
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

		Button rememberPasswordCheckBox = new Button(composite, SWT.CHECK);
		rememberPasswordCheckBox.setText("&Save Password (could trigger secure storage login)");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(rememberPasswordCheckBox);

		DataBindingUtils.observeAndPrintValidationState("", dbc);
		return composite;
	}

	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		createBindings(detailViewModel);
		addValidationStatusProviders(privateDbc.getValidationStatusProviders(), dbc);
	}

	@Override
	public void onInVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
//		removeValidationStatusProviders(privateDbc.getValidationStatusProviders(), dbc);
		disposeBindings(detailViewModel);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ExpressConnection;
	}

	@Override
	public void updateConnection(ExpressConnection connection) {
	}

	private void createBindings(IObservableValue detailViewModel) {
		this.privateDbc = new DataBindingContext();

		// username
		Binding usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("username"))
				.to(PojoProperties.value(ExpressConnection.class, "username").observeDetail(detailViewModel))
				.in(privateDbc);
		ControlDecorationSupport
				.create(usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		Binding passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("password"))
				.to(PojoProperties.value(ExpressConnection.class, "password").observeDetail(detailViewModel))
				.in(privateDbc);
		ControlDecorationSupport
				.create(passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

	}
	
	private void disposeBindings(IObservableValue detailsViewModel) {
		privateDbc.dispose();
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
