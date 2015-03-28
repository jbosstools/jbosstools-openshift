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
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.internal.common.ui.connection.BaseConnectionEditor;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;

/**
 * Detail view used in the common Connection Wizard to
 * support establishing connections to v3 instance of OpenShift
 *
 */
public class ConnectionEditor extends BaseConnectionEditor {

	private Text usernameText;
	private IObservableValue usernameObservable;
	private Binding usernameBinding;
	private Binding connectionUsernameBinding;
	private Text passwordText;
	private IObservableValue passwordObservable;
	private Binding passwordBinding;
	private Binding connectionPasswordBinding;
	
	@Override
	public Composite createControls(Composite parent, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
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
		this.usernameObservable = new WritableValue(null, String.class);
		
		// password
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setText("&Password:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passwordLabel);
		this.passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(passwordText);
		this.passwordObservable = new WritableValue(null, String.class);
		
		return composite;
	}
	
	@Override
	public void onVisible(IObservableValue detailViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		bindWidgetsToInternalModel(dbc);
		bindInternalModelToSelectedConnection(selectedConnection, dbc);
	}

	private void bindWidgetsToInternalModel(DataBindingContext dbc) {
		// username
		this.usernameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(usernameText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("username"))
				.to(usernameObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				usernameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		usernameObservable.addValueChangeListener(changeListener);

		// password
		this.passwordBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passwordText))
				.converting(new TrimmingStringConverter())
				.validatingAfterConvert(new RequiredStringValidator("password"))
				.to(passwordObservable)
				.in(dbc);
		ControlDecorationSupport.create(
				passwordBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		passwordObservable.addValueChangeListener(changeListener);
	}

	private void bindInternalModelToSelectedConnection(IObservableValue selectedConnection, DataBindingContext dbc) {
		if (selectedConnection.getValue() == null
				|| selectedConnection.getValue() instanceof NewConnectionMarker) {
			return;
		}

		// username
		this.connectionUsernameBinding = ValueBindingBuilder
				.bind(usernameObservable)
				.notUpdating(BeanProperties.value(Connection.class, Connection.PROPERTY_USERNAME)
						.observeDetail(selectedConnection))
				.in(dbc);
		
		// password
		this.connectionPasswordBinding = ValueBindingBuilder
				.bind(passwordObservable)
				.notUpdating(BeanProperties.value(Connection.class, Connection.PROPERTY_PASSWORD)
						.observeDetail(selectedConnection))
				.in(dbc);
	}
	
	@Override
	public void onInVisible(IObservableValue detailsViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		DataBindingUtils.dispose(usernameBinding);
		DataBindingUtils.dispose(usernameObservable);
		DataBindingUtils.dispose(connectionUsernameBinding);
		DataBindingUtils.dispose(passwordBinding);
		DataBindingUtils.dispose(passwordObservable);
		DataBindingUtils.dispose(connectionPasswordBinding);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ConnectionFactory;
	}
	
	@Override
	protected IConnectionAuthenticationProvider createConnectionAuthenticationProvider(ConnectionWizardPageModel pageModel) {
		return new ConnectionAuthenticationProvider();
	}
	
	private class ConnectionAuthenticationProvider implements IConnectionAuthenticationProvider {

		@Override
		public IConnection update(IConnection conn) {
			Assert.isLegal(conn instanceof Connection);
			
			final Connection connection = (Connection) conn;
			// might be called from job, switch to display thread to access observables
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					connection.setUsername((String) usernameObservable.getValue());
					connection.setPassword((String) passwordObservable.getValue());
				}
			});

			return connection;
		}
		
	}
}
