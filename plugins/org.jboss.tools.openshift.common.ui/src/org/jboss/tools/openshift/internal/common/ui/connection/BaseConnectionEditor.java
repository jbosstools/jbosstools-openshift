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
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;

/**
 * @author Andre Dietisheim
 */
public abstract class BaseConnectionEditor extends BaseDetailsView implements IConnectionEditor {

	protected ConnectionWizardPageModel pageModel;
	protected IObservableValue selectedConnection;
	protected IValueChangeListener changeListener;
	protected IConnectionAuthenticationProvider connectionAuthenticationProvider;
	
	public BaseConnectionEditor() {
	}

	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		this.pageModel = (ConnectionWizardPageModel) context;
		this.selectedConnection = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION).observe(pageModel);
		this.changeListener = onValueChanged(pageModel);
		this.connectionAuthenticationProvider = createConnectionAuthenticationProvider(pageModel);
		
		return createControls(parent, pageModel, dbc);
	}

	protected abstract IConnectionAuthenticationProvider createConnectionAuthenticationProvider(ConnectionWizardPageModel pageModel);

	protected abstract Composite createControls(Composite parent, ConnectionWizardPageModel pageModel, DataBindingContext dbc);

	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAuthenticationProvider(connectionAuthenticationProvider);
		onVisible(detailViewModel, pageModel, dbc);
	}

	protected abstract void onVisible(IObservableValue detailViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc);
	
	@Override
	public void onInVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAuthenticationProvider(null);
		onInVisible(detailViewModel, pageModel, dbc);
	}

	protected abstract void onInVisible(IObservableValue detailViewModel, ConnectionWizardPageModel pageModel, DataBindingContext dbc);

	protected IValueChangeListener onValueChanged(final ConnectionWizardPageModel pageModel) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				pageModel.resetConnectError();
			}
		};
	}
}
