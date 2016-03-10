/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.internal.common.ui.connection.BaseConnectionEditor;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews.IDetailView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * Detail view used in the common Connection Wizard to
 * support establishing connections to 3 instance of OpenShift
 * 
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 *
 */
public class ConnectionEditor extends BaseConnectionEditor {


	private static final String PROPERTY_SELECTED_DETAIL_VIEW = "selectedDetailView";
	
	private Map<String,IConnectionEditorDetailView> detailViews = new HashMap<String, IConnectionEditorDetailView>();
	private ConnectionEditorStackedDetailViews stackedViews ;
	private DetailViewModel detailViewModel = new DetailViewModel();

	private ComboViewer authTypeViewer;
	private IObservableValue rememberTokenObservable;
	private IObservableValue detailViewObservable;
	private IObservableValue authSchemeObservable;
	private Binding selectedAuthTypeBinding;
	
	private class DetailViewModel extends ObservablePojo{
		private IConnectionEditorDetailView selectedDetailView;
		
		public IConnectionEditorDetailView getSelectedDetailView() {
			return this.selectedDetailView;
		}
		@SuppressWarnings("unused")
		public void  setSelectedDetailView(IConnectionEditorDetailView view) {
			this.selectedDetailView = view;
		}
		
		public void setAuthScheme(String scheme) {
			detailViewObservable.setValue(detailViews.get(scheme));
		}
		
		public void setSelectedConnection(IConnection conn) {
			if(conn instanceof Connection) {
				Connection connection = (Connection) conn;
				detailViewObservable.setValue(detailViews.get(connection.getAuthScheme()));
			}else {
				rememberTokenObservable.setValue(Boolean.FALSE);
				detailViewObservable.setValue(detailViews.get(IAuthorizationContext.AUTHSCHEME_OAUTH));
			}
			for (IConnectionEditorDetailView view : detailViews.values()) {
				//reset all views
				view.setSelectedConnection(conn);
			}
		}
	}
	
	private class ConnectionEditorStackedDetailViews extends AbstractStackedDetailViews{

		public ConnectionEditorStackedDetailViews(IObservableValue detailViewModel, Object context, Composite parent,
				DataBindingContext dbc) {
			super(detailViewModel, context, parent, dbc);
		}

		@Override
		protected IDetailView[] getDetailViews() {
			return detailViews.values().toArray(new IConnectionEditorDetailView[detailViews.size()]);
		}
		
	}
	
	@Override
	public Composite createControls(Composite parent, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(composite);

		this.detailViewObservable = 
				BeanProperties.value(PROPERTY_SELECTED_DETAIL_VIEW, IConnectionEditorDetailView.class).observe(detailViewModel);
		this.authSchemeObservable = 
				BeanProperties.value("authScheme", String.class).observe(detailViewModel);
		//detail views
		OAuthDetailView oAuthDetailView = new OAuthDetailView(wizardPage.getWizard(), pageModel, changeListener, pageModel.getContext(), authSchemeObservable);
		detailViews.put(IAuthorizationContext.AUTHSCHEME_OAUTH, 
				oAuthDetailView);
		detailViews.put(IAuthorizationContext.AUTHSCHEME_BASIC, 
				new BasicAuthenticationDetailView(changeListener, pageModel.getContext()));
		rememberTokenObservable = oAuthDetailView.getRememberTokenObservable();

		// auth type
		Label authTypeLabel = new Label(composite, SWT.NONE);
		authTypeLabel.setText("Protocol:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(authTypeLabel);
		Combo authTypeCombo = new Combo(composite, SWT.DEFAULT);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(authTypeCombo);
		this.authTypeViewer = new ComboViewer(authTypeCombo);
		authTypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		authTypeViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element.toString();
			}
		});
		authTypeViewer.setInput(detailViews.values());

		//connection detail views
		final Composite detailsContainer = new Composite(composite, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).span(3,1).applyTo(detailsContainer);
		stackedViews = new ConnectionEditorStackedDetailViews(
				detailViewObservable,
				pageModel, 
				detailsContainer, 
				dbc);
		stackedViews.createControls(false);
		
		return composite;
	}
	
	@Override
	public void onVisible(IObservableValue detailViewModelObservable, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		bindWidgetsToInternalModel(dbc);
		detailViewModel.setSelectedConnection(pageModel.getSelectedConnection());
	}
	
	@Override
	public void onInVisible(IObservableValue detailViewModelObservable, DataBindingContext dbc) {
		detailViewModel.getSelectedDetailView().onInVisible(detailViewModelObservable, dbc);
		disposeBindings();
	}

	private void bindWidgetsToInternalModel(DataBindingContext dbc) {
		//auth protocol
		selectedAuthTypeBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(authTypeViewer))
				.validatingAfterGet(
						new IsNotNullValidator(
								ValidationStatus.cancel("Please select an authorization protocol.")))
				.to(detailViewObservable)
				.in(dbc);
		ControlDecorationSupport
				.create(selectedAuthTypeBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
	}

	@Override
	public void onInVisible(IObservableValue detailsViewModelObservable, ConnectionWizardPageModel pageModel, DataBindingContext dbc) {
		detailViewModel.getSelectedDetailView().onInVisible(detailsViewModelObservable, dbc);
		disposeBindings();
	}

	private void disposeBindings() {
		DataBindingUtils.dispose(selectedAuthTypeBinding);
		for (IDetailView view : stackedViews.getDetailViews()) {
			view.dispose();
		}			
		
	}
	
	@Override
	protected void onSelectedConnectionChanged(IObservableValue selectedConnection) {
		IConnection conn = (IConnection) selectedConnection.getValue();
		detailViewModel.setSelectedConnection(conn);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ConnectionFactory;
	}
	
	private IConnectionEditorDetailView getDetailView() {
		return detailViewModel.getSelectedDetailView();
	}
	
	@Override
	protected IConnectionAuthenticationProvider createConnectionAuthenticationProvider(ConnectionWizardPageModel pageModel) {
		return new ConnectionAuthenticationProviderProxy();
	}
	
	private class  ConnectionAuthenticationProviderProxy implements IConnectionAuthenticationProvider {
		@Override
		public IConnection update(IConnection connection) {
			return getDetailView().getConnectionAuthenticationProvider().update(connection);
		}
		
	}

}
