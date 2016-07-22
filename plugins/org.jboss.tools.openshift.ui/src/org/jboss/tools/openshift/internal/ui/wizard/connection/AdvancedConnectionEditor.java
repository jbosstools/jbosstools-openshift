/*******************************************************************************
 * coright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAdvancedPropertiesProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.IAdvancedConnectionPropertiesEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DialogAdvancedPart;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;

@SuppressWarnings("rawtypes")
public class AdvancedConnectionEditor extends BaseDetailsView implements IAdvancedConnectionPropertiesEditor{

	private static final String VALIDATOR_URL_TYPE = "image registry";
	private static final int DELAY = 500;
	private AdvancedConnectionEditorModel model;
	private ConnectionWizardPageModel pageModel;
	private IObservableValue selectedConnection;
	private IValueChangeListener connectionChangedListener;
	private IObservableValue registryURLObservable;
	private IConnectionAdvancedPropertiesProvider connectionAdvancedPropertiesProvider;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		
		this.pageModel =  (ConnectionWizardPageModel) context;
		this.selectedConnection = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION).observe(pageModel);
		
		model = new AdvancedConnectionEditorModel();
		
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		DialogAdvancedPart part = new DialogAdvancedPart() {
			


			@Override
			protected void createAdvancedContent(Composite advancedComposite) {
				Label lblRegistry = new Label(advancedComposite, SWT.NONE);
				lblRegistry.setText("Image Registry URL:");
				GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(lblRegistry);
				
				Text txtRegistry = new Text(advancedComposite, SWT.BORDER);
				GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.grab(true, false)
					.applyTo(txtRegistry);
				
				registryURLObservable = WidgetProperties.text(SWT.Modify).observeDelayed(DELAY, txtRegistry);
				ValueBindingBuilder.bind(registryURLObservable)
					.validatingAfterConvert(new URLValidator(VALIDATOR_URL_TYPE, true))
					.converting(new TrimTrailingSlashConverter())
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_REGISTRY_URL).observe(model))
					.in(dbc);
				
			}

			@Override
			protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
				return gridLayoutFactory.numColumns(2);
			}
			
			
		};
		part.createAdvancedGroup(composite, 1);
		this.connectionAdvancedPropertiesProvider = new ConnectionAdvancedPropertiesProvider();
		
		return composite;
	}
	
	@SuppressWarnings("unchecked")
	protected void onSelectedConnectionChanged(IObservableValue selectedConnection) {
		registryURLObservable.setValue(model.getRegistryURL());
	}
	
	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAdvancedPropertiesProvider(this.connectionAdvancedPropertiesProvider);
		this.connectionChangedListener = addSelectedConnectionChangedListener(selectedConnection, dbc);
	}
	
	@Override
	public void onInVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAdvancedPropertiesProvider(null);
		removeConnectionChangedListener(connectionChangedListener, selectedConnection);
		this.connectionChangedListener = null;
	}

	@SuppressWarnings("unchecked")
	private IValueChangeListener addSelectedConnectionChangedListener(final IObservableValue selectedConnection, final DataBindingContext dbc) {
		IValueChangeListener listener = new IValueChangeListener() {
			
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				onSelectedConnectionChanged(selectedConnection);
				DataBindingUtils.validateTargetsToModels(dbc);
			}
		};
		selectedConnection.addValueChangeListener(listener);
		return listener;
	}
	
	@SuppressWarnings("unchecked")
	private void removeConnectionChangedListener(final IValueChangeListener connectionChangedListener, final IObservableValue selectedConnection) {
		if (selectedConnection == null 
				|| selectedConnection.isDisposed()
				|| connectionChangedListener == null) {
			return;
		}
		
		selectedConnection.removeValueChangeListener(connectionChangedListener);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ConnectionFactory;
	}

	private Connection getConnection() {
		IConnection connection = pageModel.getSelectedConnection();
		return connection instanceof Connection ? (Connection) connection : null;
	}
	
	class AdvancedConnectionEditorModel extends ObservablePojo{
		
		static final String PROP_REGISTRY_URL = "registryURL";

		public void setRegistryURL(String value) {
			Connection connection = getConnection();
			if(connection != null) {
				Map<String, Object> properties = connection.getExtendedProperties();
				Object old = properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY);
				connection.setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, value);
				firePropertyChange(PROP_REGISTRY_URL, old, value);
			}			
		}
		
		public String getRegistryURL() {
			Connection connection = getConnection();
			if(connection != null) {
				Map<String, Object> properties = connection.getExtendedProperties();
				return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY), ""); 
			}
			return "";
		}
		
	}
	
	private class ConnectionAdvancedPropertiesProvider implements IConnectionAdvancedPropertiesProvider {

		@Override
		public IConnection update(IConnection conn) {
			Assert.isLegal(conn instanceof Connection);

			final Connection connection = (Connection) conn;
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					connection.setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, registryURLObservable.getValue());
				}
			});
			return connection;
		}
		
	}
}
