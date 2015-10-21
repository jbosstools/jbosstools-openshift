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

import org.apache.commons.lang.BooleanUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnectionFactory;
import org.jboss.tools.openshift.internal.common.ui.connection.BaseConnectionEditor;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionEditor extends BaseConnectionEditor {

	private Text usernameText;
	private IObservableValue usernameObservable;
	private Binding usernameBinding;
	private Text passwordText;
	private IObservableValue passwordObservable;
	private Binding passwordBinding;
	private IObservableValue rememberPasswordObservable;
	private Button rememberPasswordCheckBox;
	private Binding rememberPasswordBinding;
	
	public ExpressConnectionEditor() {
	}

	@Override
	public Composite createControls(Composite parent, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
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
		this.usernameObservable = new WritableValue(null, String.class);
		usernameObservable.addValueChangeListener(changeListener);
		
		// password
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		this.passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordText);
		this.passwordObservable = new WritableValue(null, String.class);
		passwordObservable.addValueChangeListener(changeListener);
		
		this.rememberPasswordCheckBox = new Button(composite, SWT.CHECK);
		rememberPasswordCheckBox.setText("&Save password (could trigger secure storage login)");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(rememberPasswordCheckBox);
		this.rememberPasswordObservable = new WritableValue(null, Boolean.class);
		rememberPasswordObservable.addValueChangeListener(changeListener);
		
		return composite;
	}

	@Override
	public void onVisible(IObservableValue detailViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		bindWidgetsToInternalModel(detailViewModel, dbc);
		updateFrom(selectedConnection);
	}

	private void bindWidgetsToInternalModel(IObservableValue detailViewModel, DataBindingContext dbc) {
		// username
		this.usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("username"))
				.to(usernameObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// password
		this.passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("password"))
				.to(passwordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		// remember password
		this.rememberPasswordBinding = ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(rememberPasswordCheckBox))
				.to(rememberPasswordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				rememberPasswordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}
	
	@Override
	public void onInVisible(IObservableValue detailViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		disposeBindings();
	}

	@Override
	protected void onSelectedConnectionChanged(IObservableValue selectedConnection) {
		updateFrom(selectedConnection);
	}
	
	private void updateFrom(IObservableValue selectedConnectionObservable) {
		if (selectedConnectionObservable.getValue() instanceof ExpressConnection) {
			ExpressConnection selectedConnection = (ExpressConnection) selectedConnectionObservable.getValue();
			usernameObservable.setValue(selectedConnection.getUsername());
			passwordObservable.setValue(selectedConnection.getPassword());
			rememberPasswordObservable.setValue(selectedConnection.isRememberPassword());
		} else if (selectedConnectionObservable.getValue() instanceof NewConnectionMarker) {
			usernameObservable.setValue(null);
			passwordObservable.setValue(null);
			rememberPasswordObservable.setValue(false);
		}
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ExpressConnectionFactory;
	}

	@Override
	public void dispose() {
		disposeBindings();
	}

	private void disposeBindings() {
		DataBindingUtils.dispose(usernameBinding);
		DataBindingUtils.dispose(passwordBinding);
		DataBindingUtils.dispose(rememberPasswordBinding);
	}

	@Override
	protected IConnectionAuthenticationProvider createConnectionAuthenticationProvider(ConnectionWizardPageModel pageModel) {
		return new ExpressConnectionAuthenticationProvider();
	}
	
	private class ExpressConnectionAuthenticationProvider implements IConnectionAuthenticationProvider {

		@Override
		public IConnection update(IConnection connection) {
			Assert.isLegal(connection instanceof ExpressConnection);
			
			final ExpressConnection expressConnection = (ExpressConnection) connection;
			// might be called from job, switch to display thread to access observables
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					expressConnection.setUsername((String) usernameObservable.getValue());
					expressConnection.setPassword((String) passwordObservable.getValue());
					expressConnection.setRememberPassword(
							BooleanUtils.toBoolean((Boolean) rememberPasswordObservable.getValue()));
				}
			});

			return expressConnection;
		}
		
	}
}
