/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAdvancedPropertiesProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.IAdvancedConnectionPropertiesEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DialogAdvancedPart;
import org.jboss.tools.openshift.internal.core.preferences.OCBinaryVersionValidator;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;
import org.osgi.framework.Version;

@SuppressWarnings("rawtypes")
public class AdvancedConnectionEditor extends BaseDetailsView implements IAdvancedConnectionPropertiesEditor{

	private static final String VALIDATOR_URL_TYPE = "image registry";
	private static final int DELAY = 500;
	private AdvancedConnectionEditorModel model;
	private ConnectionWizardPageModel pageModel;
	private IObservableValue selectedConnection;
	private IValueChangeListener connectionChangedListener;
	private IObservableValue registryURLObservable;
	private IObservableValue clusterNamespaceObservable;
	private IConnectionAdvancedPropertiesProvider connectionAdvancedPropertiesProvider;
	private ControlDecoration decoration;
	
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
				
                Label lblNamespace = new Label(advancedComposite, SWT.NONE);
                lblNamespace.setText("Cluster namespace:");
                GridDataFactory.fillDefaults()
                    .align(SWT.LEFT, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(lblNamespace);
                
                Text txtClusterNamespace = new Text(advancedComposite, SWT.BORDER);
                GridDataFactory.fillDefaults()
                    .align(SWT.FILL, SWT.CENTER)
                    .grab(true, false)
                    .applyTo(txtClusterNamespace);
                
                clusterNamespaceObservable = WidgetProperties.text(SWT.Modify).observeDelayed(DELAY, txtClusterNamespace);
                ValueBindingBuilder.bind(clusterNamespaceObservable)
                    .converting(new TrimmingStringConverter())
                    .to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_CLUSTER_NAMESPACE).observe(model))
                    .in(dbc);
                
                
                // Override OC location widgets
                
                Button overrideOC = new Button(advancedComposite, SWT.CHECK);
                overrideOC.setText("Override 'oc' location: ");
                GridDataFactory.fillDefaults()
                .align(SWT.LEFT, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(lblRegistry);
                
                Composite ocTextAndBrowse = new Composite(advancedComposite, SWT.NONE);
                ocTextAndBrowse.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
                GridDataFactory.fillDefaults()
                .align(SWT.FILL, SWT.CENTER)
                .grab(true, false)
                .applyTo(ocTextAndBrowse);
                
                final Text ocText = new Text(ocTextAndBrowse, SWT.SINGLE | SWT.BORDER);
                GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(250, SWT.DEFAULT).applyTo(ocText);
                Button ocBrowse = new Button(ocTextAndBrowse, SWT.PUSH);
                ocBrowse.setText("Browse...");
                
                
                GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(ocBrowse);
                
                // Most likely will be changed
                IObservableValue overrideOCObservable = WidgetProperties.selection().observeDelayed(DELAY, overrideOC);
                ValueBindingBuilder.bind(overrideOCObservable)
                    .to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE).observe(model))
                    .in(dbc);
                
                IObservableValue ocLocationObservable = WidgetProperties.text(SWT.Modify).observeDelayed(DELAY, ocText);
                ValueBindingBuilder.bind(ocLocationObservable)
                    .converting(new TrimmingStringConverter())
                    .to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE_LOCATION).observe(model))
                    .in(dbc);
                

                // Validation here is done via a listener rather than dbc validators
                // because dbc validators will validate in the UI thread, but validation
                // of this field requires a background job. 
                ocBrowse.addSelectionListener(new SelectionAdapter() {
                	@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fd = new FileDialog(ocBrowse.getShell());
						fd.setText(ocText.getText());
						String result = fd.open();
						if( result != null ) {
							ocLocationObservable.setValue(result);
							validateOCLocation(ocText.getText(), overrideOC.getSelection());
						}
					}
				});
                
                overrideOC.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						ocText.setEnabled(overrideOC.getSelection());
						ocBrowse.setEnabled(overrideOC.getSelection());
						validateOCLocation(ocText.getText(), overrideOC.getSelection());
					}
				});
                
                ocText.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validateOCLocation(ocText.getText(), overrideOC.getSelection());
					}});
                
        		decoration = new ControlDecoration(ocText,SWT.LEFT | SWT.TOP);
        		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
        				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        		decoration.setImage(fieldDecoration.getImage());

                
                
				ocText.setEnabled(overrideOC.getSelection());
				ocBrowse.setEnabled(overrideOC.getSelection());
				
				validateOCLocation(ocText.getText(), overrideOC.getSelection());

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
	

	private void validateOCLocation(String location, boolean override) {
		kickOCLocationValidation(location, override);
	}
	
	private UIUpdatingJob versionVerificationJob;
	private void kickOCLocationValidation(String location, boolean override) {
		if( !override ) {
			return;
		}
		
		if( versionVerificationJob != null ) {
			versionVerificationJob.cancel();
		}
		
		versionVerificationJob = new UIUpdatingJob("Checking oc binary...") {
 
		    private Version version;
		    private String error;
		    
            @Override
            protected IStatus run(IProgressMonitor monitor) {
            	
        		if (StringUtils.isBlank(location)) {
        			error = "OC Location cannot be empty";
        		} else {
	        		File file = new File(location);
	        		// Error messages have to be set to field editor, not directly to the
	        		// page.
	        		if (!file.exists()) {
	        			error = (NLS.bind("{0} was not found.", file));
	        		} else if (!file.canExecute()) {
	        			error = (NLS.bind("{0} does not have execute permissions.", file));
	        		}
        		}
        		
        		if( error == null ) {
        			version = new OCBinaryVersionValidator(location).getVersion(monitor);
        		}
                if (monitor.isCanceled()) {
                	return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }

            @Override
            protected IStatus updateUI(IProgressMonitor monitor) {
            	if( error == null ) {
					if (Version.emptyVersion.equals(version)) {
						error = ("Could not determine your OpenShift client version");
					} else if( !OCBinaryVersionValidator.isCompatibleForPublishing(version)) {
						error = NLS.bind("OpenShift client version 1.1.1 or higher is required to avoid rsync issues.", version);
					}
            	}
            	
            	//  now we have an error string
            	if( error == null ) {
            		// hide decorator
            		decoration.hide();
            	} else {
            		// show decorator, set message to error
            		decoration.setDescriptionText(error);
            		decoration.show();
            	}
            	
            	return Status.OK_STATUS;
            }
        };
        versionVerificationJob.schedule();
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
		static final String PROP_CLUSTER_NAMESPACE = "clusterNamespace";
		
		// custom oc location
		static final String PROP_OC_OVERRIDE = "ocOverride";
		static final String PROP_OC_OVERRIDE_LOCATION = "ocOverrideLocation";

		public void setOcOverride(boolean value) {
			Connection connection = getConnection();
			if(connection != null) {
				Map<String, Object> properties = connection.getExtendedProperties();
				Object old = properties.get(ICommonAttributes.OC_OVERRIDE_KEY);
				connection.setExtendedProperty(ICommonAttributes.OC_OVERRIDE_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE, old, value);
			}			
		}
		
		public boolean getOcOverride() {
			Connection connection = getConnection();
			if(connection != null) {
				Map<String, Object> properties = connection.getExtendedProperties();
				return ((Boolean)(ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_OVERRIDE_KEY), false))).booleanValue(); 
			}
			return false;
		}
		
		public void setOcOverrideLocation(String value) {
			Connection connection = getConnection();
			if(connection != null) {
				Map<String, Object> properties = connection.getExtendedProperties();
				Object old = properties.get(ICommonAttributes.OC_LOCATION_KEY);
				connection.setExtendedProperty(ICommonAttributes.OC_LOCATION_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE_LOCATION, old, value);
			}			
		}
		
		public String getOcOverrideLocation() {
			if( getOcOverride()) {
				Connection connection = getConnection();
				if(connection != null) {
					Map<String, Object> properties = connection.getExtendedProperties();
					return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_LOCATION_KEY), ""); 
				}
			}
			return OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		}
		
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
		
        public void setClusterNamespace(String value) {
            Connection connection = getConnection();
            if(connection != null) {
                Map<String, Object> properties = connection.getExtendedProperties();
                Object old = properties.get(ICommonAttributes.CLUSTER_NAMESPACE_KEY);
                connection.setExtendedProperty(ICommonAttributes.CLUSTER_NAMESPACE_KEY, value);
                firePropertyChange(PROP_CLUSTER_NAMESPACE, old, value);
            }           
        }
        
        public String getClusterNamespace() {
            Connection connection = getConnection();
            if(connection != null) {
                return connection.getClusterNamespace(); 
            }
            return ICommonAttributes.COMMON_NAMESPACE;
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
					connection.setExtendedProperty(ICommonAttributes.CLUSTER_NAMESPACE_KEY, clusterNamespaceObservable.getValue());
				}
			});
			return connection;
		}
		
	}
}
