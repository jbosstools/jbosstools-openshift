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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAdvancedPropertiesProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.IAdvancedConnectionPropertiesEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
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
	Map<String, Object> extendedProperties = null;
	private IObservableValue<IStatus> ocLocationValidity = new WritableValue(Status.OK_STATUS, IStatus.class);
	private IObservableValue<IStatus> ocVersionValidity = new WritableValue(Status.OK_STATUS, IStatus.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
		
		this.pageModel =  (ConnectionWizardPageModel) context;
		this.selectedConnection = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION).observe(pageModel);
		this.model = new AdvancedConnectionEditorModel();
		
		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		DialogAdvancedPart part = new DialogAdvancedPart() {

			@Override
			protected void createAdvancedContent(Composite advancedComposite) {
				Label lblRegistry = new Label(advancedComposite, SWT.NONE);
				lblRegistry.setText("Image Registry URL:");
				GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(lblRegistry);
				
				Text txtRegistry = new Text(advancedComposite, SWT.BORDER);
				GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
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
                    .align(SWT.LEFT, SWT.CENTER).applyTo(lblNamespace);
                
                Text txtClusterNamespace = new Text(advancedComposite, SWT.BORDER);
                GridDataFactory.fillDefaults()
                    .align(SWT.FILL, SWT.CENTER)
                    .grab(true, false).span(2, 1)
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
					.align(SWT.LEFT, SWT.CENTER)
					.applyTo(lblRegistry);
				IObservableValue<Boolean> overrideOCObservable = WidgetProperties.selection().observe(overrideOC);
				ValueBindingBuilder
					.bind(overrideOCObservable)
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE).observe(model))
					.in(dbc);

				final Text ocText = new Text(advancedComposite, SWT.SINGLE | SWT.BORDER);
				GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.CENTER).grab(true, false)
					.applyTo(ocText);
				IObservableValue<String> ocLocationObservable = WidgetProperties.text(SWT.Modify).observe(ocText);
				Binding ocLocationBinding = ValueBindingBuilder
					.bind(ocLocationObservable)
					.converting(new TrimmingStringConverter())
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE_LOCATION).observe(model))
					.in(dbc);
				
				
				overrideOCObservable.addValueChangeListener(new IValueChangeListener<Boolean>() {
					@Override
					public void handleValueChange(ValueChangeEvent<? extends Boolean> event) {
						if( !overrideOCObservable.getValue()) {
							ocLocationObservable.setValue(OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation());
						}
					}
				});
				
				ocLocationBinding.getValidationStatus().addValueChangeListener(new IValueChangeListener<IStatus>() {

					@Override
					public void handleValueChange(ValueChangeEvent<? extends IStatus> event) {
						String location = model.getOcOverrideLocation();
						IStatus ocLocationStatus = validateOCLocation(location);
						ocLocationValidity.setValue(ocLocationStatus);
						if (ocLocationStatus.isOK()) {
							ocVersionValidity.setValue(ValidationStatus.cancel("Verifying oc version..."));
							OCVersionValidationJob job = new OCVersionValidationJob(model.getOcOverrideLocation());
							job.addJobChangeListener(new JobChangeAdapter() {

								@Override
								public void done(IJobChangeEvent event) {
									ocVersionValidity.getRealm().exec(() ->							
										ocVersionValidity.setValue(job.getOCVersionValidity()));
								}});
							job.schedule();
						}
					}
				});
				ValueBindingBuilder
					.bind(overrideOCObservable)
					.to(WidgetProperties.enabled().observe(ocText))
					.notUpdatingParticipant()
					.in(dbc);

				Button ocBrowse = new Button(advancedComposite, SWT.PUSH);
				ocBrowse.setText("Browse...");
				GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT)
					.applyTo(ocBrowse);
				// Validation here is done via a listener rather than dbc validators
				// because dbc validators will validate in the UI thread, but validation
				// of this field requires a background job.
				ocBrowse.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fd = new FileDialog(ocBrowse.getShell());
						fd.setText(ocText.getText());
						IPath p = new Path(ocText.getText());
						if( p.segmentCount() > 1 ) {
							fd.setFilterPath(p.removeLastSegments(1).toOSString());
						}
						String result = fd.open();
						if (result != null) {
							ocLocationObservable.setValue(result);
						}
					}
				});

				ValueBindingBuilder
					.bind(overrideOCObservable)
					.to(WidgetProperties.enabled().observe(ocBrowse))
					.notUpdatingParticipant()
					.in(dbc);
				
				ValidationStatusProvider ocValidator = new MultiValidator() {

					public IObservableList getTargets() {
						WritableList targets = new WritableList();
						targets.add(ocLocationObservable);
						targets.add(ocLocationValidity);
						targets.add(ocVersionValidity);
						return targets;
					}
					
					@Override
					protected IStatus validate() {
						// access all observables that the framework should listen to
						Boolean overrideOC = overrideOCObservable.getValue();
						String ocLocation = ocLocationObservable.getValue();
						IStatus ocLocationStatus = ocLocationValidity.getValue();
						IStatus ocVersionStatus = ocVersionValidity.getValue();

						IStatus status = Status.OK_STATUS;
						if (Boolean.TRUE.equals(overrideOC)) {
							if (!ocLocationStatus.isOK()) {
								status = ocLocationStatus;
							} else {
								status = ocVersionStatus;
							}
						}
						return status;
					}
				};

				dbc.addValidationStatusProvider(ocValidator);
				ControlDecorationSupport
					.create(ocValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
				
			}

			protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
				return gridLayoutFactory.numColumns(3);
			}
		};
		part.createAdvancedGroup(composite, 1);
		this.connectionAdvancedPropertiesProvider = new ConnectionAdvancedPropertiesProvider();

		return composite;
	}

	private IStatus validateOCLocation(String location) {
		IStatus validity = ValidationStatus.ok();
		if (StringUtils.isBlank(location)) {
			validity = ValidationStatus.error("Please provide a location for the OC binary.");
		} else {
			File file = new File(location);
			// Error messages have to be set to field editor, not directly to
			// the page.
			if (!file.exists()) {
				validity = ValidationStatus.error((NLS.bind("{0} was not found.", file)));
			} else if (file.isDirectory()) {
				validity = ValidationStatus.error((NLS.bind("OC Location must be a file.", file)));
			} else if (!file.canExecute()) {
				validity = ValidationStatus.error(NLS.bind("{0} does not have execute permissions.", file));
			}
		}
		return validity;
	}

	private class OCVersionValidationJob extends Job {

	    private Version version;
	    private String location;
	    private IStatus ocVersionValidity = ValidationStatus.cancel("OC version not verified yet.");

	    public OCVersionValidationJob(String location) {
			super("Checking oc binary...");
	    		this.location = location;
	    }
	    
        @Override
        protected IStatus run(IProgressMonitor monitor) {
			this.version = new OCBinaryVersionValidator(location).getVersion(monitor);
			if (Version.emptyVersion.equals(version)) {
				this.ocVersionValidity = ValidationStatus.error("Could not determine your OpenShift client version");
			} else if (!OCBinaryVersionValidator.isCompatibleForPublishing(version)) {
				this.ocVersionValidity = ValidationStatus.error(NLS.bind(
						"OpenShift client version 1.1.1 or higher is required to avoid rsync issues.", version));
			} else {
				this.ocVersionValidity = ValidationStatus.ok();
			}
			if (monitor.isCanceled()) {
				this.ocVersionValidity = ValidationStatus.cancel("OC version verification was cancelled.");
			}
			return Status.OK_STATUS;
		}
        
        public IStatus getOCVersionValidity() {
        		return ocVersionValidity;
        }
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

	public Map<String, Object> getExtendedProperties() {
		if (extendedProperties != null) {
			return extendedProperties;
		}
		IConnection connection = pageModel.getSelectedConnection();
		if (connection != null) {
			if (NewConnectionMarker.getInstance() == connection) {
				extendedProperties = new HashMap<String, Object>();
			}
			else if( connection instanceof Connection) {
				extendedProperties = ((Connection)connection).getExtendedProperties();
			}
			return extendedProperties;
		}
		return null;
	}
	
	class AdvancedConnectionEditorModel extends ObservablePojo{
		
		static final String PROP_REGISTRY_URL = "registryURL";
		static final String PROP_CLUSTER_NAMESPACE = "clusterNamespace";
		
		// custom oc location
		static final String PROP_OC_OVERRIDE = "ocOverride";
		static final String PROP_OC_OVERRIDE_LOCATION = "ocOverrideLocation";

		public void setOcOverride(boolean value) {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				Object old = properties.get(ICommonAttributes.OC_OVERRIDE_KEY);
				properties.put(ICommonAttributes.OC_OVERRIDE_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE, old, value);
			}			
		}
		
		public boolean getOcOverride() {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				return ((Boolean)(ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_OVERRIDE_KEY), false))).booleanValue(); 
			}
			return false;
		}
		
		public void setOcOverrideLocation(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				Object old = properties.get(ICommonAttributes.OC_LOCATION_KEY);
				properties.put(ICommonAttributes.OC_LOCATION_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE_LOCATION, old, value);
			}			
		}
		
		public String getOcOverrideLocation() {
			if( getOcOverride()) {
				Map<String, Object> properties = getExtendedProperties();
				if( properties != null ) {
					return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_LOCATION_KEY), ""); 
				}
			}
			return OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		}
		
		public void setRegistryURL(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				Object old = properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY);
				properties.put(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, value);
				firePropertyChange(PROP_REGISTRY_URL, old, value);
			}			
		}
		
		public String getRegistryURL() {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY), ""); 
			}
			return "";
		}
		
        public void setClusterNamespace(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
                Object old = properties.get(ICommonAttributes.CLUSTER_NAMESPACE_KEY);
                properties.put(ICommonAttributes.CLUSTER_NAMESPACE_KEY, value);
                firePropertyChange(PROP_CLUSTER_NAMESPACE, old, value);
            }           
        }

        public String getClusterNamespace() {
			Map<String, Object> properties = getExtendedProperties();
			if( properties != null ) {
				return (String)properties.getOrDefault(ICommonAttributes.CLUSTER_NAMESPACE_KEY, ICommonAttributes.COMMON_NAMESPACE);
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

	@Override
	public void saveChanges(ConnectionWizardPageModel pageModel) {
		IConnection c = pageModel.getConnection();
		if( c instanceof Connection 
				&& getExtendedProperties() != null ) {
			((Connection)c).setExtendedProperties(getExtendedProperties());
		}
	}
}
